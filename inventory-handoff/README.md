# Inventory handoff location settings

Run this admin-only service on `k8s-app1`. The Inventory → External Handoff Locations page searches enabled, physical WMEC locations and saves up to 20 location IDs in `/var/lib/cwms-inventory-handoff/locations.json`. The service validates MES admin login and uses an optimistic revision to prevent stale overwrites. This setting does not adjust inventory or enable a scheduled job.

The service uses `config.example.json` for non-secret MES endpoint configuration. Deploy the Python file to `/opt/cwms-inventory-handoff`, the config to `/etc/cwms-inventory-handoff/config.json`, and the systemd unit to `/etc/systemd/system`. The web dev proxy forwards `/api/inventory-handoff/*` to port 18791 on app1.

Run tests with `PYTHONPATH=inventory-handoff python3 -m unittest discover -s inventory-handoff/tests -v` from the repository root.

`run_handoff.py --dry-run` scans the selected locations without mutations; `--execute` rechecks each inventory before calling MES `DELETE /inventory-adj/{id}`. It uses a stable `EXT-HANDOFF-{inventoryId}` document number, an English handoff comment and a dedicated activity actor. A successful HTTP response is recorded as `SUBMITTED`; an ambiguous timeout is recorded as `UNCERTAIN` and is never retried automatically within the run. The optional `--verify-after` mode also checks the virtual location and activity after each adjustment. Locked, picked, moved, or changed inventory is skipped. The systemd timer runs on app1 at 23:00 America/Los_Angeles; it does not catch up a missed run after reboot. The first run will process existing inventory in selected locations as well as future arrivals.


### Batch submission

Set `submissionMode` to `batch` to use the existing MES Send Remove Request API.
All eligible inventory IDs are sent in one DELETE request to `/inventory/batch-remove`
with `companyId` and `asyncronized=true`. MES runs its background removal workers.
This endpoint does not accept document numbers or comments. Deploy the durable removal
queue hotfix before enabling this mode; the hotfix no longer marks inventory virtual
at admission. A `SUBMITTED` report with `BATCH_ACCEPTED`
records means only that MES accepted the request, not that every adjustment completed.
No post-adjustment inventory or activity checks are made. Previous attempted IDs
(including uncertain requests) are excluded using the retained execute reports;
do not delete these reports or automatically retry uncertain records.
