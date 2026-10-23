# Manual pick sheet and shipping print availability

The sheet prints order number, drop-off location (both with scan barcodes), Item, description, remaining pick quantity and stock unit. It does not recommend source locations or LPNs. Empty drop-off locations omit the barcode.

Open/In Process orders with remaining demand can print a manual pick sheet. Packing Slip/BOL require a shipment with completed picks, no short allocations and no open shipment-line demand, or an already dispatched shipment for reprinting. Cancelled orders cannot print. The page selects a shipment; the server validates membership/readiness and filters report inventory to that shipment. Manual demand includes unallocated order quantity and allocated but unpicked quantity.

Build report-only candidates with `tools/build_item_only_pick_sheet.py` on the pinned current artifacts. No database schema, JDA, integration settings, picking or shipping transactions are changed. Deployment preserves site configuration, records backups, verifies health and artifact hashes and restores the old template on failure.
