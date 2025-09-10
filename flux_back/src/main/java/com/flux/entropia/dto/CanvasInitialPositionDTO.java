package com.flux.entropia.dto;

/**
 * Data Transfer Object for canvas initial position response.
 * Contains the optimal coordinates for new user positioning.
 */
public record CanvasInitialPositionDTO(
    int rowIndex,
    int colIndex,
    boolean isDefault,
    String message
) {
    /**
     * Constructor for default position
     */
    public static CanvasInitialPositionDTO defaultPosition() {
        return new CanvasInitialPositionDTO(0, 0, true, "Default position - no active areas found");
    }

    /**
     * Constructor for calculated hotspot position
     */
    public static CanvasInitialPositionDTO hotspotPosition(int rowIndex, int colIndex) {
        return new CanvasInitialPositionDTO(rowIndex, colIndex, false, "Hotspot position based on recent activity");
    }
}