# Colton outbound release source — 2026-11-13

This release archive is separate from the v1.64 development source tree. It preserves
the activated production features without replacing newer unrelated development work.

## Included code

- `yard/loaded_proxy.py`: persistent loading acknowledgements, separate dispatch, temporary Dock unavailability, and upstream validation. `test_loaded_proxy.py`: 17 isolated tests, all passed during archival verification.
- `wave-pdf/report.py`: SKU summary, actual pallet capacity/remainder calculation, Wave barcode, existing preview/download/printer endpoints.
- `wavereserve/`: recorded November 11 Wave Reserve/cancel-release Java source, activated by the web release. This reuses the Sales Order reserve pathway and protects picked work against cancellation.
- `loadlink/`: Load/Yard/Dock Java dependencies. Load order-planning source was reconstructed from the recorded November 10 patches and retained source copies, including shipping-started protection. It accepts planned assignments before picking while retaining loading readiness checks.
- `load-order-plan-schema.sql`: schema definition for the already-existing planning table; documentation only, not a migration to run again on production.

## Runtime provenance

- Existing shared outbound image: `brianwang1912/cwms-outboundserver:v1.62-wave-cancel-release-20261111-01`.
- No new shared outbound Java image or database migration was deployed for the November 13 Load eligibility change. That change was frontend-only and uses the existing order-plans API.
- Yard and PDF Python processes are separate services alongside the web container. Their base image names do not identify these mounted scripts; preserve the mounted source and persistent Yard acknowledgement storage when redeploying.
- Java source in this archive compiled successfully with Java 17 against the available outbound runtime dependencies. This is not a claim of a byte-for-byte rebuilt production JAR. Review and reconcile with the latest service tree before a future backend rebuild.
- The archived PDF script differs from the site copy only by requiring `MES_PRINTER_URL` instead of retaining the private printer address.

## Deployment dependencies

Keep the existing site routing/authentication headers and feature gate settings externally managed. Yard listens on loopback port 18816 and uses `YARD_LOADING_DB`; Wave PDF listens on port 18788 and uses `WAVE_PDF_DIR`. Provide a durable writable directory for acknowledgements and configure `MES_PRINTER_URL` for printer forwarding. PDF dependencies are requests and reportlab; Yard uses the Python standard library.

Do not copy a live SQLite database, generated shipment PDF, credentials, private addresses, or production account/order fixtures into Git. No live data is included here. `SHA256SUMS` records the archived source.
