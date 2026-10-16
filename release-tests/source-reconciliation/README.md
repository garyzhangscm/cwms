# V1.64 source reconciliation

The deployed compact allocation transport, manual-pick report error handling and simplified report template are integrated into the standard service source trees. Preprinted LPN and manual-pick split source is also in those trees. Rebuilding these services no longer requires files from an external patch directory.

Outbound explicitly declares Spring AOP. Compact transport remains opt-in through `allocation.candidates.compact-enabled` (default false). Preserve the intended deployment setting when rebuilding; neither credentials nor environment-specific configuration belong in this repository.

Run `verify.py --inventory-jar /path/to/inventory.jar --outbound-jar /path/to/outbound.jar --resources-jar /path/to/resources.jar --java-home /path/to/jdk17 --test-libs /path/to/test-libs.json`. The JSON file lists local Mockito, Byte Buddy, Objenesis, compatible Spring Test and Hamcrest JARs. Checks compile the merged source using each service's existing dependencies, exercise the real MVC/AOP paths with synthetic data, validate split transactions and report failures, and render the current Jasper template. All service checks deny network access.

This targeted check supplements the normal Maven build; it does not establish that every pre-existing module builds cleanly. Production artifacts and business fixtures are not checked in. The allocation-task persistence proposal under `docs/proposals` remains undeployed and outside service compilation.
