# Colton Web, Workorder and user release — 2026-10-06

Includes guarded deletion of unused PENDING work orders, completion without an unnecessary destination, settlement saves without display-detail reloads, admin-only password reset, condition-required work-order searches, production-record labels and the revised reset-password dialog.

Validation: 34 Workorder/preview tests including 8 isolated MySQL transaction scenarios, 5 Resource password-reset tests, and mocked frontend pagination/deletion/password-reset checks. Production Angular build is required before publication. Real user passwords and business deletions were not performed by the agent.

Runtime packaging starts from the exact Colton v1.62 JARs and image digests. The WorkOrder controller DELETE method and service delegation are merged into their original classes; all three unsafe legacy unscoped delete overloads reject. Other methods remain unchanged. The exact original WorkOrderLineService changes only the completion save call to `saveOrUpdate(line,false)`. Tested completion logic and additive deletion guard/repository classes are included. Resource adds only password-reset DTO, controller and service. No dependencies or database schema are upgraded.

Build helpers are in `tools/`: compile `PatchWorkOrderRelease.java` with ASM and ASM-tree 9.5; compile `PatchCompletionSettlement.java` with Spring-core 6.2.5. Run these against the original classes, then pass the patched-class directory and settlement class to `package_release_jars.py`. Source output uses the existing module Maven configuration. Preview entrypoints are outside normal application packages and are used only for startup validation without schedulers or Kafka consumers.

Deploy the two backend images before Web. Preserve original deployment manifests and image references. A test preview routes development-only APIs separately; production uses `/api/workorder` and `/api/resource`. Fay is excluded from this release. No production database migration is needed.

Completion emits existing Kafka alerts and Integration confirmations. Imported orders with retained Integration records remain protected from deletion. Administrator resets retain other account fields and default to requiring a change at next logon. New passwords are sent in request bodies and do not appear in the new reset endpoint URL.
