import java.nio.file.*;
import jdk.internal.org.objectweb.asm.*;
import jdk.internal.org.objectweb.asm.tree.*;

public class PatchInventoryPerformance implements Opcodes {
    static final String ROOT = "com/garyzhangscm/cwms/inventory/service/";
    static AbstractInsnNode previousCode(AbstractInsnNode n) {
        do { n = n.getPrevious(); } while (n != null && n.getOpcode() < 0);
        return n;
    }
    static void require(boolean value, String message) {
        if (!value) throw new IllegalStateException(message);
    }
    public static void main(String[] args) throws Exception {
        for (String name : new String[]{"InventoryService", "InventoryMixRestrictionService"}) {
            byte[] original = Files.readAllBytes(Paths.get(args[0], ROOT + name + ".class"));
            ClassReader reader = new ClassReader(original);
            ClassNode node = new ClassNode();
            reader.accept(node, ClassReader.EXPAND_FRAMES);
            int changes = 0;
            for (MethodNode m : node.methods) {
                if (name.equals("InventoryService") && m.name.equals("processImmediateMoveInventory")) {
                    for (AbstractInsnNode instruction : m.instructions.toArray()) {
                        if (!(instruction instanceof LdcInsnNode)) continue;
                        Object value = ((LdcInsnNode) instruction).cst;
                        if (!"6. destination {} has {} inventory".equals(value) &&
                            !"7. destination {} has {} inventory".equals(value)) continue;
                        AbstractInsnNode start = previousCode(instruction);
                        require(start instanceof FieldInsnNode && start.getOpcode() == GETSTATIC &&
                                ((FieldInsnNode) start).name.equals("logger"), "Unexpected log prefix");
                        AbstractInsnNode end = instruction;
                        int queries = 0;
                        while (!(end instanceof MethodInsnNode && ((MethodInsnNode) end).owner.equals("org/slf4j/Logger") &&
                                 ((MethodInsnNode) end).name.equals("debug"))) {
                            if (end instanceof MethodInsnNode && ((MethodInsnNode) end).name.equals("findByLocationId")) queries++;
                            require(end.getOpcode() != GOTO && !(end instanceof JumpInsnNode), "Unexpected branch in logging");
                            end = end.getNext();
                            require(end != null, "Missing logger call");
                        }
                        require(queries == 1, "Unexpected logging query count");
                        AbstractInsnNode after = end.getNext();
                        while (start != after) {
                            AbstractInsnNode next = start.getNext();
                            m.instructions.remove(start);
                            start = next;
                        }
                        changes++;
                    }
                }
                if (name.equals("InventoryMixRestrictionService") && m.name.equals("checkMovementAllowed") &&
                    m.desc.equals("(Lcom/garyzhangscm/cwms/inventory/model/Inventory;Lcom/garyzhangscm/cwms/inventory/model/Location;)Z")) {
                    MethodInsnNode matched = null, query = null;
                    for (AbstractInsnNode instruction : m.instructions.toArray()) {
                        if (!(instruction instanceof MethodInsnNode)) continue;
                        MethodInsnNode call = (MethodInsnNode) instruction;
                        if (call.name.equals("getMatchedInventoryMixRestriction")) matched = call;
                        if (call.name.equals("findByLocationId")) query = call;
                    }
                    require(matched != null && query != null, "Missing baseline mixing calls");
                    AbstractInsnNode ruleStart = previousCode(previousCode(previousCode(matched)));
                    AbstractInsnNode ruleStore = matched.getNext();
                    require(ruleStart instanceof VarInsnNode && ruleStart.getOpcode() == ALOAD && ((VarInsnNode)ruleStart).var == 0,
                            "Unexpected rule loading prefix");
                    require(ruleStore instanceof VarInsnNode && ruleStore.getOpcode() == ASTORE && ((VarInsnNode)ruleStore).var == 4,
                            "Unexpected rule local variable");
                    AbstractInsnNode queryStart = query;
                    for (int i = 0; i < 5; i++) queryStart = previousCode(queryStart);
                    require(queryStart instanceof VarInsnNode && queryStart.getOpcode() == ALOAD && ((VarInsnNode)queryStart).var == 0,
                            "Unexpected destination loading prefix");
                    InsnList rules = new InsnList();
                    AbstractInsnNode after = ruleStore.getNext();
                    while (ruleStart != after) {
                        AbstractInsnNode next = ruleStart.getNext();
                        m.instructions.remove(ruleStart);
                        rules.add(ruleStart);
                        ruleStart = next;
                    }
                    rules.add(new VarInsnNode(ALOAD, 4));
                    rules.add(new MethodInsnNode(INVOKEINTERFACE, "java/util/List", "isEmpty", "()Z", true));
                    LabelNode hasRules = new LabelNode();
                    rules.add(new JumpInsnNode(IFEQ, hasRules));
                    rules.add(new InsnNode(ICONST_1));
                    rules.add(new InsnNode(IRETURN));
                    rules.add(hasRules);
                    m.instructions.insertBefore(queryStart, rules);
                    changes++;
                }
            }
            require(changes == (name.equals("InventoryService") ? 2 : 1), "Unexpected patch count for " + name);
            ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS) {
                @Override protected ClassLoader getClassLoader() {
                    return PatchInventoryPerformance.class.getClassLoader();
                }
            };
            node.accept(writer);
            Path output = Paths.get(args[1], ROOT + name + ".class");
            Files.createDirectories(output.getParent());
            Files.write(output, writer.toByteArray());
            System.out.println(name + ": verified " + changes + " targeted changes");
        }
    }
}
