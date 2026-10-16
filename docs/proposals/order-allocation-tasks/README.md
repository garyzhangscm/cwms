# Order allocation tasks — test-stage change

The test frontend includes strategy validation and task monitoring. This directory contains the corresponding outbound service patch. **It has not been installed on the production backend, and the schema has not been applied there.** The test frontend currently uses its legacy compatibility path against that backend.

## Behavior

- Validate every allocatable line against the registered strategy handlers before shipment planning. Report missing or unsupported strategies with line numbers. Validate again immediately before execution.
- Persist queued, running, succeeded, failed and review-required task states. A successful task means the allocation process finished; insufficient inventory may still produce Short Allocation records.
- Serialize submissions by locking the order row. A stable request ID makes repeated submissions idempotent; a unique active-order key also prevents concurrent allocations with different request IDs. A bounded two-worker queue limits background load.
- Catch worker failures, record an error code and public reason, and preserve the full exception in service logs with a task reference. Clear ALLOCATING only when it remains the current order state. Preserve a newer COMPLETE/CANCELLED state.
- Restore the original state after a failure with no planned shipment work. Use INPROCESS after partial work; retain the task's active key and require human review before another allocation. No inventory rollback or blind retry is attempted.
- Heartbeat active workers every 5 seconds. A 30-second sweep detects queued/running tasks with no heartbeat for 5 minutes, marks them REVIEW_REQUIRED, clears a stranded ALLOCATING state and blocks automatic re-execution. Recovery also works after a restart; it never automatically replays a task. A reviewed recovery procedure for retained active keys is deliberately not exposed as a public reset API.
- Keep the old allocation endpoint but route it through the coordinator. The new frontend prefers the new task API after a read-only capability probe.

## API contract

All routes remain under the existing outbound service prefix and response envelope (`data`). Warehouse IDs are required; task/order lookups are warehouse-scoped. Existing gateway authentication and endpoint authorization must cover the new paths when releasing.

- `GET /allocation-tasks/capabilities?warehouseId=…`
- `POST /orders/{orderId}/allocation-tasks?warehouseId=…`, JSON `{ "requestId": "stable-unique-id" }`
- `GET /allocation-tasks/{id}?warehouseId=…`
- `GET /allocation-tasks/latest?warehouseId=…&orderIds=…` (maximum 100 distinct IDs)

The frontend displays Checking, Submitted, Allocating, Completed, Failed or Result needs checking. Polling is read-only, stops on navigation, and never re-submits work. A lost response, network outage or monitoring timeout yields an unconfirmed result rather than a false failure/success. On a backend without this API, the UI validates strategies and observes order status for up to 2 minutes; it explicitly states that detailed task failures are unavailable. The new task API is observed for up to 10 minutes per page session. Reloading a list reattaches to known task results.

## Isolated verification

`check-backend.py` reads an existing outbound Spring Boot JAR and source checkout, builds a temporary overlay, and compiles the three changed production classes plus the new task classes. It then runs the real coordinator and JDBC store against an in-memory H2 database and a synthetic order gateway. It never connects to business inventory, the production database, or business APIs.

Run with local paths:

```sh
python3 check-backend.py --backend /path/to/outboundsvr \
  --jar /path/to/outboundserver.jar --h2 /path/to/h2.jar \
  --java-home /path/to/jdk17
```

Coverage includes validation before enqueue, worker revalidation, persistent state transitions, safe failure cleanup, partial-work review, idempotency, simultaneous submissions, queue rejection, restart recovery, warehouse isolation and latest-result lookup. Production classes compile against the existing service dependencies; full Spring/JPA/MySQL integration and a real stock allocation remain release-stage checks.

The frontend test `validation/outbound-allocation-monitor.check.cjs` runs the actual TypeScript service/component methods with simulated HTTP streams: successful/failed jobs, no-submit validation, unknown strategy, response loss, slow requests, resuming without a POST, legacy backend compatibility, cancellation and duplicate-click prevention.

## Future release steps

After explicit release approval, back up and apply `schema.sql` to the outbound service's MySQL database, integrate `backend.patch` into the outbound source, build and deploy the outbound service, then verify the capability and task endpoints through the gateway before enabling real allocation tests. The schema must exist before this backend version starts accepting allocations. Retain the table during a rollback for diagnosis; do not silently drop task history. Validate MySQL migration, gateway permissions, JPA/JDBC transaction participation, worker shutdown/restart, and the actual inventory/pick flow in an isolated backend environment before production release.

No production image, deployment, database schema or existing order has been changed by this test-stage patch.
