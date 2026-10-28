set -eu
sh /work/compile.sh
cp='/work/classes:/pipeline/classes/inventory:/reserved/classes/inventory:/split/inventory-classes:/inv/BOOT-INF/classes:/inv/BOOT-INF/lib/*'
javac --release 17 -parameters -sourcepath '' -cp "$cp" -d /work/classes /work/tests/*.java /reserved/tests/ManualPickInventoryRegression.java /reserved/tests/ManualPickFastInventoryChecks.java /reserved/tests/ManualPickLpnGuardChecks.java /pipeline/tests/PipelineInventoryChecks.java
java -cp "$cp" UnpickFastChecks
java -cp "$cp" ManualPickInventoryRegression
java -cp "$cp" ManualPickFastInventoryChecks
java -cp "$cp" ManualPickLpnGuardChecks
java -cp "$cp" PipelineInventoryChecks
