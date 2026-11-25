# Fay work order import permissions · V19.1.65

Fay deployed this release on 2026-11-25. The web client uses [this source revision](https://github.com/garyzhangscm/cwms-client/commit/06c834f40730fe9e50cd3d8edb77299f1780211b).

The web client deployment in the Fay staging namespace uses the image pinned in `webclient.patch.json`. Use the file as a Kubernetes strategic merge patch to the existing deployment. The saved cluster manifest is `/root/k8s/4-frontend-app.yaml`. The prior image was `docker.io/brianwang1912/cwms-client:V19.1.65@sha256:1b66713b0c150b5dd5cf326c14eea2fb9c0fa2eecffd6ab710bd088018f7cc56`.

Build the app2 operator image from the `services/file-import-operator` directory with `docker build -f deploy/fay-single-work-order/Dockerfile -t mes-import-local:fay-single-wo-20261125 .`. Its base image is retained on app2. The deployed systemd override lives in `/etc/systemd/system/cwms-fay-work-order-operator.service.d/zz-single-work-order.conf`. Check the operator queue is idle before restarting the service.

For rollback, restore the prior web image and remove this release's systemd override, reload systemd and restart `cwms-fay-work-order-operator`. The existing `first-report.conf` selects the previous operator image.

Users with listing access can recheck or retry one work order in its source batch. Batch actions and missing-item recovery require an administrator or Work Order Import Operator. Production acceptance used isolated action requests and read-only deployment checks.
