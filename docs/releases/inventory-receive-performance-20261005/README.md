# Inventory receive performance release — 2026-10-05

Remove two destination inventory reads used only for debug log counts. Match mixing restrictions before loading destination inventory; skip the read only when no matching rule exists. Existing matched-rule validation, inventory writes, movement/consolidation behavior and durable removal queue remain intact.

Colton and Fay use different deployed application versions. Each production image was patched from its exact baseline; only InventoryService.class and InventoryMixRestrictionService.class changed. Do not swap backend images between sites solely because their schemas match.

Colton image: brianwang1912/cwms-inventoryserver:v1.62-colton-receive-perf-20261005
Registry digest: sha256:21b056749e1b1f0cb27b2d4ea24075eefefe4fec2f1e20e2b49d6b81362d6541
Patched JAR SHA256: 11278cc7fc8794ac8deb7f233a4f32b4dc9e7b37268b2ad7f6c296879354ea47

Fay image publication tag: brianwang1912/cwms-inventoryserver:v1.60-fay-receive-perf-20261005
Registry digest: sha256:665f07e276d7d00e9368b94df15cfbd0e3c846b736e5bbfb2ed2664bc1c2ebd7
Deployed node image ID: sha256:a14f18870d019e5792e9d66488510e79d4f12ed2e97c52f171d9625e76c77d1f
Patched JAR SHA256: 05f2c6a6e0853024e8e0d75bc93fdf4d5b7aacaff26c4db5626f61055f3dfcdb

Validation: bytecode verification; no-rule and unmatched-rule paths; matching allow/reject rules; empty destination; multiple-rule short circuit; removal of debug count reads; durable queue methods retained. Both deployments passed readiness and live probes.

Operator samples: Fay L0000474278 took 0.831 s from inventory workflow start to print acknowledgement after indexes were added; Colton L0000114914 took 0.591 s from backend production request receipt to print acknowledgement after deployment. These are individual samples and do not measure physical paper output.

The separate SQL script adds nonunique indexes for warehouse/LPN lookup and destination location lookup. Fay indexes were created by the operator. This release did not execute Colton database DDL. Check existing indexes before applying; execute the ALTER once only for missing indexes. Confirm a backup and sufficient space. INPLACE/LOCK=NONE permits concurrent DML, but brief metadata locks and additional CPU/I/O can occur. Existing rows are not deleted by adding these indexes.

The accompanying Java patcher and regression harness preserve the exact deployed baseline for older production versions. They use JDK 17 internal ASM with explicit module exports; supply extracted original BOOT-INF/classes and BOOT-INF/lib as the classpath. Patched class entries are inserted into a copy of the original JAR. Never replace dependencies or runtime configuration while creating this performance-only artifact.
