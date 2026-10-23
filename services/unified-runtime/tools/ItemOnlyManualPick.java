import java.nio.file.*;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

/** Replace report preparation only; never touch manual picking or allocation. */
public class ItemOnlyManualPick {
 public static void main(String[] args)throws Exception {
  ClassReader reader=new ClassReader(Files.readAllBytes(Path.of(args[0])));
  ClassNode owner=new ClassNode();reader.accept(owner,0);int data=0;
  String base="com/garyzhangscm/cwms/outbound/model/";
  for(MethodNode m:owner.methods){
   boolean rows=m.name.equals("setupOrderManualPickReportData")||m.name.equals("siteLegacy$setupOrderManualPickReportData");
   if(!rows)continue;
   if(!m.desc.equals("(L"+base+"Report;L"+base+"Order;)V"))throw new IllegalStateException("Unexpected report method signature");
   m.instructions.clear();m.tryCatchBlocks.clear();m.localVariables=null;
   m.visibleLocalVariableAnnotations=null;m.invisibleLocalVariableAnnotations=null;
   m.instructions.add(new VarInsnNode(Opcodes.ALOAD,1));
   m.instructions.add(new VarInsnNode(Opcodes.ALOAD,2));
   m.instructions.add(new VarInsnNode(Opcodes.ALOAD,0));
   m.instructions.add(new FieldInsnNode(Opcodes.GETFIELD,owner.name,"pickService","Lcom/garyzhangscm/cwms/outbound/service/PickService;"));
   m.instructions.add(new VarInsnNode(Opcodes.ALOAD,2));
   m.instructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL,"com/garyzhangscm/cwms/outbound/service/PickService","findByOrder","(L"+base+"Order;)Ljava/util/List;",false));
   m.instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC,"com/garyzhangscm/cwms/outbound/reportfix/ManualPickDemandRows","build","(L"+base+"Order;Ljava/util/List;)Ljava/util/List;",false));
   m.instructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL,base+"Report","setData","(Ljava/util/Collection;)V",false));
   m.instructions.add(new InsnNode(Opcodes.RETURN));m.maxStack=4;m.maxLocals=3;
   data++;
  }
  if(data<1||data>2)throw new IllegalStateException("Report preparation methods not found exactly");
  int scoped=0;
  for(MethodNode m:owner.methods)if(m.name.equals("setupWarehouseOrderPackingListData")||m.name.equals("setupWarehouseOrderBillOfLadingData")){
   for(AbstractInsnNode ins:m.instructions.toArray())if(ins instanceof MethodInsnNode){MethodInsnNode call=(MethodInsnNode)ins;
    if(call.owner.equals("com/garyzhangscm/cwms/outbound/service/PickService")&&call.name.equals("findByOrder")&&call.desc.equals("(L"+base+"Order;)Ljava/util/List;")){
     m.instructions.set(call,new MethodInsnNode(Opcodes.INVOKESTATIC,"com/garyzhangscm/cwms/outbound/reportfix/ShippingReportScope","picks","(Lcom/garyzhangscm/cwms/outbound/service/PickService;L"+base+"Order;)Ljava/util/List;",false));scoped++;
    }
   }
  }
  if(scoped!=4)throw new IllegalStateException("Unexpected shipping report pick lookups: "+scoped);
  ClassWriter writer=new ClassWriter(reader,0);owner.accept(writer);
  Path target=Path.of(args[1]);Files.createDirectories(target.getParent());Files.write(target,writer.toByteArray());
  System.out.println("Item-only report: "+data+" data method(s); drop-off parameters and picking unchanged");
 }
}
