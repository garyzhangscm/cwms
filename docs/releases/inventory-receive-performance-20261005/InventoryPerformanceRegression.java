import java.lang.reflect.Field;
import java.nio.file.*;
import java.util.*;
import com.garyzhangscm.cwms.inventory.model.*;
import com.garyzhangscm.cwms.inventory.service.*;
import jdk.internal.org.objectweb.asm.*;
import jdk.internal.org.objectweb.asm.tree.*;

public class InventoryPerformanceRegression {
    static class InventoryStub extends InventoryService {
        int queries;
        List<Inventory> records = new ArrayList<>();
        @Override public List<Inventory> findByLocationId(Long id, boolean details) {
            if (!Long.valueOf(241).equals(id) || details) throw new AssertionError("Wrong destination query");
            queries++;
            return records;
        }
    }
    static class MixingStub extends InventoryMixRestrictionService {
        List<InventoryMixRestriction> rules = new ArrayList<>();
        int validations;
        boolean allowed = true;
        List<Inventory> expectedRecords;
        @Override public List<InventoryMixRestriction> findAll(Long warehouseId, Long type, Long group,
                Long location, String name, Long client, ClientRestriction restriction, boolean details) {
            return rules;
        }
        @Override public boolean checkMovementAllowed(InventoryMixRestriction rule, Inventory inventory,
                List<Inventory> records, Location destination) {
            if (records != expectedRecords) throw new AssertionError("Rules lost destination inventory");
            validations++;
            return allowed;
        }
    }
    static InventoryMixRestriction rule(Long client) {
        InventoryMixRestriction rule = new InventoryMixRestriction();
        rule.setWarehouseId(1L);
        rule.setClientId(client);
        return rule;
    }
    static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
    public static void main(String[] args) throws Exception {
        InventoryStub inventoryService = new InventoryStub();
        MixingStub mixing = new MixingStub();
        Field field = InventoryMixRestrictionService.class.getDeclaredField("inventoryService");
        field.setAccessible(true);
        field.set(mixing, inventoryService);
        Inventory inventory = new Inventory();
        inventory.setLpn("REGRESSION-NO-PRODUCTION");
        inventory.setWarehouseId(1L);
        Location location = new Location();
        location.setId(241L);
        location.setName("LINE04-OUT");
        mixing.expectedRecords = inventoryService.records;
        require(mixing.checkMovementAllowed(inventory, location), "No rules should allow move");
        require(inventoryService.queries == 0, "No rules must not load inventory");
        mixing.rules.add(rule(9L));
        require(mixing.checkMovementAllowed(inventory, location), "Unmatched rules should allow move");
        require(inventoryService.queries == 0, "Unmatched rules must not load inventory");
        mixing.rules.clear();
        mixing.rules.add(rule(null));
        inventoryService.records.add(new Inventory());
        require(mixing.checkMovementAllowed(inventory, location), "Allowed matching rule rejected");
        require(inventoryService.queries == 1 && mixing.validations == 1, "Matching rule must query and validate");
        mixing.allowed = false;
        require(!mixing.checkMovementAllowed(inventory, location), "Blocked matching rule bypassed");
        require(inventoryService.queries == 2 && mixing.validations == 2, "Blocked rule not validated");
        inventoryService.records.clear();
        require(!mixing.checkMovementAllowed(inventory, location), "Empty destination must preserve rule evaluation");
        require(inventoryService.queries == 3 && mixing.validations == 3, "Empty destination rule bypassed");
        mixing.rules.add(rule(null));
        require(!mixing.checkMovementAllowed(inventory, location), "Multiple rules must still block");
        require(mixing.validations == 4, "Blocked rules must retain short circuit");

        ClassNode node = new ClassNode();
        new ClassReader(Files.readAllBytes(Paths.get(args[0],
                "com/garyzhangscm/cwms/inventory/service/InventoryService.class"))).accept(node, 0);
        int movementQueries = 0, removalQueueCalls = 0;
        for (MethodNode method : node.methods) {
            for (AbstractInsnNode instruction : method.instructions.toArray()) {
                if (!(instruction instanceof MethodInsnNode)) continue;
                MethodInsnNode call = (MethodInsnNode) instruction;
                if (method.name.equals("processImmediateMoveInventory") && call.name.equals("findByLocationId")) movementQueries++;
                if (method.name.equals("removeInventores") && call.owner.endsWith("InventoryRemovalQueue")) removalQueueCalls++;
            }
        }
        require(movementQueries == 0, "Logging still loads destination inventory");
        require(removalQueueCalls > 0, "Existing durable removal queue patch lost");
        System.out.println("PASS: no-rule/unmatched fast paths, allowed/blocked/empty/multiple-rule validation, no logging queries, durable queue retained");
    }
}
