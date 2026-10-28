set -eu
cp='/work/classes:/queue/classes:/pipeline/classes/outbound:/reserved/classes/outbound:/unpick/classes:/split/outbound-classes:/out/BOOT-INF/classes:/out/BOOT-INF/lib/*:/quantity/h2.jar'
javac --release 17 -parameters -sourcepath '' -cp "$cp" -d /work/classes /work/src/*.java /work/tests/*.java
java -cp "$cp" LpnCandidateReuseChecks
java -cp "$cp" QuantityAllocationChecks /quantity/schema.sql
java -cp "$cp" ManualPickQueueChecks /queue/schema.sql
java -cp "$cp" PipelineWorkflowChecks
java -cp "$cp" UnpickSyncChecks /quantity/schema.sql
