-- Run once in Colton cwms before enabling manual.pick.queue-enabled.
-- Additive only: no inventory or reservation quantities are changed.
CREATE TABLE IF NOT EXISTS cwms.manual_pick_queue (
  request_id VARCHAR(36) NOT NULL PRIMARY KEY,
  warehouse_id BIGINT NOT NULL,
  order_id BIGINT NOT NULL,
  username VARCHAR(255) NOT NULL,
  rf_code VARCHAR(100) NOT NULL,
  company_id VARCHAR(32) NOT NULL,
  state VARCHAR(20) NOT NULL,
  error_message VARCHAR(1000) NULL,
  created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  INDEX idx_manual_queue_order (warehouse_id, order_id, state, created_at),
  INDEX idx_manual_queue_worker (state, created_at)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS cwms.quantity_allocation_legacy_order (
  order_id BIGINT NOT NULL PRIMARY KEY,
  warehouse_id BIGINT NOT NULL,
  recorded_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

-- Freeze the legacy exception once; order_id=0 is a migration marker.
-- Only existing Picks / short allocations qualify, not an empty shipment.
START TRANSACTION;
INSERT IGNORE INTO cwms.quantity_allocation_legacy_order (order_id, warehouse_id)
SELECT DISTINCT ol.outbound_order_id, sl.warehouse_id
FROM cwms.shipment_line sl
JOIN cwms.outbound_order_line ol ON ol.outbound_order_line_id=sl.outbound_order_line_id
WHERE NOT EXISTS (SELECT 1 FROM cwms.quantity_allocation_legacy_order marker WHERE marker.order_id=0)
AND NOT EXISTS (SELECT 1 FROM cwms.quantity_allocation_order q WHERE q.order_id=ol.outbound_order_id)
AND (EXISTS (SELECT 1 FROM cwms.pick p WHERE p.shipment_line_id=sl.shipment_line_id)
  OR EXISTS (SELECT 1 FROM cwms.short_allocation sa WHERE sa.shipment_line_id=sl.shipment_line_id));
INSERT IGNORE INTO cwms.quantity_allocation_legacy_order (order_id,warehouse_id) VALUES (0,0);
COMMIT;
