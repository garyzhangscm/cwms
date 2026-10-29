# Shipment completion batching

The optional `SHIPMENT_COMPLETION_BATCH_ENABLED=true` Outbound path applies to ordinary order shipments. Transit-selected customers and trailer-only completion keep the existing path. Deploy Inventory before Outbound and confirm the schema/capability before enabling the flag.

Inventory preview returns a slim, warehouse/shipment-scoped snapshot. Outbound validates complete Picks, no short allocations, staged locations, matching items, per-Pick quantity ceilings, and shipment/order remaining quantities. Each affected shipment/order line is locked and saved once after the inventory batch succeeds. Per-inventory billing events remain.

A durable receipt is keyed by warehouse and shipment. The immutable inventory/quantity/Pick/source/owner fingerprint prevents a different request from replacing an existing batch. Inventory moves use the existing ship operation. Each movement and its progress marker share a JPA transaction with JDBC; completed rows are skipped on retry. After a partial failure, the remaining rows resume from the original snapshot. All movements must succeed and no additional staged stock may remain before the batch is completed. Outbound quantities are updated only after a full validated result. Completed shipment retries do not increment totals again. These are per-inventory commits, not a distributed atomic transaction across both services. A concurrent unexpected stock change fails closed and may require review.

No automatic receipt deletion: receipts support recovery if Outbound rolls back after Inventory commits. Do not fall back to the legacy shipping loop after a batch timeout. Do not roll back to an incompatible build with unfinished batches without reviewing those receipts.

Tests use synthetic fixtures only: successful batch; same-request replay; changed request rejected; partial failure and resume; source quantity change; missing snapshot row; concurrent same-shipment submissions; real JPA/JDBC commit/rollback; 33-row outbound aggregation, single snapshot/POST/line save with 33 billing events; unfinished Pick protection. Production validation is read-only until an operator performs a real completion.

Performance log markers: `shipment-complete-batch` (prepare/ship/finalize) and `shipment-inventory-batch` (prepare/moves and receipts). No latency promise is inferred from isolated tests.

## Stock-row lock correction

A JPA pessimistic find/refresh on Inventory joins eager Item and InventoryStatus associations. On MySQL this also locks shared master rows; a concurrent stock insert's foreign-key checks can form an Item/Status lock cycle. The batch now obtains `SELECT inventory_id ... WHERE inventory_id=? AND warehouse_id=? FOR UPDATE` first, then reads/refreshes the entity without a JPA lock hint. The stock row remains locked for quantity/owner/Pick/source checks and movement/receipt commit. No eligibility checks or progress records are removed.

The isolated MySQL 8 test reproduces the old shared-master lock/FK-insert deadlock, then runs six concurrent pairs with a stock-only lock without deadlock. It uses synthetic tables in a dedicated no-network test container. The batch regression also rejects any reintroduction of pessimistic eager find/refresh and verifies partial progress/resume and concurrent idempotency.
