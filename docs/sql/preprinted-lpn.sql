-- Apply to the Resource service database before enabling ordinary label printing.
CREATE TABLE IF NOT EXISTS preprinted_lpn (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 warehouse_id BIGINT NOT NULL,
 lpn VARCHAR(12) NOT NULL,
 print_batch VARCHAR(80) NOT NULL, reserved_request_id VARCHAR(36),
 UNIQUE KEY uq_preprinted_lpn (warehouse_id,lpn),
 KEY ix_preprinted_lpn_batch (warehouse_id,print_batch)
) ENGINE=InnoDB;

-- Apply this table to the Outbound service database.
CREATE TABLE IF NOT EXISTS manual_pick_operation (
 request_id VARCHAR(36) PRIMARY KEY, warehouse_id BIGINT NOT NULL, order_id BIGINT NOT NULL,
 source_inventory_id BIGINT NOT NULL, source_lpn VARCHAR(12) NOT NULL, new_lpn VARCHAR(12),
 keep_original BIT NOT NULL, quantity BIGINT NOT NULL, source_quantity BIGINT NOT NULL,
 destination_id BIGINT NOT NULL, pick_id BIGINT, completed BIT NOT NULL, cancelled BIT NOT NULL DEFAULT 0,
 UNIQUE KEY uq_manual_pick_operation_pick (pick_id), KEY ix_manual_pick_order (warehouse_id,order_id)
) ENGINE=InnoDB;
-- Apply this table to the Inventory service database.
CREATE TABLE IF NOT EXISTS manual_pick_inventory_operation (
 request_id VARCHAR(36) PRIMARY KEY, warehouse_id BIGINT NOT NULL, fingerprint VARCHAR(64) NOT NULL,
 new_lpn VARCHAR(12), picked_inventory_id BIGINT NOT NULL, picked_lpn VARCHAR(12) NOT NULL, quantity BIGINT NOT NULL,
 UNIQUE KEY uq_manual_pick_new_lpn (warehouse_id,new_lpn)
) ENGINE=InnoDB;

-- Resource database: register only the new ordinary-label template; existing report rows stay unchanged.
INSERT INTO report(company_id,warehouse_id,description,type,file_name,orientation)
SELECT NULL,NULL,'Preprinted LPN Label','PREPRINTED_LPN_LABEL','preprinted_lpn_4x6_203dpi.prn','PORTRAIT'
WHERE NOT EXISTS (SELECT 1 FROM report WHERE type='PREPRINTED_LPN_LABEL' AND company_id IS NULL AND warehouse_id IS NULL);
