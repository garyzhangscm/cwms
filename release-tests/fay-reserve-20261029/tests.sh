set -eu
out='/fix/classes/outbound:/fix/outbound-baseline/BOOT-INF/classes:/fix/outbound-baseline/BOOT-INF/lib/*:/fix/h2.jar'
inv='/fix/classes/inventory:/fix/inventory-baseline/BOOT-INF/classes:/fix/inventory-baseline/BOOT-INF/lib/*:/fix/h2.jar'
javac --release 17 -parameters -cp "$out" -d /fix/classes/outbound /fix/tests/QuantityAllocationChecks.java /fix/tests/JpaJdbcAtomicChecks.java /fix/tests/TransactionProbe.java
java -cp "$out" QuantityAllocationChecks /fix/schema.sql
java -cp "$out" JpaJdbcAtomicChecks
javac --release 17 -parameters -cp "$inv" -d /fix/classes/inventory /fix/tests/InventoryCapacityChecks.java /fix/tests/ScopedCapacityChecks.java
java -cp "$inv" InventoryCapacityChecks
java -cp "$inv" ScopedCapacityChecks
