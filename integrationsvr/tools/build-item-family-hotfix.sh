#!/bin/sh
# Run in the original integration image with sources mounted at /work.
# No server process is started. Output is a one-class JAR patch plus test logs.
set -eu
cd /work
test ! -e original.jar
cp /usr/local/service/integrationserver-v1.62.jar original.jar
mkdir baseline patched tests
cd baseline
jar xf ../original.jar
cd /work
CP='baseline/BOOT-INF/classes:baseline/BOOT-INF/lib/*'
javac --release 13 -cp "$CP" -d tests ItemFamilyCompanyRegression.java
if java -cp "tests:$CP" com.garyzhangscm.cwms.integration.model.ItemFamilyCompanyRegression > baseline-test.log 2>&1; then
    echo 'Unexpected baseline pass: stop and review deployed version'
    exit 1
fi
grep -q 'nested family lost company code' baseline-test.log
javac --release 13 -cp "$CP" -d patched DBBasedItemFamily.java
java -cp "tests:patched:$CP" com.garyzhangscm.cwms.integration.model.ItemFamilyCompanyRegression > patched-test.log 2>&1
cat patched-test.log
CLASS='com/garyzhangscm/cwms/integration/model/DBBasedItemFamily.class'
javap -private "baseline/BOOT-INF/classes/$CLASS" > original-api.txt
javap -private "patched/$CLASS" > patched-api.txt
diff -u original-api.txt patched-api.txt
mkdir -p replacement/BOOT-INF/classes
cp -R patched/com replacement/BOOT-INF/classes/
cp original.jar integrationserver-v1.62.jar
jar uf integrationserver-v1.62.jar -C replacement "BOOT-INF/classes/$CLASS"
