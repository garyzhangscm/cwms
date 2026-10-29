CREATE TABLE IF NOT EXISTS cwms.shipment_inventory_batch (
  warehouse_id BIGINT NOT NULL,
  shipment_id BIGINT NOT NULL,
  destination_id BIGINT NOT NULL,
  request_hash CHAR(64) NOT NULL,
  snapshot_json LONGTEXT NOT NULL,
  completed_at DATETIME(3) NULL,
  PRIMARY KEY (warehouse_id, shipment_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
