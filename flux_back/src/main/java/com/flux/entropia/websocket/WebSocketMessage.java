package com.flux.entropia.websocket;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * A generic wrapper for all messages sent over WebSocket.
 * This provides a standard structure for the frontend to parse.
 * @param <T> The type of the payload.
 */
@Data
@AllArgsConstructor
public class WebSocketMessage<T> {
    /**
     * The type of the message, e.g., "NODE_CREATED" or "HOTSPOT_UPDATED".
     */
    private String type;

    /**
     * The actual data payload.
     */
    private T payload;
}
