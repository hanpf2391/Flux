create database if not exists  `flux`;
use flux;
-- Drop the table if it exists to ensure a clean slate
DROP TABLE IF EXISTS `messages`;
-- Create the final version of the messages table
CREATE TABLE `messages` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `row_index` INT NOT NULL COMMENT '行坐标',
  `col_index` INT NOT NULL COMMENT '列坐标',
  `content` TEXT NULL DEFAULT NULL COMMENT '文本内容, 允许为空',
  `bg_color` VARCHAR(7) NULL DEFAULT NULL COMMENT '背景色 (#RRGGBB), 允许为空',
  `ip_address` VARCHAR(45) NOT NULL COMMENT '最后修改者的IP地址',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后更新时间',
  PRIMARY KEY (`id`),
  -- The most critical constraint for data integrity
  UNIQUE KEY `uk_coordinates` (`row_index`, `col_index`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='单元格数据表';