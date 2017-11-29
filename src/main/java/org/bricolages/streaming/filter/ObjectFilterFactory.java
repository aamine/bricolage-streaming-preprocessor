package org.bricolages.streaming.filter;
import org.bricolages.streaming.stream.DataStreamRepository;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import lombok.*;

@Slf4j
public class ObjectFilterFactory {
    @Autowired
    DataStreamRepository repos;

    @Autowired
    OpBuilder builder;

    public ObjectFilter load(String streamName) {
        List<OperatorDefinition> defs = repos.findStream(streamName).getOperatorDefinitions();
        return compose(defs);
    }

    public ObjectFilter compose(List<OperatorDefinition> defs) {
        List<Op> ops = defs.stream().map((def) -> {
            Op op = builder.build(def);
            log.debug("operator stacked: {}", op);
            return op;
        }).collect(Collectors.toList());
        return new ObjectFilter(ops);
    }
}
