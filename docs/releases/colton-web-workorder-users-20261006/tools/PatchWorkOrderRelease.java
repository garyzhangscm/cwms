import java.nio.file.*;
import java.util.jar.*;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

/** Preserve the running WorkOrder classes; replace only deletion entrypoints. */
public class PatchWorkOrderRelease {
    static final String PREFIX="com/garyzhangscm/cwms/workorder/";
    static ClassNode read(byte[] bytes){ClassNode c=new ClassNode();new ClassReader(bytes).accept(c,0);return c;}
    static void write(Path root,ClassNode c)throws Exception {ClassWriter w=new ClassWriter(0);c.accept(w);Path p=root.resolve(c.name+".class");Files.createDirectories(p.getParent());Files.write(p,w.toByteArray());}
    public static void main(String[] args)throws Exception {
        try(JarFile jar=new JarFile(args[0])){
            Path compiled=Path.of(args[1]),out=Path.of(args[2]);
            for(String suffix:new String[]{"controller/WorkOrderController","service/WorkOrderService"}){
                String name=PREFIX+suffix;
                ClassNode baseline=read(jar.getInputStream(jar.getJarEntry("BOOT-INF/classes/"+name+".class")).readAllBytes());
                ClassNode source=read(Files.readAllBytes(compiled.resolve(name+".class")));
                if(suffix.startsWith("controller")){
                    long old=baseline.methods.stream().filter(m->m.name.equals("removeWorkOrders")).count();
                    if(old!=1)throw new IllegalStateException("Expected one existing delete controller method");
                    baseline.methods.removeIf(m->m.name.equals("removeWorkOrders"));
                    source.methods.stream().filter(m->m.name.equals("removeWorkOrders")).forEach(baseline.methods::add);
                }else{
                    if(baseline.fields.stream().anyMatch(f->f.name.equals("workOrderDeletionService")))throw new IllegalStateException("Already patched");
                    baseline.fields.add(source.fields.stream().filter(f->f.name.equals("workOrderDeletionService")).findFirst().orElseThrow());
                    int disabled=0;
                    for(MethodNode m:baseline.methods){
                        if(!m.name.equals("delete"))continue;
                        m.instructions.clear();m.tryCatchBlocks.clear();m.localVariables=null;
                        m.visibleLocalVariableAnnotations=null;m.invisibleLocalVariableAnnotations=null;
                        m.instructions.add(new LdcInsnNode("Warehouse-scoped deletion is required. Use the protected work-orders endpoint."));
                        String exception=PREFIX+"exception/WorkOrderException";
                        m.instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC,exception,"raiseException","(Ljava/lang/String;)L"+exception+";",false));
                        m.instructions.add(new InsnNode(Opcodes.ATHROW));m.maxStack=1;disabled++;
                    }
                    if(disabled!=3)throw new IllegalStateException("Expected three unsafe legacy overloads, found "+disabled);
                    source.methods.stream().filter(m->m.name.equals("delete")).forEach(baseline.methods::add);
                }
                write(out,baseline);
            }
        }
        System.out.println("Patched deletion controller and service only; unsafe legacy overloads now reject.");
    }
}
