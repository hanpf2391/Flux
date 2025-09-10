package com.flux.entropia.service.impl;

import com.flux.entropia.dto.HeatmapChunkDTO;
import com.flux.entropia.mapper.HeatmapMapper;
import com.flux.entropia.service.HeatmapService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service implementation for heatmap-related business logic.
 * Implements the "on-demand chunk loading" architecture.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class HeatmapServiceImpl implements HeatmapService {

    private final HeatmapMapper heatmapMapper;
    private static final int CHUNK_SIZE = 9;

    @Override
    public HeatmapChunkDTO getHeatmapChunks(String chunks) {
        try {
            // Parse the chunks parameter
            List<ChunkCoordinate> chunkCoordinates = parseChunksParameter(chunks);
            
            if (chunkCoordinates.isEmpty()) {
                log.warn("No valid chunks provided in request: {}", chunks);
                return new HeatmapChunkDTO(CHUNK_SIZE, List.of());
            }

            log.info("Requesting heatmap data for {} chunks: {}", chunkCoordinates.size(), chunks);
            
            // Get heatmap data from database
            List<HeatmapChunkDTO.HeatmapDataDTO> heatmapData = heatmapMapper.getHeatmapChunks(chunkCoordinates);
            
            log.info("Retrieved heatmap data for {} chunks", heatmapData.size());
            
            return new HeatmapChunkDTO(CHUNK_SIZE, heatmapData);
            
        } catch (Exception e) {
            log.error("Failed to get heatmap chunks for parameter: {}", chunks, e);
            return new HeatmapChunkDTO(CHUNK_SIZE, List.of());
        }
    }

    /**
     * Parse the chunks parameter string into a list of ChunkCoordinate objects.
     * Format: "gridX,gridY;gridX,gridY;..."
     */
    private List<ChunkCoordinate> parseChunksParameter(String chunks) {
        if (chunks == null || chunks.trim().isEmpty()) {
            return List.of();
        }

        try {
            return Arrays.stream(chunks.split(";"))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .map(this::parseChunkCoordinate)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Failed to parse chunks parameter: {}", chunks, e);
            return List.of();
        }
    }

    /**
     * Parse a single chunk coordinate string "gridX,gridY" into ChunkCoordinate.
     */
    private ChunkCoordinate parseChunkCoordinate(String coordinateStr) {
        String[] parts = coordinateStr.split(",");
        if (parts.length != 2) {
            throw new IllegalArgumentException("Invalid chunk coordinate format: " + coordinateStr);
        }

        try {
            int gridX = Integer.parseInt(parts[0].trim());
            int gridY = Integer.parseInt(parts[1].trim());
            return new ChunkCoordinate(gridX, gridY);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid numbers in chunk coordinate: " + coordinateStr, e);
        }
    }

    /**
     * Inner class to represent chunk coordinates for SQL queries.
     */
    public record ChunkCoordinate(int gridX, int gridY) {
    }
}