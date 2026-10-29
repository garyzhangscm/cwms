-- Additive schema only. Does not alter orders, picks, inventory or existing allocations.
CREATE TABLE IF NOT EXISTS quantity_allocation_mutex (
  warehouse_id BIGINT NOT NULL PRIMARY KEY
) ENGINE=InnoDB;
CREATE TABLE IF NOT EXISTS quantity_allocation_order (
  order_id BIGINT NOT NULL PRIMARY KEY,
  warehouse_id BIGINT NOT NULL,
  created_by VARCHAR(128) NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_qa_order_warehouse (warehouse_id, order_id)
) ENGINE=InnoDB;
CREATE TABLE IF NOT EXISTS quantity_allocation_line (
  order_line_id BIGINT NOT NULL PRIMARY KEY,
  order_id BIGINT NOT NULL,
  warehouse_id BIGINT NOT NULL,
  item_id BIGINT NOT NULL,
  inventory_status_id BIGINT NOT NULL,
  client_id BIGINT NOT NULL DEFAULT 0,
  demand BIGINT NOT NULL,
  held BIGINT NOT NULL DEFAULT 0,
  committed BIGINT NOT NULL DEFAULT 0,
  revision BIGINT NOT NULL DEFAULT 0,
  INDEX idx_qa_pool (warehouse_id, item_id, inventory_status_id, client_id),
  INDEX idx_qa_line_order (order_id),
  CONSTRAINT chk_qa_balance CHECK (demand >= 0 AND held >= 0 AND committed >= 0 AND held <= demand AND committed <= demand-held)
) ENGINE=InnoDB;
CREATE TABLE IF NOT EXISTS quantity_allocation_request (
  request_id CHAR(36) NOT NULL PRIMARY KEY,
  order_id BIGINT NOT NULL,
  warehouse_id BIGINT NOT NULL,
  fingerprint CHAR(64) NOT NULL,
  result_json MEDIUMTEXT NOT NULL,
  created_by VARCHAR(128) NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_qa_request_order (order_id, created_at)
) ENGINE=InnoDB;
-- One shared database mutex per warehouse serializes soft-to-hard conversions with classic allocation.
-- New warehouse rows are also lazily initialized with INSERT IGNORE by the service.
INSERT IGNORE INTO quantity_allocation_mutex (warehouse_id) SELECT warehouse_id FROM warehouse;
