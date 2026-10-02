-- Required by the v1.60 entity mapping, even for Item service startup validation.
-- Back up integration_receipt_line_confirmation before running this migration.
-- Preflight: skip this statement if the column already exists.
-- Preserve existing rows; existing values in this added nullable column are NULL.
SET SESSION lock_wait_timeout = 5;
ALTER TABLE integration_receipt_line_confirmation
  ADD COLUMN quickbook_item_listid VARCHAR(255) NULL,
  ALGORITHM=INPLACE, LOCK=NONE;
