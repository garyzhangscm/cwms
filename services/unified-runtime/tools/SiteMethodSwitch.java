import java.nio.file.*;
import java.util.*;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
/** Preserve both verified deployed implementations and select with an external capability flag. */
public class SiteMethodSwitch {
 static ClassNode read(Path p)throws Exception {ClassNode n=new ClassNode();new ClassReader(Files.readAllBytes(p)).accept(n,0);return n;}
 static String key(MethodNode m){return m.name+m.desc;}
 public static void main(String[] a)throws Exception {
  ClassNode current=read(Path.of(a[0])),legacy=read(Path.of(a[1]));String flag=a[2];Set<String> names=new HashSet<>(Arrays.asList(a[4].split(",")));int changed=0;Set<String> seen=new HashSet<>();
  Set<String> present=new HashSet<>();for(MethodNode m:current.methods)present.add(key(m));
  // Legacy label helpers have distinct descriptors; retain their original bytecode.
  if(a.length>5)for(MethodNode m:legacy.methods)if(Arrays.asList(a[5].split(",")).contains(m.name)&&!present.contains(key(m))){current.methods.add(m);present.add(key(m));}
  for(MethodNode original:new ArrayList<>(current.methods)){
   if(!names.contains(original.name))continue;seen.add(original.name);
   MethodNode old=legacy.methods.stream().filter(m->key(m).equals(key(original))).findFirst().orElseThrow(()->new IllegalStateException("Missing legacy method "+key(original)));
   String oldName="siteLegacy$"+old.name;if(present.contains(oldName+old.desc))throw new IllegalStateException("Already patched");
   old.name=oldName;old.access=(old.access&~(Opcodes.ACC_PUBLIC|Opcodes.ACC_PROTECTED))|Opcodes.ACC_PRIVATE;old.visibleAnnotations=null;old.invisibleAnnotations=null;current.methods.add(old);present.add(key(old));
   InsnList head=new InsnList();LabelNode modern=new LabelNode();
   head.add(new LdcInsnNode(flag));head.add(new LdcInsnNode(flag));head.add(new MethodInsnNode(Opcodes.INVOKESTATIC,"java/lang/System","getenv","(Ljava/lang/String;)Ljava/lang/String;",false));head.add(new LdcInsnNode("true"));head.add(new MethodInsnNode(Opcodes.INVOKESTATIC,"java/util/Objects","toString","(Ljava/lang/Object;Ljava/lang/String;)Ljava/lang/String;",false));head.add(new MethodInsnNode(Opcodes.INVOKESTATIC,"java/lang/System","getProperty","(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;",false));head.add(new MethodInsnNode(Opcodes.INVOKESTATIC,"java/lang/Boolean","parseBoolean","(Ljava/lang/String;)Z",false));head.add(new JumpInsnNode(Opcodes.IFNE,modern));
   boolean stat=(original.access&Opcodes.ACC_STATIC)!=0;int slot=stat?0:1;if(!stat)head.add(new VarInsnNode(Opcodes.ALOAD,0));
   for(Type t:Type.getArgumentTypes(original.desc)){head.add(new VarInsnNode(t.getOpcode(Opcodes.ILOAD),slot));slot+=t.getSize();}
   head.add(new MethodInsnNode(stat?Opcodes.INVOKESTATIC:Opcodes.INVOKESPECIAL,current.name,oldName,old.desc,false));head.add(new InsnNode(Type.getReturnType(old.desc).getOpcode(Opcodes.IRETURN)));head.add(modern);head.add(new FrameNode(Opcodes.F_SAME,0,null,0,null));original.instructions.insert(head);changed++;
  }
  if(!seen.equals(names))throw new IllegalStateException("Expected "+names.size()+" named methods, found "+changed);
  ClassWriter w=new ClassWriter(ClassWriter.COMPUTE_MAXS);current.accept(w);Path out=Path.of(a[3]);Files.createDirectories(out.getParent());Files.write(out,w.toByteArray());System.out.println(current.name+": "+changed+" config-selected methods, flag="+flag);
 }
}
