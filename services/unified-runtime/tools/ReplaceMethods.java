import java.nio.file.*;import java.util.*;import org.objectweb.asm.*;import org.objectweb.asm.tree.*;
/** Merge only selected freshly compiled methods, retaining every other deployed method and field. */
public class ReplaceMethods {
 static ClassNode read(Path p)throws Exception {ClassNode n=new ClassNode();new ClassReader(Files.readAllBytes(p)).accept(n,0);return n;}
 static void checkLambda(ClassNode owner,Handle handle) {
  if(handle.getOwner().equals(owner.name)&&handle.getName().startsWith("lambda$")&&owner.methods.stream().noneMatch(m->m.name.equals(handle.getName())&&m.desc.equals(handle.getDesc())))
   throw new IllegalStateException("Replacement references a missing lambda implementation: "+handle.getName()+handle.getDesc());
 }
 public static void main(String[] a)throws Exception {
  ClassNode b=read(Path.of(a[0])),fresh=read(Path.of(a[1]));Set<String> names=new HashSet<>(Arrays.asList(a[3].split(",")));int count=0;
  for(int i=0;i<b.methods.size();i++){MethodNode old=b.methods.get(i);if(!names.contains(old.name))continue;MethodNode replacement=fresh.methods.stream().filter(m->m.name.equals(old.name)&&m.desc.equals(old.desc)).findFirst().orElseThrow();b.methods.set(i,replacement);count++;}
  if(count!=names.size())throw new IllegalStateException("Expected methods missing: "+count);
  for(MethodNode method:b.methods)if(names.contains(method.name))for(AbstractInsnNode instruction:method.instructions) {
   if(instruction instanceof InvokeDynamicInsnNode) {
    InvokeDynamicInsnNode dynamic=(InvokeDynamicInsnNode)instruction;
    checkLambda(b,dynamic.bsm);
    for(Object argument:dynamic.bsmArgs)if(argument instanceof Handle)checkLambda(b,(Handle)argument);
   }
  }
  ClassWriter w=new ClassWriter(0);b.accept(w);Path out=Path.of(a[2]);Files.createDirectories(out.getParent());Files.write(out,w.toByteArray());System.out.println(b.name+": replaced "+count+" selected methods");
 }
}
