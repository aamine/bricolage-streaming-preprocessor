package org.bricolages.streaming;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import lombok.*;
import lombok.extern.java.Log;
import lombok.extern.slf4j.Slf4j;

@AllArgsConstructor
@Slf4j
public class ObjectMapper {
    final List<Entry> entries;

    void check() {
        for (Entry ent : entries) {
            ent.sourcePattern();
        }
    }

    public S3ObjectLocation map(S3ObjectLocation src) throws ConfigError {
        for (Entry ent : entries) {
            Matcher m = ent.sourcePattern().matcher(src.urlString());
            if (m.matches()) {
                try {
                    return S3ObjectLocation.forUrl(m.replaceFirst(ent.dest));
                }
                catch (S3UrlParseException ex) {
                    throw new ConfigError(ex);
                }
            }
        }
        // FIXME: error??
        log.error("unknown S3 object URL: {}", src);
        return null;
    }

    @NoArgsConstructor
    public static final class Entry {
        @Getter @Setter String src;
        @Getter @Setter String dest;

        Entry(String src, String dest) {
            this.src = src;
            this.dest = dest;
        }

        Pattern pat = null;

        Pattern sourcePattern() {
            if (pat != null) return pat;
            pat = Pattern.compile("^" + src + "$");
            return pat;
        }
    }
}
