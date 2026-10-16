import com.garyzhangscm.cwms.outbound.model.*;
import com.garyzhangscm.cwms.outbound.service.*;
import com.garyzhangscm.cwms.outbound.clients.*;
import com.garyzhangscm.cwms.outbound.repository.*;
import com.garyzhangscm.cwms.outbound.exception.OrderOperationException;
import java.lang.reflect.*;
import java.util.*;
import javax.persistence.*;
import org.springframework.transaction.*;
import org.springframework.transaction.support.SimpleTransactionStatus;
public class ManualPickOutboundRegression {
 static void set(Object o,String n,Object v)throws Exception{Field f=o.getClass().getDeclaredField(n);f.setAccessible(true);f.set(o,v);}
 static void require(boolean b,String m){if(!b)throw new AssertionError(m);}
 public static void main(String[]args)throws Exception {
  Order o=new Order();o.setId(1L);o.setWarehouseId(1L);o.setNumber("TEST");o.setAllowForManualPick(true);o.setStatus(OrderStatus.OPEN);
  OrderLine line=new OrderLine();line.setId(1L);line.setItemId(1L);line.setInventoryStatusId(1L);line.setOpenQuantity(40L);line.setNumber("1");line.setOrder(o);o.setOrderLines(List.of(line));
  Item item=new Item();item.setId(1L);InventoryStatus status=new InventoryStatus();status.setId(1L);
  Inventory stock=new Inventory();stock.setId(1L);stock.setItem(item);stock.setInventoryStatus(status);stock.setLocationId(2L);stock.setWarehouseId(1L);stock.setLpn("L000000001");
  OrderService service=new OrderService();
  set(service,"inventoryServiceRestemplateClient",new InventoryServiceRestemplateClient(){
   public List<Inventory> getInventoryByLpn(Long w,String l){return List.of(stock);}
   public InventoryConfiguration getInventoryConfiguration(Long w){return new InventoryConfiguration();}
   public List<Inventory> getPickableInventory(Long i,Long s,Long l,String lpn){return List.of(stock);}
  });
  set(service,"shipmentLineService",new ShipmentLineService(){public List<ShipmentLine> findByOrderLineId(Long w,Long id){return List.of();}});
  for(long amount:new long[]{1,39,40,41,325}){
   stock.setQuantity(amount);require(service.getPickableQuantityForManualPick(o,stock.getLpn(),false)==Math.min(40,amount),"Bound partial pick");
   if(amount>40){try{service.getPickableQuantityForManualPick(o,stock.getLpn(),true);throw new AssertionError("Legacy overpick accepted");}catch(OrderOperationException expected){}}
   else require(service.getPickableQuantityForManualPick(o,stock.getLpn(),true)==amount,"Full pallet within demand");
  }
  PreprintedManualPickService workflow=new PreprintedManualPickService();Map<String,ManualPickOperation> ops=new HashMap<>();
  EntityManager em=(EntityManager)Proxy.newProxyInstance(ManualPickOutboundRegression.class.getClassLoader(),new Class[]{EntityManager.class},(p,m,a)->{if(m.getName().equals("find")){require(a[2]==LockModeType.PESSIMISTIC_WRITE,"Cancellation must serialize with preparation");return o;}throw new UnsupportedOperationException(m.getName());});
  ManualPickOperationRepository repo=(ManualPickOperationRepository)Proxy.newProxyInstance(ManualPickOutboundRegression.class.getClassLoader(),new Class[]{ManualPickOperationRepository.class},(p,m,a)->{
   if(m.getName().equals("findById"))return Optional.ofNullable(ops.get(a[0]));
   if(m.getName().equals("saveAndFlush")){ManualPickOperation op=(ManualPickOperation)a[0];ops.put(op.requestId,op);return op;}throw new UnsupportedOperationException(m.getName());});
  set(workflow,"em",em);set(workflow,"operations",repo);
  set(workflow,"transactions",new PlatformTransactionManager(){public TransactionStatus getTransaction(TransactionDefinition d){return new SimpleTransactionStatus();}public void commit(TransactionStatus s){}public void rollback(TransactionStatus s){}});
  set(workflow,"layout",new WarehouseLayoutServiceRestemplateClient(){public Location getLocationByName(Long w,String n){Location l=new Location();l.setId(3L);return l;}});
  String id=UUID.randomUUID().toString();require(Boolean.TRUE.equals(workflow.cancelUnstarted(id,1L,1L).get("cancelled")),"Unstarted cancellation");
  try{workflow.process(id,1L,1L,"L000000001","L000000002",false,40L,325L,"RF");throw new AssertionError("Delayed request started after cancellation");}catch(OrderOperationException expected){}
  require(ops.get(id).pickId==null,"No picking generated for cancellation");
  ops.get(id).cancelled=false;ops.get(id).pickId=7L;require(Boolean.FALSE.equals(workflow.cancelUnstarted(id,1L,1L).get("cancelled")),"Reserved pick must not be discarded");
  ops.get(id).completed=true;require(Boolean.TRUE.equals(workflow.cancelUnstarted(id,1L,1L).get("completed")),"Completed acknowledgement can clear local pending state");
  System.out.println("PASS: actual legacy quantity calculation at 1/39/40/41/325, full-pallet overpick rejection, locked unstarted cancellation, late-submit tombstone, reserved-operation retention and completed acknowledgement. Dependencies mocked.");
 }
}
