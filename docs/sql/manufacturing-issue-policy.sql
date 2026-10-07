-- Run once on the correct environment before deploying the updated layout service.
-- Verify SHOW COLUMNS FROM warehouse_configuration first; do not repeat existing columns.
-- No inventory, Pick or work order records are changed by this migration.
SET SESSION lock_wait_timeout = 10;
ALTER TABLE warehouse_configuration
  ADD COLUMN manufacturing_issue_require_source_location BIT(1) NOT NULL DEFAULT b'1',
  ADD COLUMN manufacturing_issue_require_allocated_lpn BIT(1) NOT NULL DEFAULT b'1',
  ALGORITHM=INPLACE, LOCK=NONE;
