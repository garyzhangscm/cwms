import org.springframework.asm.*;
import java.nio.file.*;
public class PatchRemovalQueue {
    public static void main(String[] args) throws Exception {
        String root = "com/garyzhangscm/cwms/inventory/service/";
        for (String name : new String[]{"InventoryService", "UserService"}) {
            Path source = Paths.get(args[0], root + name + ".class");
            ClassReader reader = new ClassReader(Files.readAllBytes(source));
            ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_MAXS);
            final int[] patched = {0};
            reader.accept(new ClassVisitor(Opcodes.ASM7, writer) {
                @Override public MethodVisitor visitMethod(int access, String method, String descriptor, String signature, String[] exceptions) {
                    MethodVisitor target = super.visitMethod(access, method, descriptor, signature, exceptions);
                    if (name.equals("InventoryService") && method.equals("removeInventores") && descriptor.equals("(Ljava/lang/Long;Ljava/lang/String;Ljava/lang/Boolean;)Ljava/lang/String;")) {
                        patched[0]++;
                        target.visitCode();
                        target.visitMethodInsn(Opcodes.INVOKESTATIC, root + "InventoryRemovalQueue", "getInstance", "()L"+root+"InventoryRemovalQueue;", false);
                        target.visitVarInsn(Opcodes.ALOAD, 1); target.visitVarInsn(Opcodes.ALOAD, 2); target.visitVarInsn(Opcodes.ALOAD, 3);
                        target.visitMethodInsn(Opcodes.INVOKEVIRTUAL, root + "InventoryRemovalQueue", "submit", descriptor, false);
                        target.visitInsn(Opcodes.ARETURN); target.visitMaxs(0,0); target.visitEnd();
                        return null;
                    }
                    if (name.equals("UserService") && method.equals("getCurrentUserName") && descriptor.equals("()Ljava/lang/String;")) {
                        patched[0]++;
                        return new MethodVisitor(Opcodes.ASM7, target) {
                            @Override public void visitCode() {
                                super.visitCode();
                                super.visitMethodInsn(Opcodes.INVOKESTATIC, root+"InventoryRemovalQueue", "currentActor", "()Ljava/lang/String;", false);
                                super.visitInsn(Opcodes.DUP);
                                Label original = new Label();
                                super.visitJumpInsn(Opcodes.IFNULL, original);
                                super.visitInsn(Opcodes.ARETURN);
                                super.visitLabel(original);
                                super.visitFrame(Opcodes.F_SAME1, 0, null, 1, new Object[]{"java/lang/String"});
                                super.visitInsn(Opcodes.POP);
                            }
                        };
                    }
                    return target;
                }
            }, 0);
            if (patched[0] != 1) throw new IllegalStateException("Unexpected deployed API: " + name);
            Path output = Paths.get(args[1], root + name + ".class");
            Files.createDirectories(output.getParent()); Files.write(output, writer.toByteArray());
        }
    }
}
