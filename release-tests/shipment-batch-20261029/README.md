# Shipment completion batching

The optional `SHIPMENT_COMPLETION_BATCH_ENABLED=true` Outbound path applies to ordinary order shipments. Transit-selected customers and trailer-only completion keep the existing path. Deploy Inventory before Outbound and confirm the schema/capability before enabling the flag.

Inventory preview returns a slim, warehouse/shipment-scoped snapshot. Outbound validates complete Picks, no short allocations, staged locations, matching items, per-Pick quantity ceilings, and shipment/order remaining quantities. Each affected shipment/order line is locked and saved once after the inventory batch succeeds. Per-inventory billing events remain.

A durable receipt is keyed by warehouse and shipment. The immutable inventory/quantity/Pick/source/owner fingerprint prevents a different request from replacing an existing batch. Inventory moves use the existing ship operation. Each movement and its progress marker share a JPA transaction with JDBC; completed rows are skipped on retry. After a partial failure, the remaining rows resume from the original snapshot. All movements must succeed and no additional staged stock may remain before the batch is completed. Outbound quantities are updated only after a full validated result. Completed shipment retries do not increment totals again. These are per-inventory commits, not a distributed atomic transaction across both services. A concurrent unexpected stock change fails closed and may require review.

No automatic receipt deletion: receipts support recovery if Outbound rolls back after Inventory commits. Do not fall back to the legacy shipping loop after a batch timeout. Do not roll back to an incompatible build with unfinished batches without reviewing those receipts.

Tests use synthetic fixtures only: successful batch; same-request replay; changed request rejected; partial failure and resume; source quantity change; missing snapshot row; concurrent same-shipment submissions; real JPA/JDBC commit/rollback; 33-row outbound aggregation, single snapshot/POST/line save with 33 billing events; unfinished Pick protection. Production validation is read-only until an operator performs a real completion.

Performance log markers: `shipment-complete-batch` (prepare/ship/finalize) and `shipment-inventory-batch` (prepare/moves and receipts). No latency promise is inferred from isolated tests.
