package org.bricolages.streaming.stream.processor;
import org.bricolages.streaming.stream.StreamColumn;
import org.bricolages.streaming.filter.*;
import org.bricolages.streaming.exception.*;
import java.time.ZoneOffset;
import java.time.OffsetDateTime;
import java.time.DateTimeException;
import lombok.*;

public class TimestampColumnProcessor extends SingleColumnProcessor {
    static TimestampColumnProcessor build(StreamColumn column, ProcessorContext ctx) {
        try {
            if (column.getSourceOffset() == null) {
                throw new ConfigError("source_offset is null: " + column.getName());
            }
            val src = ZoneOffset.of(column.getSourceOffset());
            val dest = (column.getZoneOffset() != null) ? ZoneOffset.of(column.getZoneOffset()) : null;
            return new TimestampColumnProcessor(column, src, dest);
        }
        catch (DateTimeException ex) {
            throw new ConfigError("bad zone offset: " + column.getName() + ": " + ex.getMessage());
        }
    }

    final ZoneOffset sourceOffset;
    final ZoneOffset zoneOffset;

    public TimestampColumnProcessor(StreamColumn column, ZoneOffset sourceOffset, ZoneOffset zoneOffset) {
        super(column);
        this.sourceOffset = sourceOffset;
        this.zoneOffset = zoneOffset;
    }

    // For tests
    public TimestampColumnProcessor(StreamColumn column, String sourceOffset, String zoneOffset) {
        this(column, ZoneOffset.of(sourceOffset), (zoneOffset == null) ? null : ZoneOffset.of(zoneOffset));
    }

    @Override
    public Object processValue(Object value) throws FilterException {
        if (value == null) return null;
        val tm = getOffsetDateTime(value);
        if (tm == null) return null;
        return Cleanse.formatSqlTimestamp(tm);
    }

    OffsetDateTime getOffsetDateTime(Object value) throws FilterException {
        if (zoneOffset == null) {
            // Use source offset as-is
            return Cleanse.getOffsetDateTime(value, sourceOffset, true);
        }
        else {
            return Cleanse.getLocalOffsetDateTime(value, sourceOffset, zoneOffset);
        }
    }
}