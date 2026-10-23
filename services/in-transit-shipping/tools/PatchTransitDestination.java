import org.springframework.asm.*;
import java.nio.file.*;
import java.util.*;

/** Changes one invocation in the live ShipmentService; every other existing method is preserved. */
public class PatchTransitDestination {
    private static final String SERVICE="com/garyzhangscm/cwms/outbound/service/ShipmentService";
    private static final String TRANSIT="com/garyzhangscm/cwms/outbound/transit/TransitShippingService";
    public static void main(String[] args) throws Exception {
        byte[] original=Files.readAllBytes(Paths.get(args[0]));
        ClassReader reader=new ClassReader(original);
        ClassWriter writer=new ClassWriter(reader,ClassWriter.COMPUTE_MAXS);
        final int[] calls={0};
        reader.accept(new ClassVisitor(Opcodes.ASM9,writer) {
            @Override public FieldVisitor visitField(int access,String name,String descriptor,String signature,Object value) {
                if(name.equals("transitShippingService")) throw new IllegalStateException("Baseline already patched");
                return super.visitField(access,name,descriptor,signature,value);
            }
            @Override public MethodVisitor visitMethod(int access,String name,String descriptor,String signature,String[] exceptions) {
                MethodVisitor next=super.visitMethod(access,name,descriptor,signature,exceptions);
                if(!name.equals("completeShipmentByOrder") || !descriptor.equals("(Lcom/garyzhangscm/cwms/outbound/model/Shipment;Lcom/garyzhangscm/cwms/outbound/model/Order;)V")) return next;
                return new MethodVisitor(Opcodes.ASM9,next) {
                    @Override public void visitMethodInsn(int opcode,String owner,String name,String desc,boolean itf) {
                        if(opcode==Opcodes.INVOKEVIRTUAL && owner.equals("com/garyzhangscm/cwms/outbound/clients/WarehouseLayoutServiceRestemplateClient") && name.equals("createOrderLocation") && desc.equals("(Ljava/lang/Long;Lcom/garyzhangscm/cwms/outbound/model/Order;)Lcom/garyzhangscm/cwms/outbound/model/Location;")) {
                            calls[0]++;
                            super.visitVarInsn(Opcodes.ALOAD,0);
                            super.visitFieldInsn(Opcodes.GETFIELD,SERVICE,"transitShippingService","L"+TRANSIT+";");
                            super.visitMethodInsn(Opcodes.INVOKESTATIC,TRANSIT,"destination","(Lcom/garyzhangscm/cwms/outbound/clients/WarehouseLayoutServiceRestemplateClient;Ljava/lang/Long;Lcom/garyzhangscm/cwms/outbound/model/Order;L"+TRANSIT+";)Lcom/garyzhangscm/cwms/outbound/model/Location;",false);
                        } else super.visitMethodInsn(opcode,owner,name,desc,itf);
                    }
                };
            }
            @Override public void visitEnd() {
                FieldVisitor field=super.visitField(Opcodes.ACC_PRIVATE,"transitShippingService","L"+TRANSIT+";",null,null);
                AnnotationVisitor annotation=field.visitAnnotation("Lorg/springframework/beans/factory/annotation/Autowired;",true);
                annotation.visit("required",false);annotation.visitEnd();field.visitEnd();
                super.visitEnd();
            }
        },0);
        if(calls[0]!=1) throw new IllegalStateException("Expected exactly one original order-location call, got "+calls[0]);
        Files.createDirectories(Paths.get(args[1]).getParent());Files.write(Paths.get(args[1]),writer.toByteArray());
    }
}
