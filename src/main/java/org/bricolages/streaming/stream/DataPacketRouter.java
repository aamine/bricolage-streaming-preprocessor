package org.bricolages.streaming.stream;
import org.bricolages.streaming.locator.*;
import org.bricolages.streaming.exception.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.regex.PatternSyntaxException;
import lombok.*;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Slf4j
public class DataPacketRouter {
    @NoArgsConstructor
    public static final class Entry {
        @Setter public String srcUrlPattern;
        @Setter public String streamName;
        @Setter public String streamPrefix;
        @Setter public String destBucket;
        @Setter public String destPrefix;
        @Setter public String objectPrefix;
        @Setter public String objectName;

        public Entry(String srcUrlPattern, String streamName, String streamPrefix, String destBucket, String destPrefix, String objectPrefix, String objectName) {
            this.srcUrlPattern = srcUrlPattern;
            this.streamName = streamName;
            this.streamPrefix = streamPrefix;
            this.destBucket = destBucket;
            this.destPrefix = destPrefix;
            this.objectPrefix = objectPrefix;
            this.objectName = objectName;
        }

        Pattern pat = null;

        Pattern sourcePattern() {
            if (pat != null) return pat;
            pat = Pattern.compile("^" + srcUrlPattern + "$");
            return pat;
        }
    }

    final List<Entry> entries;

    void check() throws ConfigError {
        for (Entry ent : entries) {
            try {
                ent.sourcePattern();
            }
            catch (PatternSyntaxException ex) {
                throw new ConfigError("source pattern syntax error: " + ent.srcUrlPattern);
            }
        }
    }

    @RequiredArgsConstructor
    public static class Result {
        @Getter final DataStream stream;
        @Getter final StreamBundle bundle;
        @Getter final String objectPrefix;
        @Getter final String objectName;

        public S3ObjectLocator getDestLocator() {
            if (stream == null) return null;
            if (bundle == null) return null;
            return new S3ObjectLocator(bundle.getDestBucket(), Paths.get(bundle.getDestPrefix(), objectPrefix, objectName).toString());
        }

        public String getStreamName() {
            if (stream == null) return null;
            return stream.getStreamName();
        }
    }

    public Result route(S3ObjectLocator src) throws ConfigError {
        Result r1 = routeBySavedRoutes(src);
        if (r1 != null) return r1;
        Result r2 = routeByPatterns(src);
        if (r2 != null) {
            return r2;
        }
        else {
            logUnknownS3Object(src.toString());
            return null;
        }
    }

    @Autowired
    DataStreamRepository streamRepos;

    @Autowired
    StreamBundleRepository streamBundleRepos;

    /**
     * Expects URL like: s3://src-bucket/prefix1/prefix2/prefix3/.../YYYY/MM/DD/objectName.gz
     * streamPrefix: "prefix1/prefix2/prefix3/..."
     * objectPrefix: "YYYY/MM/DD"
     * objectName: "objectName.gz"
     */
    Result routeBySavedRoutes(S3ObjectLocator src) {
        val components = src.key().split("/");
        if (components.length < 5) {
            logUnknownS3Object(src.toString());
            return null;
        }
        String[] prefixComponents = Arrays.copyOfRange(components, 0, components.length - 4);
        val prefix = String.join("/", prefixComponents);
        String[] objPrefixComponents = Arrays.copyOfRange(components, components.length - 4, components.length - 1);
        val objPrefix = String.join("/", objPrefixComponents);
        val objName = components[components.length - 1];
        log.debug("parsed url: prefix={}, objPrefix={}, objName={}", prefix, objPrefix, objName);

        val bundle = streamBundleRepos.findStreamBundle(src.bucket(), prefix);
        if (bundle == null) return null;
        val stream = bundle.getStream();
        if (stream == null) throw new ApplicationError("FATAL: could not get stream for stream_bundle: stream_bundle_id=" + bundle.getId());

        return new Result(stream, bundle, objPrefix, objName);
    }

    Result routeByPatterns(S3ObjectLocator src) throws ConfigError {
        val components = matchRoutes(src);
        if (components == null) return null;
        val stream = findOrCreateStream(components.streamName);
        if (stream.doesDefer()) {
            // Processing is temporary disabled; process objects later
            return null;
        }
        val bundle = findOrCreateStreamBundle(stream, components);
        return new Result(stream, bundle, components.objectPrefix, components.objectName);
    }

    // For preflight
    public Result routeWithoutDB(S3ObjectLocator src) throws ConfigError {
        val components = matchRoutes(src);
        if (components == null) return null;
        val stream = new DataStream(components.streamName);
        val bundle = new StreamBundle(stream, components.srcBucket, components.srcPrefix, components.destBucket, components.destPrefix);
        return new Result(stream, bundle, components.objectPrefix, components.objectName);
    }

    RouteComponents matchRoutes(S3ObjectLocator src) throws ConfigError {
        for (Entry ent : entries) {
            Matcher m = ent.sourcePattern().matcher(src.toString());
            if (m.matches()) {
                return new RouteComponents(
                    safeSubst(ent.streamName, m),
                    src.bucket(),
                    safeSubst(ent.streamPrefix, m),
                    ent.destBucket,
                    safeSubst(ent.destPrefix, m),
                    safeSubst(ent.objectPrefix, m),
                    safeSubst(ent.objectName, m)
                );
            }
        }
        return null;
    }

    String safeSubst(String template, Matcher m) throws ConfigError {
        try {
            return m.replaceFirst(template);
        }
        catch (IndexOutOfBoundsException ex) {
            throw new ConfigError("bad replacement: " + template);
        }
    }

    @RequiredArgsConstructor
    static final class RouteComponents {
        final String streamName;
        final String srcBucket;
        final String srcPrefix;
        final String destBucket;
        final String destPrefix;
        final String objectPrefix;
        final String objectName;
    }

    DataStream findOrCreateStream(String streamName) {
        DataStream stream = streamRepos.findStream(streamName);
        if (stream == null) {
            try {
                // create new stream with disabled (to avoid to produce non preprocessed output)
                stream = new DataStream(streamName);
                streamRepos.save(stream);
                logNewStream(stream.getId(), streamName);
            }
            catch (DataIntegrityViolationException ex) {
                stream = streamRepos.findStream(streamName);
            }
            log.info("new data packet for unconfigured stream: stream_id={}, stream_name={}", stream.getId(), streamName);
        }
        return stream;
    }

    StreamBundle findOrCreateStreamBundle(DataStream stream, RouteComponents components) {
        val srcBucket = components.srcBucket;
        val srcPrefix = components.srcPrefix;
        val destBucket = components.destBucket;
        val destPrefix = components.destPrefix;

        StreamBundle bundle = streamBundleRepos.findStreamBundle(stream, srcBucket, srcPrefix);
        if (bundle == null) {
            try {
                bundle = new StreamBundle(stream, srcBucket, srcPrefix, destBucket, destPrefix);
                streamBundleRepos.save(bundle);
                logNewStreamBundle(stream.getId(), srcPrefix);
            }
            catch (DataIntegrityViolationException ex) {
                bundle = streamBundleRepos.findStreamBundle(stream, srcBucket, srcPrefix);
            }
        }
        if (! Objects.equals(bundle.getDestBucket(), destBucket)) {
            throw new ApplicationError("FATAL: assertion failed: dest_bucket is different: bundle=" + bundle.getDestBucket() + ", incoming=" + destBucket);
        }
        if (! Objects.equals(bundle.getDestPrefix(), destPrefix)) {
            throw new ApplicationError("FATAL: assertion failed: dest_prefix is different: bundle=" + bundle.getDestPrefix() + ", incoming=" + destPrefix);
        }
        return bundle;
    }

    public void logNewStream(long streamId, String streamName) {
        log.warn("new stream: stream_id={}, stream_name={}", streamId, streamName);
    }

    public void logNewStreamBundle(long streamId, String streamPrefix) {
        log.warn("new stream bundle: stream_id={}, stream_prefix={}", streamId, streamPrefix);
    }

    public void logUnknownS3Object(String url) {
        log.warn("unknown S3 object URL: {}", url);
    }
}