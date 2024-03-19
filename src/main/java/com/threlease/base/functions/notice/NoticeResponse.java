package com.threlease.base.functions.notice;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Builder;
import lombok.Data;

import java.util.Optional;

@Data
@Builder
public class NoticeResponse {
    private String type;
    private Optional<String> message;

    public String toJson() throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        ObjectNode jsonNode = objectMapper.createObjectNode();

        jsonNode.put("NoticeType", type);
        message.ifPresent(s -> jsonNode.put("message", s));

        return objectMapper.writeValueAsString(jsonNode);
    }
}
