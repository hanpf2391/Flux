package com.flux.entropia.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.flux.entropia.dto.CanvasInitialPositionDTO;
import com.flux.entropia.entity.Message;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * Mapper interface for the Message entity.
 * Provides CRUD operations via BaseMapper and custom queries in MessageMapper.xml.
 */
@Mapper
public interface MessageMapper extends BaseMapper<Message> {

    /**
     * Sets the is_latest flag to 0 for all message versions at a given cell coordinate.
     *
     * @param rowIndex The row index of the cell.
     * @param colIndex The column index of the cell.
     */
    void unsetLatestFlagForCell(@Param("rowIndex") int rowIndex, @Param("colIndex") int colIndex);

    /**
     * Selects the single latest version of a message for a given cell coordinate.
     *
     * @param rowIndex The row index of the cell.
     * @param colIndex The column index of the cell.
     * @return The latest Message entity, or null if none exists.
     */
    Message selectLatestForCell(@Param("rowIndex") int rowIndex, @Param("colIndex") int colIndex);

    /**
     * Counts the number of distinct coordinates (cells) that have messages.
     *
     * @return The total count of distinct cells.
     */
    long countDistinctCoordinates();

    /**
     * Counts the number of distinct coordinates within a given grid range.
     *
     * @param startRow The starting row index.
     * @param endRow The ending row index.
     * @param startCol The starting column index.
     * @param endCol The ending column index.
     * @return The count of distinct coordinates in the range.
     */
    long countDistinctCoordinatesInGrid(@Param("startRow") int startRow, @Param("endRow") int endRow, @Param("startCol") int startCol, @Param("endCol") int endCol);

    /**
     * Calculates the optimal hotspot position using grid-based aggregation.
     * Implements the optimized SQL query from the design specification.
     *
     * @param gridSize The size of the grid cells for aggregation.
     * @param timeWindowDays The time window in days to consider for recent activity.
     * @return The calculated hotspot position, or null if no suitable position found.
     */
    CanvasInitialPositionDTO calculateHotspotPosition(@Param("gridSize") int gridSize, @Param("timeWindowDays") int timeWindowDays);
}
