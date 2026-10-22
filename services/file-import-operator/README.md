# File import exception operator image — 2026-10-22

This additive service packages the existing Colton Work Order and Item exception
APIs for container deployment. Source was recovered from the actual active app2
services and verified against their hashes, without copying runtime credentials,
queues, business CSVs or state into Git or image layers. The generic current Item
operator also supplies the Work Order API; the original 93 import/auth/queue tests
pass unchanged. Six additional checks cover factory prefix isolation, retained
source-file behavior and explicit rejection when an import source is unconfigured.

The source incorporates FAY's existing configurable Item filename prefix and
`deleteSourceFiles` policy. Defaults remain Colton's original int_item matching and
source cleanup. Explicit FAY settings must be retained. A retained completed file
must not block later batches. Prefix and cleanup-policy validation occurs before
any network request. These changes do not alter the host's existing daily import
services or schedules.

Build: `docker build --platform linux/amd64 -t brianwang1912/mes-import-operator:v20261022-02 .`
The build runs all 99 mock/temporary-file tests before assembling runtime code.
The runtime contains only Python modules, no tests, examples, credentials or state.

Run with `--kind items` or `--kind work-orders` and `--config /existing/config.json`.
Mount configuration and referenced credential files read-only at their existing
absolute paths; mount the appropriate existing state directory read/write. Use
the existing service UID/GID, host networking for the site's VPN/API reachability,
a read-only root filesystem and exactly one process per queue/state directory.
Stop the corresponding host API before taking over its port; keep its original
systemd unit for rollback. Back up state and units privately on the server. Refuse
cutover while jobs are queued/running, and verify no queued job is created by QA.
Do not change the daily import timers or enable FAY's inactive batch importer.

For a source not configured at a factory, `--unconfigured-reason` starts an explicit
unavailable endpoint: health reports configured=false; all business methods return
503 with that message. It creates no queue and starts no worker. It never points at
another factory's source or fabricates a healthy empty exception list. FAY has no
existing Work Order source; the user selected the explicit unconfigured endpoint and prohibited retry while preserving the original import setup.

After cutover, verify health and unauthenticated rejection through the new image,
then perform authenticated read-only browser acceptance. Do not test retry against
real batches or upload/create a real sales order as a deployment check.

Runtime: Python 3.12 Alpine is used for compatibility with the existing Docker 20.10.8 hosts. The first glibc candidate failed the empty-queue preflight before any live API was stopped. No global Docker upgrade or seccomp relaxation is part of this release.
