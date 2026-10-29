set -eu
javac --release 17 -parameters -sourcepath '' -cp '/fix/outbound-baseline/BOOT-INF/classes:/fix/outbound-baseline/BOOT-INF/lib/*' -d /fix/rejection-classes /code/outboundsvr/src/main/java/com/garyzhangscm/cwms/outbound/service/ManualPickRejection.java /code/outboundsvr/src/main/java/com/garyzhangscm/cwms/outbound/service/PreprintedManualPickService.java
