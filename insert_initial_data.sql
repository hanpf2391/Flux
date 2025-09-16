-- 插入欢迎信息和使用信息的SQL语句
-- 执行这些语句来在数据库中添加初始内容

-- 欢迎信息（位置0,0）
INSERT INTO messages (content, bg_color, row_index, col_index, ip_address, created_at) 
VALUES ('欢迎来到Flux画布！🎉', '#87CEEB', 0, 0, '127.0.0.1', NOW());

-- 使用说明（垂直排列在左侧）
INSERT INTO messages (content, bg_color, row_index, col_index, ip_address, created_at) 
VALUES ('🖱️ 拖拽移动画布', NULL, 1, 0, '127.0.0.1', NOW());

INSERT INTO messages (content, bg_color, row_index, col_index, ip_address, created_at) 
VALUES ('🎨 右上角画笔绘制', NULL, 2, 0, '127.0.0.1', NOW());

INSERT INTO messages (content, bg_color, row_index, col_index, ip_address, created_at) 
VALUES ('✏️ 点击单元格编辑', NULL, 3, 0, '127.0.0.1', NOW());

INSERT INTO messages (content, bg_color, row_index, col_index, ip_address, created_at) 
VALUES ('🧹 橡皮擦清除颜色', NULL, 4, 0, '127.0.0.1', NOW());

INSERT INTO messages (content, bg_color, row_index, col_index, ip_address, created_at) 
VALUES ('🏠 Home回到原点', NULL, 5, 0, '127.0.0.1', NOW());

INSERT INTO messages (content, bg_color, row_index, col_index, ip_address, created_at) 
VALUES ('🔍 热力图显示活跃区域', NULL, 6, 0, '127.0.0.1', NOW());

-- 装饰性内容
INSERT INTO messages (content, bg_color, row_index, col_index, ip_address, created_at) 
VALUES ('开始创作吧！', '#FFB6C1', 0, 2, '127.0.0.1', NOW());

INSERT INTO messages (content, bg_color, row_index, col_index, ip_address, created_at) 
VALUES ('✨', '#FFD700', 0, 3, '127.0.0.1', NOW());

INSERT INTO messages (content, bg_color, row_index, col_index, ip_address, created_at) 
VALUES ('🚀', '#98FB98', 0, 4, '127.0.0.1', NOW());

-- 添加一些额外的提示信息
INSERT INTO messages (content, bg_color, row_index, col_index, ip_address, created_at) 
VALUES ('提示：按ESC键退出当前工具', NULL, 8, 0, '127.0.0.1', NOW());

INSERT INTO messages (content, bg_color, row_index, col_index, ip_address, created_at) 
VALUES ('左上角显示在线人数和统计', NULL, 9, 0, '127.0.0.1', NOW());

INSERT INTO messages (content, bg_color, row_index, col_index, ip_address, created_at) 
VALUES ('右下角显示当前坐标', NULL, 10, 0, '127.0.0.1', NOW());