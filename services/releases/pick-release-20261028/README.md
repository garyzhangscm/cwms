# Picking release — 2026-10-28

Colton and Fay Web updated to `v19.2-<site>-pick-release-20261028-02`.
Both sites serve the same built Web index SHA256 recorded in the evidence files.
Production role rendering, report date/item-family filters, shipment print guards and site-specific Nginx routing are retained. Test-only Line Clearance is excluded.

Inventory: `brianwang1912/cwms-inventoryserver:v1.62-unpick-fast-20261028-01`.
Colton Outbound: `brianwang1912/cwms-outboundserver:v1.62-pick-team-20261028-01`.
Fay Outbound: `brianwang1912/cwms-outboundserver:v1.62-fay-pick-release-20261028-01`.
Fay retains its existing OrderService.removeOrder and pallet-report classes while adding the tested quantity allocation and continuous-pick pipeline. All other pre-existing JAR resources/dependencies matched the Colton candidate.

Fay tables were created by the operator. Release code performs no DDL or stock movement. Existing allocated orders remain on the legacy allowlist. New orders require explicit Allocate/reservation before Pick. Reserve checks total stock; Pick checks the scanned LPN and remaining held quantity, with duplicate-claim protection and final row-lock validation. Unpick synchronizes reservation quantities.

Verification: 310 mobile tests; 22 queue checks; 11 real Hibernate order-removal cases on isolated H2. Fay image JAR hashes verified before switching. Pending operations drained; only the positively identified read-only inventory listing was permitted to terminate. The reservation ledger was unchanged during the switch. Backend capability checks passed and the maintenance gate was removed. Both Web releases verified Nginx/config preservation, loaded scripts, service health and the exact new index hash. An earlier Web candidate was rolled back and superseded by -02 to retain all existing production fixes.

Images uploaded to Docker Hub. Per-release OTA results are recorded separately after publication. No credentials or production data exports are included.
