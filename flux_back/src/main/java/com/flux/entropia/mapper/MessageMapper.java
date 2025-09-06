package com.flux.entropia.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
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
}
