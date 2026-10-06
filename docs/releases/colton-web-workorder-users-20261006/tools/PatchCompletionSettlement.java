import java.nio.file.*;
import org.springframework.asm.*;
public class PatchCompletionSettlement {
    static int changed;
    public static void main(String[] args) throws Exception {
        byte[] baseline=Files.readAllBytes(Path.of(args[0]));
        ClassReader reader=new ClassReader(baseline);
        ClassWriter writer=new ClassWriter(reader,ClassWriter.COMPUTE_MAXS);
        reader.accept(new ClassVisitor(Opcodes.ASM9,writer){
            @Override public MethodVisitor visitMethod(int access,String name,String desc,String signature,String[] exceptions){
                MethodVisitor next=super.visitMethod(access,name,desc,signature,exceptions);
                if(!name.equals("completeWorkOrderLine")) return next;
                return new MethodVisitor(Opcodes.ASM9,next){
                    @Override public void visitMethodInsn(int opcode,String owner,String method,String descriptor,boolean isInterface){
                        if(method.equals("saveOrUpdate") && descriptor.equals("(Lcom/garyzhangscm/cwms/workorder/model/WorkOrderLine;)Lcom/garyzhangscm/cwms/workorder/model/WorkOrderLine;")){
                            super.visitInsn(Opcodes.ICONST_0);
                            descriptor="(Lcom/garyzhangscm/cwms/workorder/model/WorkOrderLine;Z)Lcom/garyzhangscm/cwms/workorder/model/WorkOrderLine;";
                            changed++;
                        }
                        super.visitMethodInsn(opcode,owner,method,descriptor,isInterface);
                    }
                };
            }
        },0);
        if(changed!=1)throw new IllegalStateException("Expected one settlement save call, found "+changed);
        Files.write(Path.of(args[1]),writer.toByteArray());
        System.out.println("Patched one completion settlement save to skip display-only detail loading.");
    }
}
