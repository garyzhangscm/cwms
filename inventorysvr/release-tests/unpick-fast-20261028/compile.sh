set -eu
cp='/work/classes:/pipeline/classes/inventory:/reserved/classes/inventory:/split/inventory-classes:/inv/BOOT-INF/classes:/inv/BOOT-INF/lib/*'
javac --release 17 -parameters -sourcepath '' -cp "$cp" -d /work/classes /work/src/*.java
javac --add-exports java.base/jdk.internal.org.objectweb.asm=ALL-UNNAMED --add-exports java.base/jdk.internal.org.objectweb.asm.tree=ALL-UNNAMED -d /work/tools /work/tools/PatchUnpick.java
java --add-exports java.base/jdk.internal.org.objectweb.asm=ALL-UNNAMED --add-exports java.base/jdk.internal.org.objectweb.asm.tree=ALL-UNNAMED -cp "/work/tools:$cp" PatchUnpick /work/baseline /work/classes
