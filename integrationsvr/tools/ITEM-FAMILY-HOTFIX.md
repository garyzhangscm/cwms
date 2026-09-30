# Item family company-field hotfix

`DBBasedItemFamily(ItemFamily)` previously discarded `companyId` and `companyCode`.
The nested family then failed conversion with “company information is required for
item family integration”. Copy both fields; a company code is not a database ID.

## Regression against the deployed version

`ItemFamilyCompanyRegression` exercises the actual `Item -> DBBasedItem -> nested
family` conversion, company-code lookup, explicit company ID, warehouse identity,
description and ATTACHED status. It uses an in-memory layout client, no database
or network. Run against the original class to reproduce the lost-company-code
assertion, then against the patched class to verify the fix.

The public source version is v1.63 while the affected deployment runs v1.62.
For that deployment, compile only this class against its original JAR and reuse
the original image and runtime. Do not deploy the entire newer source version
as part of this fix.

`build-item-family-hotfix.sh` expects a fresh `/work` directory containing:

- `DBBasedItemFamily.java` from `src/main/java/.../model/`.
- `ItemFamilyCompanyRegression.java` from `src/test/java/.../model/`.
- The build script itself.

Run the script in the pinned original image with `/work` mounted, networking
disabled and entrypoint overridden to `sh`. It does not start the server. It
compiles Java 13 bytecode, records the baseline failure and patched success,
checks unchanged public/private member signatures, and patches the original JAR.

Before building the derived image, compare every ZIP entry by content. The only
changed entry must be:

`BOOT-INF/classes/com/garyzhangscm/cwms/integration/model/DBBasedItemFamily.class`

Keep the original JAR/image, patched JAR hashes and deployment snapshot for rollback.
Use a new image tag. A node-local image requires `imagePullPolicy: Never` and the
image must exist on every eligible node; publish to an approved registry before
expanding scheduling beyond the prepared node.

## Deployment and retry

These instances run scheduled jobs. Stop the old instance before starting the
replacement to avoid concurrent schedulers. This briefly pauses integration.
Verify actual HTTP health/startup, not only Pod Running (the existing deployment
has no readiness probe).

Existing ERROR records are not repaired by deploying this change: company fields
were already discarded when they were persisted. Keep such records as evidence.
Only after checking that no successful item or in-flight event exists, submit a
corrected, explicitly identified replacement event with its original company
information. Do not blindly resend the incomplete stored record or edit database
tables to bypass the application.

Confirm the replacement's terminal integration status and actual inventory item,
including family, packaging quantities and default flags. If the application
cannot start or regresses, stop it and restore the saved image, pull policy and
replica count. This fix does not modify schema or require Oracle connectivity.
