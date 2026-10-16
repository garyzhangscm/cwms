CREATE TABLE outbound_allocation_task (
 id VARCHAR(36) PRIMARY KEY,
 sequence_id BIGINT NOT NULL AUTO_INCREMENT UNIQUE,
 warehouse_id BIGINT NOT NULL,
 order_id BIGINT NOT NULL,
 request_id VARCHAR(64) NOT NULL,
 active_order_key VARCHAR(80) NULL UNIQUE,
 state VARCHAR(24) NOT NULL,
 original_order_state VARCHAR(24) NOT NULL,
 error_code VARCHAR(64) NULL,
 message VARCHAR(1000) NULL,
 requires_review BOOLEAN NOT NULL DEFAULT FALSE,
 created_at TIMESTAMP(3) NOT NULL,
 started_at TIMESTAMP(3) NULL,
 finished_at TIMESTAMP(3) NULL,
 heartbeat_at TIMESTAMP(3) NOT NULL,
 CONSTRAINT uq_allocation_request UNIQUE (warehouse_id, order_id, request_id)
);
CREATE INDEX ix_allocation_order ON outbound_allocation_task (warehouse_id, order_id, created_at);
CREATE INDEX ix_allocation_heartbeat ON outbound_allocation_task (state, heartbeat_at);
