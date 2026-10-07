import com.garyzhangscm.cwms.workorder.service.*;
import com.garyzhangscm.cwms.workorder.clients.*;
import com.garyzhangscm.cwms.workorder.model.*;
import java.util.*;import java.lang.reflect.*;
public class ReturnedInventoryProbe {
 public static class Orders extends WorkOrderService {WorkOrder order=new WorkOrder();public WorkOrder findById(Long id){return order;}}
 public static class InventoryClient extends InventoryServiceRestemplateClient {int calls;public List<Inventory> getReturnedInventory(Long warehouse,String ids){if(!Long.valueOf(1).equals(warehouse)||!"11901".equals(ids))throw new AssertionError("wrong filter");calls++;return List.of(new Inventory());}}
 public static void main(String[] args)throws Exception {
  Orders orders=new Orders();orders.order.setWarehouseId(1L);InventoryClient inventory=new InventoryClient();Field f=WorkOrderService.class.getDeclaredField("inventoryServiceRestemplateClient");f.setAccessible(true);f.set(orders,inventory);
  if(!orders.getReturnedInventory(2310L).isEmpty()||inventory.calls!=0)throw new AssertionError("empty order queries inventory");
  orders.order.getWorkOrderLines().add(new WorkOrderLine());if(!orders.getReturnedInventory(2310L).isEmpty()||inventory.calls!=0)throw new AssertionError("null line ID queries inventory");
  WorkOrderLine line=new WorkOrderLine();line.setId(11901L);orders.order.getWorkOrderLines().add(line);if(orders.getReturnedInventory(2310L).size()!=1||inventory.calls!=1)throw new AssertionError("valid filter broken");
  InventoryServiceRestemplateClient base=new InventoryServiceRestemplateClient();for(String ids:new String[]{null,"","  "})if(!base.getReturnedInventory(1L,ids).isEmpty())throw new AssertionError("client guard failed");
  System.out.println("PASS actual JAR: empty/null-only order lines make no inventory requests; valid IDs retain exact warehouse filter; client guards null, empty and whitespace. No network or database accessed.");
 }
}
