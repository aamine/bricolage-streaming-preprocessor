package org.bricolages.streaming.filter;

import lombok.*;
import java.util.regex.Pattern;

public class AggregateOp extends Op {
    static final void register(OpBuilder builder) {
        builder.registerOperator("aggregate", (def) ->
            new AggregateOp(def, def.mapParameters(Parameters.class))
        );
    }

    @Getter
    @Setter
    public static class Parameters {
        String targetColumns;
        String aggregatedColumn;
        boolean keepTargetColumns = false;
    }

    final Pattern targetColumns;
    final String aggregatedColumn;
    final boolean keepTargetColumns;

    AggregateOp(OperatorDefinition def, Parameters params) {
        this(def, params.targetColumns, params.aggregatedColumn, params.keepTargetColumns);
    }

    AggregateOp(OperatorDefinition def, String targetColumns, String aggregatedColumn, boolean keepTargetColumns) {
        super(def);
        this.targetColumns = Pattern.compile(targetColumns);
        this.aggregatedColumn = aggregatedColumn;
        this.keepTargetColumns = keepTargetColumns;
    }

    @Override
    public Record apply(Record record) {
        val buf = new Record();
        val it = record.entries();
        while (it.hasNext()) {
            val ent = it.next();
            val m = targetColumns.matcher(ent.getKey());
            if (m.find()) {
                if (ent.getValue() != null) {
                    buf.put(m.replaceFirst(""), ent.getValue());
                }
                if (!keepTargetColumns) {
                    it.remove();
                }
            }
        }
        record.put(aggregatedColumn, buf.getObject());
        return record;
    }
}
