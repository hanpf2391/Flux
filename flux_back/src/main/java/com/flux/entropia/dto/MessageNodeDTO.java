package com.flux.entropia.dto;

/**
 * DTO for returning the data of a message cell.
 * Used for displaying cells in the grid.
 * (From Task B-2, adapted for grid system)
 */
public record MessageNodeDTO(
    Long id,
    Integer rowIndex,
    Integer colIndex,
    String content,
    String bgColor
) {
}
