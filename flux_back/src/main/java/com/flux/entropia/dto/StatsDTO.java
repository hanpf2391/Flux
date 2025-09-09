package com.flux.entropia.dto;

public record StatsDTO(
    long totalMessages,
    int onlineUsers,
    long visibleMessages
) {
}
