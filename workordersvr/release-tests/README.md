# Released Work Order QC regressions

These standalone regressions do not call live services or write business records. Compile the module normally before exercising actual classes; a return-descriptor change requires callers to be rebuilt.

`python3 release-tests/check_shift.py` extracts the actual shift-validation methods, compiles them with dependency-only stubs and checks adjacent boundaries, overnight overlap, invalid durations and repeated boundary stability. Set JAVA_HOME when Java is not on PATH.

The Java harnesses use the module classes plus its normal runtime dependency classpath. Compile to a temporary/test output directory with `javac --release 17 -cp <module-runtime-classpath> -d <test-output> release-tests/*.java`.

Run CancelPickResponseRegression for empty/single/multiple cancellation lists, unchanged IDs/quantities, propagated rejection, one request per attempt, and the real assignment caller. RegistrationRegression checks Spring injection, current registrant, generic identity rejection, creation-only callbacks and historical audit preservation. ReleaseRegression runs the actual shift service's pure validation methods. RegistrationJpaRegression additionally requires H2 and exercises actual Hibernate persistence with a Spring bean container against an isolated in-memory database; pass the compiled module-classes directory as its only argument. Network-free container execution was verified for the released snapshot.

No credentials or production configuration are needed. Do not point the H2 integration harness at a business database.
