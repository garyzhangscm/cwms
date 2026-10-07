# Colton manufacturing material issue release — 2026-10-07

Adds warehouse switches for allocated Source Location and LPN enforcement during manufacturing Manual Pick. The phone app is unchanged. Existing material, demand, quantity and reservation checks remain, and existing Picks are never automatically released or adjusted.

The user added the database columns and confirmed the test configuration, then authorized the three Docker Hub uploads and Colton deployment. Formal Layout, Outbound and Web are healthy. Both saved switches are disabled. Fay is unchanged.

`release.json` records the image references, runtime validation, backups and deployment state. `patch-manifest.json` records the actual formal baseline and release JAR hashes. Only listed classes were replaced; all other formal JAR entries were verified unchanged. Web was built from frontend commit ca6b8ef plus this feature's five files.

The tools preserve the exact release-build procedure and offline JAR probes. Rebuilding requires the recorded baseline JARs and local JDK/dependencies; do not substitute a different baseline without reviewing it. See ../../MANUFACTURING_ISSUE_POLICY.md for configuration behavior, migration and deployment details.

The private Web image was loaded using the SSH session established by the user. Its node image configuration hash was checked; the persistent deployment uses its unique version tag with IfNotPresent. The tested Docker Hub registry digest is also recorded. Backends use registry digests.

No allocation or material issue business test was performed by the agent. Actual phone testing is left to the user after release.
