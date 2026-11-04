import java.nio.file.*;
import org.springframework.asm.*;
public class ReplaceMethod implements Opcodes {
  public static void main(String[] args) throws Exception {
    String pkg="com/garyzhangscm/cwms/workorder/clients/", proxy="L"+pkg+"RestTemplateProxy;";
    Path input=Path.of(args[0]); ClassReader r=new ClassReader(Files.readAllBytes(input)); ClassWriter w=new ClassWriter(r,0);
    int[] matched={0};
    r.accept(new ClassVisitor(ASM9,w) {
      public MethodVisitor visitMethod(int access,String name,String desc,String sig,String[] ex) {
        MethodVisitor m=super.visitMethod(access,name,desc,sig,ex);
        if (!name.equals("findDeliveredInventory") || !desc.equals("(Ljava/lang/Long;Ljava/lang/Long;Ljava/lang/String;)Ljava/util/List;")) return m;
        matched[0]++; m.visitCode(); m.visitVarInsn(ALOAD,0); m.visitFieldInsn(GETFIELD,r.getClassName(),"restTemplateProxy",proxy);
        for(int i=1;i<=3;i++) m.visitVarInsn(ALOAD,i);
        m.visitMethodInsn(INVOKESTATIC,pkg+"DeliveredInventoryBatches","load","("+proxy+desc.substring(1),false);
        m.visitInsn(ARETURN); m.visitMaxs(4,4); m.visitEnd(); return null;
      }
    },0);
    if(matched[0]!=1) throw new IllegalStateException("Production signature mismatch");
    Files.write(Path.of(args[1]),w.toByteArray());
  }
}
