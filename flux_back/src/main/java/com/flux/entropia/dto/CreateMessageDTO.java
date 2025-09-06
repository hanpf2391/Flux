package com.flux.entropia.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * DTO for receiving a request to create a new message node.
 * (From Task B-2)
 */
public record CreateMessageDTO(
    // The ID of the message version this edit is based on.
    // Can be null if creating a message in a new, empty cell.
    Long baseVersionId,

    @Size(max = 300, message = "Content must not exceed 300 characters")
    String content,

    @NotNull(message = "Row index cannot be null")
    Integer rowIndex,

    @NotNull(message = "Column index cannot be null")
    Integer colIndex
) {
}