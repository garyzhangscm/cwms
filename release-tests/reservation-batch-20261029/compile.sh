set -eu
mkdir -p /fix/classes/outbound /fix/classes/inventory
javac --release 17 -parameters -sourcepath '' -cp '/fix/outbound-baseline/BOOT-INF/classes:/fix/outbound-baseline/BOOT-INF/lib/*' -d /fix/classes/outbound /code/outboundsvr/src/main/java/com/garyzhangscm/cwms/outbound/quantity/QuantityAllocationService.java
javac --release 17 -parameters -sourcepath '' -cp '/fix/inventory-baseline/BOOT-INF/classes:/fix/inventory-baseline/BOOT-INF/lib/*' -d /fix/classes/inventory /code/inventorysvr/src/main/java/com/garyzhangscm/cwms/inventory/controller/QuantityCapacityController.java
