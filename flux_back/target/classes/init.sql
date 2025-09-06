-- Drop the table if it exists to ensure a clean slate for development
CREATE DATABASE IF NOT EXISTS `flux`;
USE flux;

-- Create the messages table with support for version history
CREATE TABLE IF NOT EXISTS `messages` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Primary Key, unique identifier for each message version',
  `content` TEXT COMMENT 'The text content of the message cell',
  `is_latest` TINYINT(1) NOT NULL DEFAULT 0 COMMENT 'Flag to indicate if this is the latest version for the cell. 1=true, 0=false',
  `row_index` INT NOT NULL COMMENT 'The row index of the cell in the grid',
  `col_index` INT NOT NULL COMMENT 'The column index of the cell in the grid',
  `ip_address` VARCHAR(45) NOT NULL COMMENT 'The IP address of the message creator',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'The timestamp when this version was created',
  PRIMARY KEY (`id`),
  -- The unique key on (row_index, col_index) is removed to allow for history.
  -- We now use a dedicated index to quickly find the latest version of a cell.
  KEY `idx_latest_coords` (`is_latest`, `row_index`, `col_index`),
  KEY `idx_coords` (`row_index`, `col_index`) COMMENT 'Index to quickly find all versions of a cell'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Stores the message cells and their history for the Flux grid';

-- The old sample data logic is removed as it conflicts with the new history model.
-- You can manually insert data if needed for testing.
