CREATE TABLE IF NOT EXISTS cwms.load_order_plan (
  warehouse_id BIGINT NOT NULL,
  order_id BIGINT NOT NULL,
  load_id BIGINT NOT NULL,
  shipment_id BIGINT NULL,
  next_check_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  created_by VARCHAR(50) NOT NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (warehouse_id, order_id),
  KEY idx_load_order_plan_load (warehouse_id, load_id),
  KEY idx_load_order_plan_pending (shipment_id, next_check_at),
  CONSTRAINT fk_load_order_plan_order FOREIGN KEY (order_id) REFERENCES outbound_order(outbound_order_id) ON DELETE CASCADE,
  CONSTRAINT fk_load_order_plan_load FOREIGN KEY (load_id) REFERENCES trailer_appointment(trailer_appointment_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
