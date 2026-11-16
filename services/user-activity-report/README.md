# Warehouse User Activity

Read-only Python report for the MES route `/warehouse-report/user-activity`.
The three `activity_*.py` files match the bytes in `/activity` of the
Colton `warehouse-user-activity` container as verified on 2026-11-15.

Production image: `brianwang1912/mes-user-activity:v20261027-01@sha256:1ecf35ef684f526e32dd2e948bd920c72d3b1eb3ad3c3d6f27c9e60df7e3787b`.

- `activity_api.py` serves report summaries, detail pages, and Excel exports.
- `activity_auth.py` checks existing MES identity and Inventory Activity permissions.
- `activity_core.py` classifies and aggregates warehouse events.

The service uses the pinned image's shared report adapter and time utilities. For
local testing, place that runtime's `/app` Python modules on `PYTHONPATH` and
install openpyxl, requests, PyMySQL, and reportlab in a virtual environment.
Run `python -m unittest test_activity` from this directory. All 13 tests
passed against the verified report code with the shared runtime modules locally.

Supply the MES gateway and read-only database configuration through the site's
deployment. This directory contains no deployment credentials or database data.
`SHA256SUMS` records the verified production Python files.
