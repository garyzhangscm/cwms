# FAY order removal repair

Repair the managed Order aggregate deletion: clean shipment children once, clear the inverse OrderLine collections before deleting the parent, and avoid merging the stale shipment graph through staging-location updates. Order removal runs in one database transaction and locks the parent order. Reject completed/allocating orders, shipped/dispatched demand, shared shipments, picks and short allocations before deletion. Existing shipment cleanup retains its historical child cleanup rules.

This release replaces only OrderService.removeOrder(Long) in the pinned FAY pallet-sheet JAR. All other methods, classes, report resources and dependencies remain byte-identical. Colton, Integration, JDA, database schema and site configuration are unchanged. No production order is removed as a release check.

Read-only FAY audit found one active order line with total shipment-plan quantity exceeding its demand. The two associated plans were created in 2023. This confirms historical excess planning, but does not establish the original trigger. Multiple shipments alone are valid for partial fulfillment and are not automatically deduplicated. The audit does not modify plans, quantities or inventory; an operator's later Remove can remove both idle plans atomically.

Validation uses actual deployed Hibernate mappings and isolated H2, including zero/two shipment plans, open picks, shortages, completed/allocating orders, shipped quantities, dispatched/shared shipments and rollback on child deletion failure. The baseline reproduces repeated child deletion failure once shipment cleanup is flushed; the candidate succeeds. Existing print-policy checks also pass.

Build with tools/build_order_remove_fix.py, pinned baseline JAR, JDK 17+, ASM core/tree. Test source: tests/OrderRemovePersistenceCheck.java. Test H2 dependency: com.h2database:h2:2.3.232 (QA only, never added to the production JAR). The local build-evidence file verifies that no other JAR entry changes.

Deploy using deploy.py --site fay --service outboundservice --config release.json on the authorized factory master. The deployer requires the exact pinned baseline, privately backs up the deployment and persistent manifest, changes only the image, verifies rollout/health/JAR identity and preserves the other services. On verification failure it restores the prior template and manifest.
