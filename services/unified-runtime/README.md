# Shared Colton / FAY MES service images

Each MES service uses one immutable image in both factories. Database connections,
authentication material, file mounts, warehouse settings and capability flags stay
site-local. The Java `integrationservice` intentionally keeps its separate images
and configuration. Kubernetes infrastructure images are outside this application
release. Existing Web, Work Order and QC report images were already shared.

`config/targets.json` pins nine service targets. Four are new shared images:
Inventory, Outbound, Resource and Inbound. Admin, API Gateway, Auth and Common use
the exact Colton v1.62 artifact: comparison found their application classes,
resources, dependencies and Java base layers identical to the running FAY v1.60
artifacts, except version metadata. Layout is normalized to its already shared
image digest.

## Capability configuration

`config/sites.json` is the release's explicit environment-variable configuration.
Do not choose behavior by IP address, warehouse name or a hardcoded factory name.

| Variable | Colton | FAY | Behavior |
| --- | --- | --- | --- |
| `MES_INVENTORY_ATTRIBUTE_LABELS` | true | false | Attribute-rich versus existing item/quantity inventory label path |
| `MES_INVENTORY_ACTIVITY_WAREHOUSE_TIME` | true | false | Preserve each deployment's existing activity date filter interpretation |
| `MES_RFID_LOOKUP_ENABLED` | false | true | Register the authenticated, client-restricted RFID UPC lookup endpoint |
| `MES_OUTBOUND_MANUAL_PICK_REMAINDER` | true | false | Shipment remainder versus existing shipment-line quantity calculation |
| `MES_OUTBOUND_MANUAL_PICK_REPORT_V2` | true | false | Matching Manual Pick row preparation and Resource template set |
| `ALLOCATION_CANDIDATES_COMPACT_ENABLED` | true | false | Keep compact allocation transport confined to its existing caller |
| `MES_RECEIVING_EXTENDED_LABEL_CONTENT` | true | false | Description wrapping and stock UOM in receiving labels |

Absent legacy-selection flags default to the Colton implementation. RFID remains
disabled unless explicitly enabled. The deployer always supplies both sites' full
flag sets; pair Outbound and Resource report settings. System properties with the
same uppercase names can override environment flags in isolated regression tests.
RFID uses Spring's `mes.rfid.lookup.enabled` property and relaxed environment binding.

Inbound additionally handles missing descriptions, default package and stock UOM
without introducing a new label failure. FAY's original label format is retained
when extended content is disabled. RFID company/warehouse/client restrictions and
check-digit validation remain in the shared implementation.

## Build and code provenance

Historical formal images contain fixes beyond a plain Maven build of the current
branch. This release preserves those verified production artifacts rather than
silently discarding them. `config/input-artifacts.json` pins their image digests,
JAR paths and SHA-256 checksums. The formerly local-only Colton Outbound JAR is also
archived as a published build-input image; it is not a separate running service.

`tools/prepare.py --output <inputs>` obtains these pinned artifacts without server
access. It creates containers only to copy the JARs; it never starts them.
`tools/build.py` takes `--inputs <inputs> --compat <inputs>/compat --output <build>`
plus `--java-home`, `--asm-core` and `--asm-tree` paths. Use JDK 17+ and ASM core
9.7.1 / tree 9.5. The method switch tool retains the exact verified legacy methods
and dispatches according to the documented capability flags. The source fixes for
Inbound and Resource are compiled and only the selected methods are merged. RFID
source is compiled against the same production dependency set.

Every other original JAR entry is checked byte-for-byte for unchanged content.
No dependency upgrade, new inventory movement logic, new database column/table,
or Integration implementation is included. Resource ships both original report
sets; startup chooses the set before the existing template copy operation, and
continues honoring each site's existing custom-template mount.

`tools/check.py` runs actual candidate classes with both configurations.
`tools/check_images.py` repeats those checks in the published images' Java runtimes,
using disconnected, read-only containers. The checks cover JVM bytecode verification,
14 receiving-label cases including absent data, RFID validation and registration,
Manual Pick display rows, and both sets of all three report resources. Real import,
receiving, picking and shipping transactions are not QA fixtures.

## Deployment and recovery

`tools/deploy.py` runs on the authorized factory master. It refuses unknown baseline
images and excludes Integration. For each service it privately backs up the complete
Deployment and persistent manifest, applies the shared image and that site's flags,
waits for readiness, checks service connectivity and runtime JAR SHA-256, and persists
the verified change to `/root/k8s/3-framework.yaml` or `5-backend-app.yaml`.

Existing JVM memory settings and site mounts remain. Pods stay on their previous
healthy application node; workloads without a readiness probe receive a TCP readiness
check so old pods are not removed merely because the new Java process has started.
Database-backed services explicitly retain `ddl-auto=none`; Integration's existing
`validate` configuration is untouched. The deployer detects Integration template drift.

Private backups are under `/root/k8s/backups/unified-runtime-*`. A failed service is
restored to its prior Pod template when no concurrent template change is detected;
previously completed services remain recorded in `results.json`. Restore image,
capability settings and matching Resource/Outbound report settings together. Do not
restore business data or erase import queues as part of image rollback.
