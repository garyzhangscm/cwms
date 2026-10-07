# Manufacturing Manual Pick policy

Warehouse Configuration now includes two independent settings:

- `manufacturingIssueRequireSourceLocation`: match the source location of an open allocation.
- `manufacturingIssueRequireAllocatedLpn`: match its LPN when that allocation specifies an LPN.

Both default to enabled. Missing/null configuration is interpreted as enabled; configuration lookup errors stop the request. Old clients omitting the fields preserve saved settings.

The outbound Manual Pick service checks rules before creating allocation records. Original Picks are scoped to warehouse, work order line, material and the selected production line's inbound staging location. Only unfinished Picks are considered. When both settings are enabled, the same original Pick must satisfy both. No open original Pick means the existing Manual Pick rules apply. Location-only allocations do not create an artificial LPN requirement.

The phone app does not need a new endpoint or UI. Existing material, quantity, inventory status, demand and reservation checks remain. Disabling these settings does not cancel/release/adjust existing Picks, does not allow stealing reserved stock, and does not guarantee Manual Pick when demand is already fully allocated. Existing outbound shipment Pick confirmation is unchanged.

## Deployment

1. Confirm the target database and inspect `SHOW COLUMNS FROM warehouse_configuration`.
2. Apply `docs/sql/manufacturing-issue-policy.sql` once, before deploying the new layout service. It adds two columns and does not modify inventory/work order/Pick rows. If columns already exist, inspect their definition instead of rerunning the ALTER. Test the migration on a database copy first.
3. Deploy the updated layout service, then outbound service, then Web. Work order and phone code require no deployment for this feature.
4. Verify the two settings save and reload for each warehouse. Use controlled Manual Pick tests for all four combinations, including absent original allocations and location-only allocations. Live tests may create business records.
5. Reverting application images may leave the added columns in place. Older layout code ignores them. Older outbound code does not enforce these rules.

## Local validation

- Outbound: 12 unit tests, including policy rejection before allocation, rule combinations, absent allocation, same-Pick matching, missing configuration/context and lookup failure.
- Layout: 2 unit tests for omitted fields and explicit choices. Existing legacy JUnit 4 context test cannot compile with the current dependency set; local validation excludes that unrelated test using a temporary Maven POM.
- Web: production Angular build passed in the existing local build container with network disabled. Existing CommonJS warnings remain.

## Test deployment — 2026-10-07

The user added the two columns in Colton. Standalone Layout and Outbound test jars run in `staging/manufacturing-issue-preview`, separate from formal services. Test-only jars remove the Layout startup initializer, the outbound Kafka receiver and outbound scheduling; automatic DDL and service registration are disabled. The configuration facade verifies the caller with the existing gateway, checks warehouse/company ownership, and requires an administrator for writes.

Only the Warehouse Configuration requests from `10.0.202.70:4200` route to the facade at `10.0.10.159:31530`. The page forces a fresh configuration read. Saving edits updates the actual Colton database. The phone's Manual Pick route remains on formal services; this test configuration deployment does not enable new enforcement on the phone. Formal application images were not changed. No successful configuration save, allocation or material issue test was submitted by the agent.

New field reads, 3/3 readiness, Web compilation and unauthenticated GET/POST rejection were verified. User-authenticated save/reload still needs manual testing.

During the initial preview startup, old Layout initialization ran and the custom Kafka factory ignored the standard listener shutdown flag. The test deployment was stopped immediately and rebuilt without those components. Checked logs did not show message processing. The old and new static location-type CSV records are semantically identical; exact database differences from initialization were not established.


## Colton formal release — 2026-10-07

The user confirmed the configuration and explicitly authorized Docker Hub publication and Colton deployment. Layout, Outbound and Web rolled out successfully. The phone app is unchanged; the formal Manual Pick backend now applies the new warehouse policy. Both settings were read back as `false`, preserving the user's saved choices. Material, quantity, demand and existing reservation checks still apply; no automatic cancellation/release/adjustment of old Picks was introduced. Fay was not changed.

Backend release jars were built from the actual Colton formal jars, with only the manufacturing configuration/policy classes replaced. All other classes, dependencies and resources were verified byte-identical. Production message consumption and scheduling remain intact. Actual release-image probes passed 12 policy scenarios, legacy client configuration preservation, and default formal configuration URL resolution with network disabled. Live jar hashes match the release manifest. No allocation or material issue was performed by the agent.

Images published:
- `brianwang1912/cwms-layoutserver:v1.62-colton-manufacturing-20261007`
- `brianwang1912/cwms-outboundserver:v1.62-colton-manufacturing-20261007`
- `brianwang1912/mes-web:v19.2-colton-manufacturing-20261007`

The Web repository denied anonymous node pulls. The user established an application-node SSH session and the exact built image was exported/loaded there. Web uses its unique version tag with `IfNotPresent`; its loaded image configuration SHA was verified. No Docker socket loader Pod was created. Backends use registry digests.

Persistent configuration: `/root/k8s/5-backend-app.yaml` and `/root/k8s/4-frontend-app.yaml`. Original deployment/YAML backups: `/root/k8s/backups/colton-manufacturing-20261007/`. The temporary image warmup DaemonSet was removed.
