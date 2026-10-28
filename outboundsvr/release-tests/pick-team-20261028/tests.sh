set -eu
cp='/work/classes:/work/baseline/BOOT-INF/classes:/work/baseline/BOOT-INF/lib/*:/quantity/h2.jar'
javac --release 17 -parameters -sourcepath '' -cp "$cp" -d /work/classes /work/src/*.java /work/tests/*.java
java -cp "$cp" ManualPickQueueChecks /queue/schema.sql
