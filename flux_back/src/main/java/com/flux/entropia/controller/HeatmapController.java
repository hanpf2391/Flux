package com.flux.entropia.controller;

import com.flux.entropia.common.ApiResponse;
import com.flux.entropia.dto.HeatmapChunkDTO;
import com.flux.entropia.service.HeatmapService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for handling heatmap-related API requests.
 * Provides dynamic chunk-based heatmap functionality.
 */
@RestController
@RequestMapping("/api/heatmap")
@RequiredArgsConstructor
@Slf4j
public class HeatmapController {

    private final HeatmapService heatmapService;

    /**
     * GET /api/heatmap/chunks : Get heatmap data for specified chunks.
     * Returns heat values for chunks containing recent message activity.
     *
     * @param chunks A semicolon-separated string of chunk IDs (format: "gridX,gridY")
     * @return Heatmap data for the requested chunks
     */
    @GetMapping("/chunks")
    public ResponseEntity<ApiResponse<HeatmapChunkDTO>> getHeatmapChunks(
            @RequestParam String chunks) {
        try {
            log.info("Received heatmap chunks request: {}", chunks);
            HeatmapChunkDTO result = heatmapService.getHeatmapChunks(chunks);
            return ResponseEntity.ok(ApiResponse.success(result));
        } catch (Exception e) {
            log.error("Failed to get heatmap chunks", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to load heatmap data"));
        }
    }
}