package com.flux.entropia.mapper;

import com.flux.entropia.dto.HeatmapChunkDTO;
import com.flux.entropia.service.impl.HeatmapServiceImpl;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * Mapper interface for heatmap-related database operations.
 * Provides optimized SQL queries for chunk-based heatmap data loading.
 */
@Mapper
public interface HeatmapMapper {

    /**
     * Get heatmap data for specified chunks using optimized SQL query.
     * Implements the "on-demand chunk loading" architecture from the design specification.
     *
     * @param chunkCoordinates List of chunk coordinates to query
     * @return List of heatmap data points with heat values
     */
    List<HeatmapChunkDTO.HeatmapDataDTO> getHeatmapChunks(
            @Param("chunkCoordinates") List<HeatmapServiceImpl.ChunkCoordinate> chunkCoordinates
    );
}