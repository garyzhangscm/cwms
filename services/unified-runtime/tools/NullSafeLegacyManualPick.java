import java.nio.file.*;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
/** Preserve the FAY report workflow, replacing only its legacy row formatter with the shared null-safe formatter. */
public class NullSafeLegacyManualPick {
 public static void main(String[] args)throws Exception {
  byte[] original=Files.readAllBytes(Path.of(args[0]));ClassReader reader=new ClassReader(original);ClassNode owner=new ClassNode();reader.accept(owner,0);
  String target="siteLegacy$setupManualPickableInventoryForDisplay";int changed=0;
  for(MethodNode m:owner.methods)if(m.name.equals(target)&&m.desc.equals("(Ljava/util/List;)V")){
   m.instructions.clear();m.tryCatchBlocks.clear();m.localVariables=null;m.visibleLocalVariableAnnotations=null;m.invisibleLocalVariableAnnotations=null;
   m.instructions.add(new VarInsnNode(Opcodes.ALOAD,0));m.instructions.add(new VarInsnNode(Opcodes.ALOAD,1));
   m.instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC,"com/garyzhangscm/cwms/outbound/reportfix/ManualPickReportRows","build","(Ljava/util/List;)Ljava/util/List;",false));
   m.instructions.add(new FieldInsnNode(Opcodes.PUTFIELD,owner.name,"manualPickableInventoryForDisplay","Ljava/util/List;"));m.instructions.add(new InsnNode(Opcodes.RETURN));m.maxStack=2;m.maxLocals=2;changed++;
  }
  if(changed!=1)throw new IllegalStateException("Expected exactly one legacy formatter");
  ClassWriter w=new ClassWriter(reader,0);owner.accept(w);Path output=Path.of(args[1]);Files.createDirectories(output.getParent());Files.write(output,w.toByteArray());
  System.out.println("Replaced only the legacy report row formatter; site switches and inventory selection retained");
 }
}
