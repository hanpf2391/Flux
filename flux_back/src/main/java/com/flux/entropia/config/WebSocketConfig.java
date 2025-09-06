package com.flux.entropia.config;

import com.flux.entropia.websocket.FluxWebSocketHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * Configures the WebSocket handler and endpoint.
 */
@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {

    private final FluxWebSocketHandler fluxWebSocketHandler;

    /**
     * Registers the WebSocket handler to a specific path.
     * @param registry The WebSocket handler registry.
     */
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(fluxWebSocketHandler, "/ws/flux")
                .setAllowedOrigins("*"); // Allow all origins for simplicity
    }
}
