-- MySQL 8 / InnoDB. Inspect indexes first; do not repeat an existing index.
SHOW INDEX FROM cwms.inventory;

-- Execute only after verifying both named indexes are absent.
SET SESSION lock_wait_timeout = 10;
ALTER TABLE cwms.inventory
  ADD INDEX idx_inventory_warehouse_lpn (warehouse_id, lpn),
  ADD INDEX idx_inventory_location_id (location_id),
  ALGORITHM=INPLACE,
  LOCK=NONE;
