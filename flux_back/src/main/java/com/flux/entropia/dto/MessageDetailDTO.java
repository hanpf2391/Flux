package com.flux.entropia.dto;

import java.time.LocalDateTime;

/**
 * DTO for returning detailed message info when a user hovers over a node.
 * (From Task B-2)
 */
public record MessageDetailDTO(
    String content,
    LocalDateTime createdAt
) {
}