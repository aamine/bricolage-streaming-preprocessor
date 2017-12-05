package org.bricolages.streaming.filter;
import java.time.*;
import lombok.*;

public class UnixTimeOp extends SingleColumnOp {
    static final void register(OpBuilder builder) {
        builder.registerOperator("unixtime", (def) ->
            new UnixTimeOp(def, def.mapParameters(Parameters.class))
        );
    }

    @Getter
    @Setter
    public static class Parameters {
        String zoneOffset;
    }

    final ZoneOffset zoneOffset;

    UnixTimeOp(OperatorDefinition def, Parameters params) {
        this(def, params.zoneOffset);
    }

    UnixTimeOp(OperatorDefinition def, String offset) {
        super(def);
        this.zoneOffset = ZoneOffset.of(offset);
    }

    @Override
    public Object applyValue(Object value, Record record) throws FilterException {
        if (value == null) return null;
        if (isFloat(value)) {
            return formatSqlTimestamp(unixTimeToOffsetDateTime(getFloat(value), zoneOffset));
        }
        else {
            return formatSqlTimestamp(unixTimeToOffsetDateTime(getInteger(value), zoneOffset));
        }
    }
}
