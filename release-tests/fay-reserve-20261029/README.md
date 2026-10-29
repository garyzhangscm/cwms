# Fay Reserve timeout / mutex deadlock fix

## Evidence and scope

FY000004087 saved a reservation of 864 despite a browser timeout. Its legacy capacity read took 45.005 seconds in a read-only reproduction. The MySQL deadlock report showed two duplicate INSERT IGNORE callers holding shared mutex-record locks and attempting exclusive upgrades.

The mutex now obtains its exclusive record lock through an upsert before the existing FOR UPDATE. The same transaction, scope serialization, revision, idempotency and quantity accounting remain intact.

Inventory capacity can use a warehouse/item/status/owner-scoped SQL aggregation grouped by location. It excludes allocated stock, open Picks, virtual stock, inbound-QC stock, adjustment-locked stock and the existing lock policy; checks enabled, pickable, four-wall locations through the existing layout client; fails closed on invalid location/quantity/overflow. The existing selector remains the default. Outbound still subtracts open Pick quantities and held reservations inside the warehouse lock. This change does not change Pick eligibility or bypass quantity checks.

Enable `QUANTITY_CAPACITY_SCOPED_SUMMARY=true` on inventory and `QUANTITY_ALLOCATION_SUMMARY_ENABLED=true` on outbound together after capacity equivalence checks. No schema change is required.

## Packaging / preserving previous work

Built from the exact running Fay images, not a generic branch build. Baseline JAR SHA256:
- Outbound: 18d627d846a83bf0aa494a3c7dc39bf1604afaa32d56ad0e23f0d64c62d4a03c
- Inventory: 110d3080997dea7ec78ed1d61f31acb7923fb5176c9c2fb672ef71d8edbb7a97

Each candidate changes exactly one ZIP entry, listed in artifacts.json. Every other entry is byte-identical, including Fay removeOrder and pallet reports. No Web/mobile image changes.

## Reproduce

Extract baseline JARs to /fix/outbound-baseline and /fix/inventory-baseline. Mount current repository at /code, this test folder at /fix, and supply H2 at /fix/h2.jar. Run compile.sh then tests.sh inside the baseline JDK image. Candidate class directories must precede baseline classes in the classpath.

mysql-lock-check.py uses an isolated local MySQL 8 container named fay-reserve-mysql-20261029 (no published ports). It reproduces the old deadlock, checks 24 updated concurrent transactions, 16 first-use transactions and a maximum held quantity of 10. It never connects to production. Change D to your Docker binary if needed.

Deployment must preload images, back up live templates, drain in-flight work, avoid mixed old/new workers and compare ledger snapshots. Roll back if capacity differs or health checks fail. Do not test by creating a second reservation for this already-reserved order.
