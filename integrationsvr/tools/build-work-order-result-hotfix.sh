#!/bin/sh
# Run inside the currently deployed integration image, mounting sources at /work.
# This preserves the Item fixes already present in that image.
set -eu
cd /work
test ! -e original.jar
cp /usr/local/service/integrationserver-v1.62.jar original.jar
mkdir baseline patched tests
cd baseline
jar xf ../original.jar
cd /work
CP='baseline/BOOT-INF/classes:baseline/BOOT-INF/lib/*'
javac --release 13 -cp "$CP" -d tests WorkOrderResultOrderingRegression.java
if java -cp "tests:$CP" com.garyzhangscm.cwms.integration.service.WorkOrderResultOrderingRegression > baseline-test.log 2>&1; then
    echo 'Unexpected baseline pass: stop and review deployed version'
    exit 1
fi
grep -q 'published before SENT was saved\|late SENT overwrote completion' baseline-test.log
javac --release 13 -cp "$CP" -d patched DBBasedWorkOrderIntegration.java
java -cp "tests:patched:$CP" com.garyzhangscm.cwms.integration.service.WorkOrderResultOrderingRegression > patched-test.log 2>&1
tail -1 patched-test.log
CLASS='com/garyzhangscm/cwms/integration/service/DBBasedWorkOrderIntegration.class'
javap -public "baseline/BOOT-INF/classes/$CLASS" > original-public-api.txt
javap -public "patched/$CLASS" > patched-public-api.txt
diff -u original-public-api.txt patched-public-api.txt
mkdir -p replacement/BOOT-INF/classes
cp -R patched/com replacement/BOOT-INF/classes/
cp original.jar integrationserver-v1.62.jar
jar uf integrationserver-v1.62.jar -C replacement BOOT-INF/classes
