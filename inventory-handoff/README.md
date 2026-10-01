# Inventory handoff location settings

Run this admin-only service on `k8s-app1`. The Inventory → External Handoff Locations page searches enabled, physical WMEC locations and saves up to 20 location IDs in `/var/lib/cwms-inventory-handoff/locations.json`. The service validates MES admin login and uses an optimistic revision to prevent stale overwrites. This setting does not adjust inventory or enable a scheduled job.

The service uses `config.example.json` for non-secret MES endpoint configuration. Deploy the Python file to `/opt/cwms-inventory-handoff`, the config to `/etc/cwms-inventory-handoff/config.json`, and the systemd unit to `/etc/systemd/system`. The web dev proxy forwards `/api/inventory-handoff/*` to port 18791 on app1.

Run tests with `PYTHONPATH=inventory-handoff python3 -m unittest discover -s inventory-handoff/tests -v` from the repository root.
