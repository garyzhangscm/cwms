-- Fay cwms: add once; do not rerun if index already exists.
-- Online build still consumes IO/CPU; execute during a quiet period.
SET SESSION lock_wait_timeout=10;
ALTER TABLE cwms.inventory
 ADD INDEX idx_inventory_reserve_capacity
 (warehouse_id,item_id,inventory_status_id,pick_id,allocated_by_pick_id,
 virtual_inventory,inbound_qc_required,locked_for_adjust,client_id,location_id,quantity),
 ALGORITHM=INPLACE, LOCK=NONE;
SHOW INDEX FROM cwms.inventory WHERE Key_name='idx_inventory_reserve_capacity';
