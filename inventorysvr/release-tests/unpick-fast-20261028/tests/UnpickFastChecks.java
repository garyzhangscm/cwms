import com.garyzhangscm.cwms.inventory.service.*;
import com.garyzhangscm.cwms.inventory.clients.*;
import com.garyzhangscm.cwms.inventory.model.*;
import java.util.*;import java.lang.reflect.*;
public class UnpickFastChecks {
 static int passed;static void check(boolean b,String m){if(!b)throw new AssertionError(m);passed++;}
 static void field(Object o,Class<?> type,String n,Object v)throws Exception{Field f=type.getDeclaredField(n);f.setAccessible(true);f.set(o,v);}
 static class Layout extends WarehouseLayoutServiceRestemplateClient {InventoryConsolidationStrategy strategy=InventoryConsolidationStrategy.NONE;int allocated;boolean fail;
  @Override public InventoryConsolidationStrategy getInventoryConsolidationStrategy(long id){if(fail)throw new IllegalStateException("layout down");return strategy;}
  @Override public Location allocateLocation(Location l,Inventory i){allocated++;return l;}
 }
 static class Activity extends InventoryActivityService {int unpicked;@Override public void logInventoryActivitiy(Inventory i,InventoryActivityType t,String v,String from,String to,String doc){if(t==InventoryActivityType.UNPICKING)unpicked++;}
 @Override public void logInventoryActivitiy(Inventory i,InventoryActivityType t,String v,String from,String to){if(t==InventoryActivityType.UNPICKING)unpicked++;}}
 static class Outbound extends OutbuondServiceRestemplateClient {int calls;long quantity;boolean fail;@Override public List<Pick> unpick(Long p,Long q){if(fail)throw new IllegalStateException("sync failed");calls++;quantity+=q;return List.of();}}
 static class Movement extends InventoryMovementService {int cleared;@Override public void clearInventoryMovement(Inventory i){cleared++;}}
 static class InventoryFixture extends InventoryService {int reads,saves,moves,paths;boolean moveFail;List<Inventory> candidates=new ArrayList<>();
  @Override public List<Inventory> findByLocationId(Long id,boolean details){reads++;return new ArrayList<>(candidates);}
  @Override public Inventory save(Inventory i){saves++;return i;}
  @Override public Inventory moveInventory(Inventory i,Location l){moves++;if(moveFail)throw new IllegalStateException("move failed");i.setLocation(l);i.setLocationId(l.getId());return save(i);}
  @Override public Inventory setupMovementPath(Inventory i,List<InventoryMovement> m){paths++;return i;}
 }
 static Inventory row(long id,String lpn){Inventory i=new Inventory();i.setId(id);i.setLpn(lpn);i.setQuantity(305L);i.setPickId(21L);return i;}
 static Location location(){Location l=new Location();l.setId(289L);l.setName("C1");LocationGroup g=new LocationGroup();g.setId(20L);l.setLocationGroup(g);return l;}
 static void rejects(Runnable r){try{r.run();throw new AssertionError("failure not propagated");}catch(IllegalStateException expected){passed++;}}
 public static void main(String[] args)throws Exception{
  Layout layout=new Layout();Activity activity=new Activity();InventoryFixture inv=new InventoryFixture();Outbound outbound=new Outbound();Movement movement=new Movement();
  field(inv,InventoryService.class,"warehouseLayoutServiceRestemplateClient",layout);field(inv,InventoryService.class,"inventoryActivityService",activity);field(inv,InventoryService.class,"outbuondServiceRestemplateClient",outbound);field(inv,InventoryService.class,"inventoryMovementService",movement);
  InventoryConsolidationService cs=new InventoryConsolidationService();field(cs,InventoryConsolidationService.class,"warehouseLayoutServiceRestemplateClient",layout);field(cs,InventoryConsolidationService.class,"inventoryService",inv);field(cs,InventoryConsolidationService.class,"inventoryActivityService",activity);
  Inventory stock=row(1,"W10000237375");check(cs.consolidateInventoryAtLocation(location(),stock)==stock&&inv.reads==0,"NONE avoids all location inventory reads");
  layout.strategy=InventoryConsolidationStrategy.CONSOLIDATE_BY_LPN;inv.candidates.add(row(2,"TARGET"));check(cs.consolidateInventoryAtLocation(location(),stock)==stock&&"TARGET".equals(stock.getLpn())&&inv.reads==1,"configured LPN consolidation is retained");
  inv.candidates.add(row(3,"ANOTHER"));try{cs.consolidateInventoryAtLocation(location(),row(4,"SOURCE"));throw new AssertionError("mixed destination accepted");}catch(com.garyzhangscm.cwms.inventory.exception.InventoryConsolidationException expected){passed++;}
  layout.strategy=InventoryConsolidationStrategy.CONSOLIDATE_BY_INVENTORY;inv.candidates.clear();int count=inv.reads;cs.consolidateInventoryAtLocation(location(),stock);check(inv.reads==count+1,"inventory consolidation still queries candidates");
  layout.fail=true;count=inv.reads;rejects(()->cs.consolidateInventoryAtLocation(location(),stock));check(inv.reads==count,"strategy failure does not silently skip consolidation");layout.fail=false;
  Inventory first=row(5,"SOURCE");check(inv.unpick(first,location(),true)==first,"immediate unpick returns saved movement");check(inv.saves==1&&inv.moves==1,"immediate movement saves exactly once");check(outbound.calls==1&&outbound.quantity==305&&activity.unpicked==1&&movement.cleared==1&&first.getPickId()==null,"quantity synchronization, audit and movement cleanup retained");
  inv.unpick(first,location(),true);check(inv.saves==1&&outbound.calls==1&&activity.unpicked==1,"already unpicked replay has no duplicate quantity or audit");
  Inventory deferred=row(6,"DEFER");inv.unpick(deferred,location(),false);check(inv.saves==2&&inv.moves==1&&inv.paths==1&&layout.allocated==1,"deferred movement keeps its necessary save and path");check(outbound.quantity==610&&activity.unpicked==2,"deferred audit and quantity sync retained");
  outbound.fail=true;rejects(()->inv.unpick(row(7,"SYNCFAIL"),location(),true));check(inv.saves==2&&activity.unpicked==2,"sync failure stops movement and local save");outbound.fail=false;
  inv.moveFail=true;rejects(()->inv.unpick(row(8,"MOVEFAIL"),location(),true));check(inv.saves==2,"failed movement never returns success or invokes outer save");
  System.out.println("PASS "+passed+" Unpick fast checks against patched production method; strategy, sync, audit, deferred path, retry and errors");
 }
}
