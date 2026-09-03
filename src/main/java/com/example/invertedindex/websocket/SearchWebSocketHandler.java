package com.example.invertedindex.websocket;

import com.example.invertedindex.model.request.SearchRequest;
import com.example.invertedindex.model.response.SearchStreamMessage;
import com.example.invertedindex.service.SearchService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Slf4j
@Component
@RequiredArgsConstructor
public class SearchWebSocketHandler extends TextWebSocketHandler {

    private final ObjectMapper objectMapper;
    private final SearchService searchService;

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        try {
            SearchRequest request = objectMapper.readValue(message.getPayload(), SearchRequest.class);
            searchService.searchStream(request, msg -> {
                try {
                    if (session.isOpen()) {
                        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(msg)));
                    }
                } catch (Exception e) {
                    log.error("Failed to send websocket message", e);
                }
            });
        } catch (Exception e) {
            log.error("WebSocket handle logic error", e);
        }
    }
}
