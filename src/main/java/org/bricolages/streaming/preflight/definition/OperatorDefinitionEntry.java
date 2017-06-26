package org.bricolages.streaming.preflight.definition;

import java.io.IOException;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.node.ObjectNode;


import lombok.*;

@AllArgsConstructor
public class OperatorDefinitionEntry {
    @JsonProperty("op")
    @Getter
    private String operatorId;

    @JsonDeserialize(using = ObjectTreeDeserializer.class)
    private Object params;

    public String getParams() {
        ObjectMapper mapper = new ObjectMapper();
        try {
            return mapper.writeValueAsString(params);
        } catch(JsonProcessingException ex)  {
            // this json serialization must be succeed
            // because data is from a valid yaml
            throw new RuntimeException(ex);
        }
    }
}

class ObjectTreeDeserializer extends JsonDeserializer<ObjectNode> {
    public ObjectNode deserialize(JsonParser jp, DeserializationContext ctx) throws IOException {
        return jp.getCodec().readTree(jp);
    }
}
