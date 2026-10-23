# FAY → Olivet inventory retained in Trans_shipping

## Agreed behavior

For FAY / company 1 / WMEC warehouse 1, shipment completion for customer 1
(Olivet International Inc.) or 25 (Olivet Fortsmith) moves the shipped LPN inventory
to existing location 16169 (`Trans_shipping`) rather than an order's virtual location.
It remains non-virtual four-wall company inventory. Shipment and order completion,
quantities, existing outbound notifications and delivery-note printing retain their
existing behavior. The existing inventory shipping operation still records the
shipment time. No new inventory status or database schema is introduced.

Inventory stays in this location until the team handles it manually. There is no
JDA integration, receipt event listener, automatic clearing, scheduled clearing,
or new clearing button. Partial receipts and reconciliation remain manual.

All customers outside the explicit customer whitelist, other warehouses, and a
feature-disabled deployment use the original shipping destination operation without
reading the new configuration file. Existing stock is never migrated by enabling,
saving or disabling a rule. The shipping dock remains non-pickable; this feature
does not bypass picking restrictions or resolve attempts to pick stock already in
Trans_shipping.

## Scope and safety

The exact FAY deployed outbound JAR is the build baseline. One existing method's
bytecode changes: `ShipmentService.completeShipmentByOrder`. All other existing
JAR entries and existing method bodies are compared byte-for-byte during build.
New transit package classes supply validation, file-backed rules and admin APIs.
This supports the current order-linked sales-order shipping path. The separate
trailer-only shipment path, without a linked order/customer, remains unchanged.

The feature defaults off. Customer and location IDs are restricted by deployment
environment; the admin API cannot expand this scope. The destination must be in
the source warehouse, enabled, non-virtual, four-wall and non-pickable. Selected
shipments are blocked if active configuration is unavailable or the destination
becomes invalid, preventing an accidental switch to virtual inventory. Unselected
customers remain on their original route even if the file is unavailable.

GET and PUT `/outbound/in-transit-shipping/rules?warehouseId=1` (through the gateway)
require a bearer token verified by MES Auth, an enabled unlocked company user and
administrator rights. Caller-supplied username headers are not trusted. Saves
validate warehouse/customer/location scope, record the verified actor and timestamp,
and require a matching revision. File writes use locking and atomic replacement.
The file must be on durable writable storage; use one outbound replica for this
file-backed release. Keep backups and avoid manual concurrent edits to this file.

## Build and validation

Put the exact deployed JAR at `build/baseline.jar` (ignored in Git). Its required
SHA256 is `15da73e384fa4ba0cd1ff94f7c44e9a7548c01c6cffe22646936fc07047dbbcc`.
Run `python3 tools/build.py --java-home /path/to/jdk17` from this directory.
The candidate is `build/outboundservice.jar`;
`build/manifest.json` records hashes and entry/method comparisons. Tests use fake
remote clients and an overridden inventory completion operation, so they perform
no live shipment, pick or inventory writes.

The source is maintained in the shared v1.64 checkout. Build the narrow candidate
against the verified current shared JAR; do not replace it with an unrelated full build. Package the narrow candidate with the
provided Dockerfile, preserving the exact currently deployed runtime. Rebase and
revalidate the patch if the production image has changed.

## Web configuration card

`web/` contains a standalone Angular card for the existing outbound configuration
page. Run `python3 tools/apply-web.py /path/to/isolated/cwms-client` to apply the card
and imports. It preserves the existing form and its save logic. It appears only
for enabled warehouses and admin accounts, offers the two allowed customers and
the validated destination list, and explains manual inventory handling. Saving
requires confirmation. Conflicting/failed saves require a reload. Existing servers
without the API and non-admin users retain the existing page.

The Web overlay must be compiled against the current Web source before release;
it is not a replacement for the full Web application. Do not deploy unrelated Web
changes from a different environment.

## Enablement checklist

1. Recheck FAY outbound's actual image digest, warehouse/customer IDs and location
   16169 properties. Confirm the order-linked shipment path is the operational path.
2. Review the candidate hash and passing test output. Build and tag a candidate
   image using `docker build -f deploy/Dockerfile .` from this directory.
3. Seed `config/fay-rules.example.json` as `rules.json` in a durable directory with
   permissions for the running container. The enabled sample applies to customer
   IDs 1 and 25. Back it up. Mount the directory at `/var/lib/cwms-in-transit`.
4. Apply the scoped environment values from `deploy/env.example.yaml` to the FAY
   outbound deployment only. Keep inventory, inbound and other customer deployments
   unchanged. No business database migration or location-group change is needed.
5. Verify admin GET returns the correct customers, physical destination and revision.
   In an isolated test environment, complete one selected and one unselected order:
   selected inventory must remain non-virtual at 16169 and unselected inventory must
   follow the original virtual order location. Check order/shipment quantities,
   duplicate completion prevention, reporting visibility and manual handling.
6. Compile and review the Web card in the existing outbound configuration page.
   Release only the scoped artifacts after environment verification.

Disable `FAY_TRANSIT_ENABLED` (or restore the exact prior image) to revert future
shipping to the original behavior. This never clears or moves inventory already
retained in Trans_shipping; the team must handle that stock manually.

This directory contains code and candidate artifacts, not proof of a production
rollout. No live inventory movement is part of these build/regression checks.

2026-10-23: rebased on the shared work-order pick fix, preserving every other JAR entry. Actual image validation includes 45 isolated shipment-completion checks and the prior pick-scope checks for both site flags. Production images and backed-up rollout script are recorded in the adjacent unified-runtime release.
