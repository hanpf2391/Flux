create database if not exists  `flux`;
use flux;
-- Drop the table if it exists to ensure a clean slate
DROP TABLE IF EXISTS `messages`;
-- Create the final version of the messages table
CREATE TABLE `messages` (
                            `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
                            `row_index` int NOT NULL COMMENT '行坐标',
                            `col_index` int NOT NULL COMMENT '列坐标',
                            `content` text COLLATE utf8mb4_unicode_ci COMMENT '文本内容, 允许为空',
                            `bg_color` varchar(7) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '背景色 (#RRGGBB), 允许为空',
                            `ip_address` varchar(45) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '最后修改者的IP地址',
                            `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                            `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后更新时间',
                            PRIMARY KEY (`id`),
    --  这是在建表时定义普通索引的正确语法
                            INDEX `idx_coordinates` (`row_index`, `col_index`)
) ENGINE=InnoDB AUTO_INCREMENT=165 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='单元格数据表';