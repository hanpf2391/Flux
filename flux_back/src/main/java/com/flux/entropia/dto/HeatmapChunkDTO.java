package com.flux.entropia.dto;

import java.util.List;

/**
 * Data Transfer Object for heatmap chunk data response.
 */
public record HeatmapChunkDTO(
    int chunkSize,
    List<HeatmapDataDTO> data
) {
    /**
     * Data Transfer Object for individual heatmap data points.
     */
    public record HeatmapDataDTO(
        int gridY,
        int gridX,
        int heatValue
    ) {
    }
}