package org.bricolages.streaming.preflight;
import org.bricolages.streaming.preflight.definition.*;
import org.bricolages.streaming.stream.PacketFilterFactory;
import org.bricolages.streaming.stream.PacketFilter;
import org.bricolages.streaming.stream.PacketFilterLog;
import org.bricolages.streaming.object.S3ObjectLocator;
import org.bricolages.streaming.object.LocatorIOException;
import org.bricolages.streaming.exception.*;
import java.io.FileReader;
import java.io.IOException;
import java.io.FileNotFoundException;
import lombok.*;

@RequiredArgsConstructor
public class Runner {
    final PacketFilterFactory factory;

    public PacketFilter loadFilter(String streamDefPath) throws IOException, LocatorIOException {
        val streamDef = loadStreamDef(streamDefPath);
        val generator = new PacketFilterGenerator(factory, streamDef);
        return generator.generate();
    }

    StreamDefinitionEntry loadStreamDef(String path) throws IOException {
        val domainCollection = loadDomainCollection("config/domains.yml");
        val columnCollection = loadWellknownCollumnCollection("config/wellknown_columns.yml", domainCollection);
        val fileReader = new FileReader(path);
        return StreamDefinitionEntry.load(fileReader, domainCollection, columnCollection);
    }

    DomainCollection loadDomainCollection(String domainCollectionFilePath) throws IOException {
        try {
            val fileReader = new FileReader(domainCollectionFilePath);
            return DomainCollection.load(fileReader);
        } catch(FileNotFoundException ex) {
            return DomainCollection.empty();
        }
    }

    WellknownColumnCollection loadWellknownCollumnCollection(String wellknownColumnCollectionFilePath, DomainCollection domainCollection) throws IOException {
        try {
            val fileReader = new FileReader(wellknownColumnCollectionFilePath);
            return WellknownColumnCollection.load(fileReader, domainCollection);
        } catch(FileNotFoundException ex) {
            return WellknownColumnCollection.empty();
        }
    }

    public void preprocess(PacketFilter filter, S3ObjectLocator src, S3ObjectLocator dest) throws IOException, LocatorIOException {
        System.err.printf("*** preproc start\n");
        System.err.printf("preproc source     : %s\n", src.toString());
        System.err.printf("preproc destination: %s\n", dest.toString());
        val filterLog = new PacketFilterLog(src.toString(), dest.toString());
        filter.processLocator(src, dest, filterLog);
        System.err.printf("*** preproc succeeded: in=%d, out=%d, error=%d\n", filterLog.inputRows, filterLog.outputRows, filterLog.errorRows);
    }
}
