# Durable inventory batch removal

The existing `/inventory/batch-remove` entry point now uses one process-wide queue,
with ten workers by default, 2,000 pending/running records maximum, and 1,000 IDs
per request. Both values of the legacy `asyncronized` parameter now submit actual
background work; the old false branch previously returned success without doing work.
The legacy `remove request has been sent` response is preserved for the UI.

The queue atomically persists each admitted batch before executing anything. Records
are not marked virtual at submission. Workers invoke the ordinary adjustment path,
including its approval rules and activity logging, in a local database transaction.
The original actor is propagated only on the worker thread and cleared afterward.
Changed, virtual, locked or allocated records are skipped before mutation.

JSON reports in `/var/lib/cwms-inventory-removal` contain QUEUED, RUNNING, COMPLETED,
AWAITING_APPROVAL, SKIPPED_CHANGED_OR_UNAVAILABLE or UNCERTAIN states. COMPLETED
means the business method and local transaction returned successfully; it is not an
independent inventory/activity verification. Failures are logged and persisted. On
restart QUEUED records resume; RUNNING records become UNCERTAIN and are never
replayed automatically. Submitted inventory IDs remain deduplicated across restarts.
Reusing a previously adjusted/restored inventory ID needs explicit reconciliation;
do not delete report files to force retry. Archive/back up reports with their dedup
semantics intact. A persistent directory does not survive loss of the app1 disk.

A filesystem lock enforces a single queue owner. The deployment uses one replica,
Recreate, app1 affinity and a hostPath. Shutdown stops new work, waits up to 120s,
and leaves unstarted work on disk. The 150s pod grace period accommodates this.
A business call can still be slow or stuck in an external service; the fixed workers
prevent accumulating more execution threads, not guarantee an execution deadline.
The local transaction uses a 120s database timeout. External integration side effects
are not part of this database transaction, so uncertain operations need review.

`build-removal-queue-hotfix.sh` compiles only new classes against the deployed v1.62
JAR and patches two existing methods with Spring's bundled ASM. It preserves all
other deployed bytecode and dependencies. The equivalent source changes are in
InventoryService and UserService. Run it inside the existing image with `/work`
containing original.jar, its extracted baseline/, the three tools Java files and
DurableRemovalQueue.java / InventoryRemovalQueue.java. Regression tests do not call
production APIs. Use `Dockerfile.removal-queue` and the deployment patch afterward.

The deployed image stays local to app1, following the current decision to defer
publishing images. The Python handoff job switches to `submissionMode: batch` only
after this service is deployed. HTTP acknowledgment means batch acceptance, not
completion. Nightly timer enablement is a separate operation.
