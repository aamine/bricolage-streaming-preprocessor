package org.bricolages.streaming.filter;
import org.bricolages.streaming.object.Record;
import lombok.*;

public class DeleteOp extends SingleColumnOp {
    static final void register(OpBuilder builder) {
        builder.registerOperator("delete", (def) ->
            new DeleteOp(def)
        );
    }

    DeleteOp(OperatorDefinition def) {
        super(def);
    }

    @Override
    public Object applyValue(Object value, Record record) throws FilterException {
        return null;
    }
}
