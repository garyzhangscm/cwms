# Batch reservation capacity

Candidate code; not deployed. Enable outbound QUANTITY_ALLOCATION_BATCH_ENABLED only after inventory batch endpoint is deployed and verified. Inventory uses existing QUANTITY_CAPACITY_SCOPED_SUMMARY. Index SQL must be executed by the user on Fay, then checked with EXPLAIN and read-only timing.

One capacity POST covers up to 200 unique warehouse/item/status/owner scopes, aggregated in one SQL request. Location IDs across scopes are deduplicated and loaded in chunks of 200 through the existing warehouse-scoped locations endpoint, including disabled locations so their policy can be evaluated explicitly. No cross-request cache is introduced. Missing locations, ACL failures, overflow, malformed scope responses and incomplete replies fail closed.

Reservation update reads its ledger once, aggregates open Picks and held quantities for relevant items, applies decreases first, and updates the in-request held total after each change. Transaction/mutex, revisions, idempotency, saved audit record and total-quantity protection are retained. No lock granularity change.

Tests include existing allocation/transaction regression, 1500 randomized inventory rows, multi-owner batch equivalence, duplicate/ACL rejection, one shared location batch, and deduplicated capacity/open-Pick arithmetic. Use the baseline runtime/H2 setup documented in ../fay-reserve-20261029/README.md.
