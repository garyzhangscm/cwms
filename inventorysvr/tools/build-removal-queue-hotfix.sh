#!/bin/sh
set -eu
cd /work
CP='baseline/BOOT-INF/classes:baseline/BOOT-INF/lib/*'
mkdir -p patched tests
javac --release 13 -cp "$CP" -d patched DurableRemovalQueue.java InventoryRemovalQueue.java
javac --release 13 -cp "$CP" -d tests PatchRemovalQueue.java
java -cp "tests:$CP" PatchRemovalQueue baseline/BOOT-INF/classes patched
javac --release 13 -cp "patched:$CP" -d tests RemovalQueueRegression.java
java -Xverify:all -cp "tests:patched:$CP" RemovalQueueRegression
mkdir -p replacement/BOOT-INF/classes
cp -R patched/com replacement/BOOT-INF/classes/
cp original.jar inventoryserver-v1.62.jar
jar uf inventoryserver-v1.62.jar -C replacement BOOT-INF/classes
