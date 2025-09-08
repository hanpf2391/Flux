package com.flux.entropia.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Represents the `messages` table in the database, mapping to a message cell in the grid.
 */
@Data
@TableName("messages")
public class Message {

    /**
     * Primary Key, auto-incremented.
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * The text content of the message cell.
     */
    private String content;

    /**
     * The background color of the cell.
     */
    @TableField("bg_color")
    private String bgColor;

    /**
     * The row index of the cell in the grid.
     */
    @TableField("row_index")
    private Integer rowIndex;

    /**
     * The column index of the cell in the grid.
     */
    @TableField("col_index")
    private Integer colIndex;

    /**
     * The IP address of the message creator, used for rate limiting.
     */
    @TableField("ip_address")
    private String ipAddress;

    /**
     * The timestamp when the message was created.
     */
    @TableField(value = "created_at", insertStrategy = com.baomidou.mybatisplus.annotation.FieldStrategy.NEVER)
    private LocalDateTime createdAt;
}
