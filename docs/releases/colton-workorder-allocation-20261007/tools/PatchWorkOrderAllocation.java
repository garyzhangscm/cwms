import java.nio.file.*;
import java.util.jar.*;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
public class PatchWorkOrderAllocation {
 public static void main(String[] args)throws Exception {
  String prefix="com/garyzhangscm/cwms/workorder/",name=prefix+"clients/OutboundServiceRestemplateClient";
  ClassNode baseline=new ClassNode(),source=new ClassNode();
  try(JarFile jar=new JarFile(args[0])) {new ClassReader(jar.getInputStream(jar.getJarEntry("BOOT-INF/classes/"+name+".class")).readAllBytes()).accept(baseline,0);}
  new ClassReader(Files.readAllBytes(Path.of(args[1],name+".class"))).accept(source,0);
  if(baseline.fields.stream().anyMatch(f->f.name.equals("allocationDataService")))throw new IllegalStateException("Already patched");
  baseline.fields.add(source.fields.stream().filter(f->f.name.equals("allocationDataService")).findFirst().orElseThrow());
  int changed=0;
  for(MethodNode m:baseline.methods)if(m.name.equals("allocateWorkOrder")&&m.desc.equals("(L"+prefix+"model/WorkOrder;Ljava/lang/Long;Ljava/lang/Long;)L"+prefix+"model/AllocationResult;")){
   InsnList prep=new InsnList();prep.add(new VarInsnNode(Opcodes.ALOAD,0));
   prep.add(new FieldInsnNode(Opcodes.GETFIELD,name,"allocationDataService","L"+prefix+"service/WorkOrderAllocationDataService;"));
   prep.add(new VarInsnNode(Opcodes.ALOAD,1));
   prep.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL,prefix+"service/WorkOrderAllocationDataService","prepare","(L"+prefix+"model/WorkOrder;)V",false));
   m.instructions.insert(prep);m.maxStack=Math.max(m.maxStack,2);changed++;
  }
  if(changed!=1)throw new IllegalStateException("Expected one whole/production-line allocation entrypoint, found "+changed);
  ClassWriter writer=new ClassWriter(0);baseline.accept(writer);Files.write(Path.of(args[2]),writer.toByteArray());
  System.out.println("Patched one existing allocation entrypoint; other client methods retained.");
 }
}
