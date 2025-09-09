package com.flux.entropia.websocket;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flux.entropia.service.MessageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArraySet;

@Component
@Slf4j
public class FluxWebSocketHandler extends TextWebSocketHandler {

    private final ObjectMapper objectMapper;
    private static final CopyOnWriteArraySet<WebSocketSession> sessions = new CopyOnWriteArraySet<>();
    
    @Autowired
    private MessageService messageService;

    public FluxWebSocketHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        log.warn("FluxWebSocketHandler INSTANCE CREATED. HashCode: {}", this.hashCode());
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        sessions.add(session);
        log.info("New WebSocket connection from: {}, Session ID: {}. Total sessions: {}", session.getRemoteAddress(), session.getId(), sessions.size());
        broadcastOnlineCount();
        broadcastSystemStats();
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        try {
            Map<String, Object> messageMap = objectMapper.readValue(message.getPayload(), new TypeReference<>() {});
            String type = (String) messageMap.get("type");

            if ("USER_IS_EDITING".equals(type) || "USER_STOPPED_EDITING".equals(type)) {
                // For editing status, broadcast to OTHERS, excluding the sender.
                broadcastToOthers(session, message);
            } else {
                log.warn("Received unhandled message type: {}", type);
            }
        } catch (IOException e) {
            log.error("Error processing incoming WebSocket message: {}", e.getMessage());
        }
    }

    /**
     * Broadcasts a message to all connected WebSocket clients.
     * @param message The message object to broadcast.
     */
    public void broadcast(WebSocketMessage<?> message) {
        try {
            String messageJson = objectMapper.writeValueAsString(message);
            TextMessage textMessage = new TextMessage(messageJson);
            int recipientCount = 0;
            for (WebSocketSession session : sessions) {
                if (session.isOpen()) {
                    try {
                        session.sendMessage(textMessage);
                        recipientCount++;
                    } catch (IOException e) {
                        log.error("Error broadcasting to session {}: {}", session.getId(), e.getMessage());
                    }
                }
            }
            log.info("Broadcasted '{}' message to {} sessions", message.getType(), recipientCount);
        } catch (IOException e) {
            log.error("Error serializing WebSocket message: {}", e.getMessage(), e);
        }
    }

    /**
     * Broadcasts a message to all clients EXCEPT the sender.
     * @param senderSession The session of the client who sent the original message.
     * @param message The message to broadcast.
     */
    private void broadcastToOthers(WebSocketSession senderSession, TextMessage message) {
        int recipientCount = 0;
        for (WebSocketSession session : sessions) {
            if (session.isOpen() && !session.getId().equals(senderSession.getId())) {
                try {
                    session.sendMessage(message);
                    recipientCount++;
                } catch (IOException e) {
                    log.error("Error broadcasting to other session {}: {}", session.getId(), e.getMessage());
                }
            }
        }
        if (recipientCount > 0) {
            log.info("Relayed message to {} other sessions", recipientCount);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessions.remove(session);
        log.info("WebSocket connection closed: {} with status: {}. Total sessions: {}", session.getId(), status, sessions.size());
        broadcastOnlineCount();
        broadcastSystemStats();
    }

    private void broadcastOnlineCount() {
        broadcast(new WebSocketMessage<>("ONLINE_COUNT_UPDATED", getOnlineUserCount()));
    }

    /**
     * Broadcasts system statistics to all connected clients.
     * This includes online user count and total message count.
     */
    public void broadcastSystemStats() {
        int onlineCount = getOnlineUserCount();
        long totalMessages = messageService.getTotalMessageCount();
        
        // Create a map with statistics data
        Map<String, Object> statsData = Map.of(
            "onlineCount", onlineCount,
            "totalMessages", totalMessages
        );
        
        broadcast(new WebSocketMessage<>("SYSTEM_STATS_UPDATED", statsData));
    }

    public int getOnlineUserCount() {
        return sessions.size();
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.error("WebSocket transport error for session {}: {}", session.getId(), exception.getMessage());
        if (session.isOpen()) {
            try {
                session.close(CloseStatus.SERVER_ERROR);
            } catch (IOException e) {
                log.error("Error closing session with transport error: {}", e.getMessage());
            }
        }
    }
}