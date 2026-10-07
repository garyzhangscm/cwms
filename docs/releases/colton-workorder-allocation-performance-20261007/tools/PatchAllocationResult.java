import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.commons.*;
import java.nio.file.*;
import java.util.*;
import java.util.jar.*;
public class PatchAllocationResult {
 public static void main(String[] args)throws Exception {
  String owner="com/garyzhangscm/cwms/workorder/service/WorkOrderService";
  ClassNode baseline=new ClassNode(), compiled=new ClassNode();
  try(JarFile jar=new JarFile(args[0])) {new ClassReader(jar.getInputStream(jar.getJarEntry("BOOT-INF/classes/"+owner+".class"))).accept(baseline,0);}
  new ClassReader(Files.readAllBytes(Path.of(args[1],owner+".class"))).accept(compiled,0);
  Set<String> roots=Set.of("getInprocessQuantities","isPickSparePart","getAllocationLine","processAllocateResult","processAllocationResultForSpareParts");
  Map<String,MethodNode> selected=new LinkedHashMap<>();
  for(MethodNode m:compiled.methods)if(roots.contains(m.name))selected.put(m.name+m.desc,m);
  boolean added;
  do {added=false; for(MethodNode m:new ArrayList<>(selected.values()))for(AbstractInsnNode ins:m.instructions){
   List<Handle> handles=new ArrayList<>();
   if(ins instanceof InvokeDynamicInsnNode indy)for(Object arg:indy.bsmArgs)if(arg instanceof Handle h)handles.add(h);
   for(Handle h:handles)if(h.getOwner().equals(owner)&&h.getName().startsWith("lambda$"))for(MethodNode candidate:compiled.methods)if(candidate.name.equals(h.getName())&&candidate.desc.equals(h.getDesc())&&!selected.containsKey(candidate.name+candidate.desc)){selected.put(candidate.name+candidate.desc,candidate);added=true;}
  }}while(added);
  Remapper remap=new Remapper(){@Override public String mapMethodName(String o,String n,String d){return o.equals(owner)&&selected.containsKey(n+d)&&n.startsWith("lambda$")?"codexAllocationResult$"+n:n;}};
  for(MethodNode m:selected.values()) {
   String name=remap.mapMethodName(owner,m.name,m.desc);
   MethodNode copy=new MethodNode(m.access,name,m.desc,m.signature,m.exceptions.toArray(String[]::new));
   m.accept(new MethodRemapper(copy,remap));
   baseline.methods.removeIf(existing->existing.name.equals(name)&&existing.desc.equals(m.desc));baseline.methods.add(copy);
  }
  ClassWriter writer=new ClassWriter(0);baseline.accept(writer);Files.write(Path.of(args[2]),writer.toByteArray());
  System.out.println("Patched allocation-result methods and isolated lambda dependencies only: "+selected.size());
 }
}
