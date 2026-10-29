# Manual Pick rejection explanations

Additive PICK_REJECTION_V1 JSON message inside the existing error envelope. Applies after the original selector explicitly rejects an LPN as not pickable. Uses already-read inventory and an error-only location lookup; successful picking is unchanged. The original rejection remains authoritative. Missing data yields a general explanation instead of guessing.

Verified reasons: existing Pick/allocation, explicit transfer-shipping name/type/group, shipping-stage type, disabled or non-pickable location, zero stock. Other causes retain the generic explanation. No order identifiers or credentials are exposed. No database migration is required. Other backend errors retain their existing handling.

The mobile consumer displays the structured reason only on a definite preview rejection. It keeps timeout/pending-confirmation recovery unchanged and clears the scan only after the operator chooses Scan Another LPN. Inventory details are never changed by the explanation.

Build from the current Fay reserve-batch image; replace PreprintedManualPickService.class and add ManualPickRejection.class only. Do not overwrite Fay OrderService compatibility patches or other classes.
