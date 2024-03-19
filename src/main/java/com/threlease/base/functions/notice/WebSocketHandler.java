package com.threlease.base.functions.notice;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.threlease.base.functions.notice.enums.NoticeType;
import com.threlease.base.utils.EnumStringComparison;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class WebSocketHandler extends TextWebSocketHandler {
    private static final ConcurrentHashMap<String, WebSocketSession> client = new ConcurrentHashMap<String, WebSocketSession>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        client.put(session.getId(), session);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        client.forEach((key, value) -> {
            try {
                value.sendMessage(message);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        client.remove(session.getId());
    }

    public void send(NoticeResponse response) {
        boolean type_check = EnumStringComparison.compareEnumString(response.getType(), NoticeType.class);
        if (type_check) {
            client.forEach((key, value) -> {
                try {
                    value.sendMessage(new TextMessage(response.toJson()));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        }
    }
}
