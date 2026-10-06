# Safe deletion of unused work orders

API: `DELETE /api/workorder/work-orders?warehouseId=<selected warehouse>&workOrderIds=<comma-separated IDs>`.

The warehouse is required. IDs must be positive integers; a request supports at most 50 IDs. IDs are deduplicated and locked in ascending order. Every order is validated before any deletion. Validation, deletion and flush run in one SERIALIZABLE transaction; any failure rolls back the entire batch.

Only PENDING orders qualify. Production, QC and by-product activity, allocated/processed material, production plan/flow, short allocation and outbound document links block deletion. Ordinary unused material lines, instructions and by-product definitions are owned draft data and may be removed through the existing JPA cascades.

The guard inspects the shared MySQL schema for references by work_order_id, material_work_order_id, consume_from_work_order_id, work_order_line_id, work_order_by_product_id and work_order_number. It covers current and archived inventory, current and cancelled picks, allocations, line/worker assignments, production/QC/history records and other documents following these reference conventions. Integration order and confirmation records are also checked by number and warehouse; unknown warehouse references block conservatively. Even a zero-quantity or deassigned history record blocks physical deletion.

Cross-service inventory/outbound tables must be visible in the same database. Missing mandatory tables/columns or failed queries stop deletion. This feature does not support deleting historical records or deleting across warehouses. A schema using different reference conventions requires review before enabling deletion. Database transaction locks protect the validation/deletion window; external asynchronous writers without database foreign keys are not a substitute for referential integrity.

Tests: 11 JUnit scenarios passed with zero skips, including real SQL/schema inspection and transaction rollback on isolated MySQL. The repository is mocked for fixtures; real JPA cascade mappings are unchanged. The MySQL fixture requires an explicitly supplied localhost URL ending in cwms_delete_test and never accepts a production server. Enable tests explicitly because this repository defaults maven.test.skip to true:

```
scripts/mvn-local -f workordersvr/pom.xml -Dmaven.test.skip=false -DskipTests=false -Dtest=WorkOrderDeletionServiceTest -Ddeletion.test.mysql.url=jdbc:mysql://127.0.0.1:<temporary-port>/cwms_delete_test test
```

Deploy the protected Work Order backend BEFORE the Web deletion entry. Production servers use older backend versions: preserve their exact runtime/dependency baseline when packaging, rather than replacing them with the local v1.64 artifact blindly. No production order was deleted during implementation or testing. No schema migration is required.

## Test environment enabled

The user authorized use of the Colton formal database and confirmed a backup. The test Web on 10.0.202.70:4200 routes development deletion requests through /api/workorder-deletion-test/work-orders to the independent workorder-deletion-preview service (NodePort 31527). The service uses the cached production v1.62 models/runtime plus additive guard classes. The formal workorderservice deployment remains unchanged. No scheduling or Kafka listener components are loaded; Hibernate schema generation is disabled. Authorization is checked through an existing gateway GET before the guard executes. An authenticated GET /work-orders/deletion-check executes the same validations without deletion.

Sixteen local tests passed with zero skips after the isolated repository/entrypoint addition. Actual formal-database read-only checks accepted unused draft 2005030077-17 and rejected INPROCESS order 2025030077-17. No production orders were deleted by the agent.
