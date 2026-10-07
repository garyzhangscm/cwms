import java.nio.file.*;
import java.util.jar.*;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
public class PatchWorkOrderAllocationTransport {
 static ClassNode read(byte[] data){ClassNode node=new ClassNode();new ClassReader(data).accept(node,0);return node;}
 static void write(ClassNode node,Path out)throws Exception{ClassWriter writer=new ClassWriter(0);node.accept(writer);Files.write(out,writer.toByteArray());}
 public static void main(String[] args)throws Exception{
  String p="com/garyzhangscm/cwms/workorder/",client=p+"clients/OutboundServiceRestemplateClient",item=p+"model/Item";
  try(JarFile jar=new JarFile(args[0])){
   ClassNode c=read(jar.getInputStream(jar.getJarEntry("BOOT-INF/classes/"+client+".class")).readAllBytes());int count=0;
   for(MethodNode m:c.methods)if(m.name.equals("allocateWorkOrder")&&m.desc.equals("(L"+p+"model/WorkOrder;Ljava/lang/Long;Ljava/lang/Long;)L"+p+"model/AllocationResult;")){
    for(AbstractInsnNode n=m.instructions.getFirst();n!=null;n=n.getNext())if(n instanceof MethodInsnNode call&&call.owner.equals(p+"clients/RestTemplateProxy")&&call.name.equals("exchange")){
     InsnList list=new InsnList();list.add(new VarInsnNode(Opcodes.ALOAD,0));
     list.add(new FieldInsnNode(Opcodes.GETFIELD,client,"allocationDataService","L"+p+"service/WorkOrderAllocationDataService;"));
     list.add(new InsnNode(Opcodes.SWAP));
     list.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL,p+"service/WorkOrderAllocationDataService","requestBody","(L"+p+"model/WorkOrder;)Ljava/util/Map;",false));
     m.instructions.insertBefore(n,list);m.maxStack+=2;count++;
    }
   }
   if(count!=1)throw new IllegalStateException("Expected one allocation POST call, found "+count);write(c,Path.of(args[2],"OutboundServiceRestemplateClient.class"));
   ClassNode original=read(jar.getInputStream(jar.getJarEntry("BOOT-INF/classes/"+item+".class")).readAllBytes());
   ClassNode source=read(Files.readAllBytes(Path.of(args[1],item+".class")));
   if(original.fields.stream().anyMatch(f->f.name.equals("warehouseId")))throw new IllegalStateException("Already patched Item");
   original.fields.add(source.fields.stream().filter(f->f.name.equals("warehouseId")).findFirst().orElseThrow());
   source.methods.stream().filter(m->m.name.equals("getWarehouseId")||m.name.equals("setWarehouseId")).forEach(original.methods::add);
   write(original,Path.of(args[2],"Item.class"));
   String inventory=p+"clients/InventoryServiceRestemplateClient";
   ClassNode oldInventory=read(jar.getInputStream(jar.getJarEntry("BOOT-INF/classes/"+inventory+".class")).readAllBytes());
   ClassNode newInventory=read(Files.readAllBytes(Path.of(args[1],inventory+".class")));
   if(oldInventory.methods.stream().anyMatch(m->m.name.equals("getItemForAllocation")))throw new IllegalStateException("Already patched Inventory client");
   oldInventory.methods.add(newInventory.methods.stream().filter(m->m.name.equals("getItemForAllocation")).findFirst().orElseThrow());
   write(oldInventory,Path.of(args[2],"InventoryServiceRestemplateClient.class"));
  }
  System.out.println("Patched allocation transport and added Item warehouseId only; original methods preserved.");
 }
}
