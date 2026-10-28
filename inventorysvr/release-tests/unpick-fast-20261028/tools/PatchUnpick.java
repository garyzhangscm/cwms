import java.nio.file.*;import jdk.internal.org.objectweb.asm.*;import jdk.internal.org.objectweb.asm.tree.*;
public class PatchUnpick implements Opcodes {
 public static void main(String[] args)throws Exception{
  String path="com/garyzhangscm/cwms/inventory/service/InventoryService.class";byte[] before=Files.readAllBytes(Path.of(args[0],path));ClassNode c=new ClassNode();new ClassReader(before).accept(c,ClassReader.EXPAND_FRAMES);int changed=0;
  for(MethodNode m:c.methods)if(m.name.equals("unpick")&&m.desc.equals("(Lcom/garyzhangscm/cwms/inventory/model/Inventory;Lcom/garyzhangscm/cwms/inventory/model/Location;Z)Lcom/garyzhangscm/cwms/inventory/model/Inventory;")){
   for(AbstractInsnNode n:m.instructions.toArray())if(n instanceof MethodInsnNode){MethodInsnNode call=(MethodInsnNode)n;
    if(call.owner.equals("com/garyzhangscm/cwms/inventory/service/InventoryService")&&call.name.equals("moveInventory")&&call.desc.equals("(Lcom/garyzhangscm/cwms/inventory/model/Inventory;Lcom/garyzhangscm/cwms/inventory/model/Location;)Lcom/garyzhangscm/cwms/inventory/model/Inventory;")){
     AbstractInsnNode next=n.getNext();while(next.getOpcode()<0)next=next.getNext();if(!(next instanceof VarInsnNode)||next.getOpcode()!=ASTORE||((VarInsnNode)next).var!=1)throw new IllegalStateException("Unexpected immediate move result handling");
     m.instructions.set(next,new InsnNode(ARETURN));changed++;
    }
   }
  }
  if(changed!=1)throw new IllegalStateException("Expected exactly one immediate Unpick save path, found "+changed);
  ClassWriter w=new ClassWriter(ClassWriter.COMPUTE_FRAMES|ClassWriter.COMPUTE_MAXS){@Override protected ClassLoader getClassLoader(){return PatchUnpick.class.getClassLoader();}};c.accept(w);Path out=Path.of(args[1],path);Files.createDirectories(out.getParent());Files.write(out,w.toByteArray());System.out.println("Patched only immediate Unpick return; preserved deferred movement, outbound synchronization and audit calls");
 }
}
