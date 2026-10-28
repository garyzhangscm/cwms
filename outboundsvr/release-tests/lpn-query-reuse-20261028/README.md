Single-LPN candidate reuse

Reuse one eligible availability response for the subsequent allocation query only within the same preparation transaction. Exact item/status/location/LPN matching; single-row response only; additional filters fall back; single-use snapshot discarded on return/exception. Movement source row locks, saved request exclusion and held quantity protections are unchanged.

Baseline: manual-pick-queue-20261028-04. Changes only ManualPickPreparationScope and nested Context. No schema, mobile, formal Web or Fay changes. Production timing remains to be measured on user scans.
