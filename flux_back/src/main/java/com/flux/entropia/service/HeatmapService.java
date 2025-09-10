package com.flux.entropia.service;

import com.flux.entropia.dto.HeatmapChunkDTO;

/**
 * Service interface for heatmap-related business logic.
 * Provides dynamic chunk-based heatmap functionality.
 */
public interface HeatmapService {

    /**
     * Get heatmap data for specified chunks.
     * Implements the "on-demand chunk loading" architecture.
     *
     * @param chunks A semicolon-separated string of chunk IDs (format: "gridX,gridY")
     * @return Heatmap data for the requested chunks
     */
    HeatmapChunkDTO getHeatmapChunks(String chunks);
}