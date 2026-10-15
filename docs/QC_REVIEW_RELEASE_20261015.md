# QC exception review release — 2026-10-15

The confirmed QC endpoint evaluates persisted acceptance standards, requires trimmed exception notes for failed checks (maximum 2000 characters), and requires explicit failure acknowledgement. A failed inspection remains Fail after confirmation. Pending inspections do not update completed QC quantity.

`POST /qc-inspection-requests/confirmed?warehouseId=...&failureAcknowledged=...&rfCode=...` preserves RF context and work-order sample quantity. `POST /qc-inspection-request/{id}/prepare-review?warehouseId=...` supplies persisted checkpoint IDs before input and only repairs strictly untouched legacy inbound tasks. Recorded observations and completed inspections cannot be reopened through this recovery. Existing endpoints remain available for older clients.

Required schema, already added manually in Colton and Fay:

```sql
ALTER TABLE cwms.qc_inspection_request_item_option
  ADD COLUMN exception_notes VARCHAR(2000) NULL;
```

Do not rerun if the column already exists. Tests and an offline runner are in `inventorysvr/release-tests/qc-review`.

Online inventory images:
- Colton: `brianwang1912/cwms-inventoryserver:v1.62-colton-mobile-qc-20261015`
- Fay: `brianwang1912/cwms-inventoryserver:v1.60-fay-mobile-qc-20261015`

Each image preserves its exact existing production JAR baseline, replacing only the QC option model where needed and adding QC services/controllers. Both deployments reached 1/1 available; live probes passed; the confirmed endpoint rejected an empty list before any business record was loaded. No real QC was submitted for verification.

Web production snapshot: `brianwang1912/mes-web:v19.2-colton-qc-notes-20261015`.
Web complete recent-source backup: `brianwang1912/mes-web:v19.2-all-changes-20261015`; this includes additional cache/startup recovery, receipt search and UI changes and is not a production rollout request.

Password reset role preservation is also saved with this commit; it uses the dedicated password/scalar update service and never merges or clears roles. Its existing prepared image is `brianwang1912/cwms-ressvr:v1.62-password-role-fix-20261008`. All 13 authorization and role-preservation tests passed again before upload.

Mobile testing: explicit Yes/No, inline nonconformance warning, required exception notes, final failure confirmation and uncertain-outcome status check. 48 targeted Flutter/OTA tests and static checks passed; Android and iOS test builds succeeded. User chose test packages; no OTA or App Store release is authorized in this checkpoint.
