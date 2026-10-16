import com.garyzhangscm.cwms.inventory.model.*;
import com.garyzhangscm.cwms.inventory.service.*;
import com.garyzhangscm.cwms.inventory.repository.*;
import com.garyzhangscm.cwms.inventory.clients.*;
import jakarta.persistence.*;
import java.lang.reflect.*;
import java.util.*;
import org.springframework.http.HttpMethod;
public class ManualPickInventoryRegression {
 static void set(Object target,String name,Object value)throws Exception {Field f=target.getClass().getDeclaredField(name);f.setAccessible(true);f.set(target,value);}
 static void require(boolean value,String message){if(!value)throw new AssertionError(message);}
 static class Stock extends InventoryService {
  Map<Long,Inventory> rows=new HashMap<>(); int splits,moves; boolean used;
  public void loadInventoryAttribute(Inventory i){}
  public void validateLPN(Long w,String l,boolean n){}
  public String validateNewLPN(Long w,String l){return used?"VALUE_ALREADY_EXISTS":"";}
  public Inventory findById(Long id){return rows.get(id);}
  public Inventory saveOrUpdate(Inventory i){rows.put(i.getId(),i);return i;}
  public List<Inventory> splitInventory(Inventory i,String l,Long q){splits++;Inventory n=new Inventory();n.setId(2L);n.setLpn(l);n.setQuantity(q);n.setWarehouseId(i.getWarehouseId());n.setItem(i.getItem());n.setLocationId(i.getLocationId());n.setAllocatedByPickId(i.getAllocatedByPickId());i.setQuantity(i.getQuantity()-q);rows.put(2L,n);return List.of(i,n);}
  public Inventory moveInventory(Inventory i,Location d,Long p,boolean immediate,String destinationLpn){moves++;i.setLocationId(d.getId());i.setPickId(p);return i;}
 }
 static class Fixture {
  Stock stock=new Stock();Map<String,ManualPickInventoryOperation> operations=new HashMap<>();ManualPickInventoryService service=new ManualPickInventoryService();
  Location target=new Location();Pick pick=new Pick();boolean issued=true;String currentRequest;
  Fixture()throws Exception{
   Warehouse warehouse=new Warehouse();warehouse.setId(7L);target.setId(200L);target.setWarehouse(warehouse);
   Item item=new Item();item.setId(10L);Inventory source=new Inventory();source.setId(1L);source.setWarehouseId(7L);source.setLpn("L000000001");source.setQuantity(325L);source.setItem(item);source.setLocationId(100L);source.setAllocatedByPickId(77L);stock.rows.put(1L,source);
   pick.setItemId(10L);pick.setSourceLocationId(100L);pick.setQuantity(40L);pick.setPickedQuantity(0L);
   EntityManager em=(EntityManager)Proxy.newProxyInstance(getClass().getClassLoader(),new Class[]{EntityManager.class},(p,m,a)->{if(m.getName().equals("find")){require(a.length==3&&a[2]==LockModeType.PESSIMISTIC_WRITE,"Source row lock required");if(a[0]==ManualPickInventoryOperation.class)return operations.get(a[1]);return stock.rows.get(a[1]);}throw new UnsupportedOperationException(m.getName());});
   ManualPickInventoryOperationRepository repo=(ManualPickInventoryOperationRepository)Proxy.newProxyInstance(getClass().getClassLoader(),new Class[]{ManualPickInventoryOperationRepository.class},(p,m,a)->{
    if(m.getName().equals("findById"))return Optional.ofNullable(operations.get(a[0]));
    if(m.getName().equals("saveAndFlush")){ManualPickInventoryOperation op=(ManualPickInventoryOperation)a[0];operations.put(op.requestId,op);return op;}throw new UnsupportedOperationException(m.getName());});
   set(service,"em",em);set(service,"inventory",stock);set(service,"operations",repo);
   set(service,"outbound",new OutbuondServiceRestemplateClient(){public Pick getPickById(Long id){return pick;}});
   set(service,"layout",new WarehouseLayoutServiceRestemplateClient(){public Location getLocationById(Long id){return target;}});
   set(service,"rest",new RestTemplateProxy(){public <T>T exchange(Class<T> type,String url,HttpMethod method,Object body){return type.cast(Map.of("issued",issued,"reservedFor",currentRequest));}});
  }
  Inventory apply(String id,boolean keep,String lpn,Long q)throws Exception{currentRequest=id;return service.apply(id,7L,1L,"L000000001",325L,q,lpn,keep,77L,target);}
 }
 public static void main(String[] args)throws Exception {
  for(boolean keep:new boolean[]{false,true}){
   Fixture f=new Fixture();String id=UUID.randomUUID().toString();Inventory result=f.apply(id,keep,"L000000002",40L);
   require(result.getQuantity()==40L,"Only 40 picked");require(result.getLpn().equals(keep?"L000000001":"L000000002"),"Picked LPN role");
   Inventory remaining=f.stock.rows.values().stream().filter(i->i!=result).findFirst().get();require(remaining.getQuantity()==285L,"285 retained");require(remaining.getLocationId()==100L,"Remaining stock stays at source");require(remaining.getAllocatedByPickId()==null,"Remaining stock available for future orders");
   f.apply(id,keep,"L000000002",40L);require(f.stock.splits==1&&f.stock.moves==1,"Acknowledgement lost: retry must not split or move again");
   try{f.apply(id,!keep,"L000000002",40L);throw new AssertionError("Changed retry accepted");}catch(com.garyzhangscm.cwms.inventory.exception.InventoryException expected){}
  }
  for(String lpn:new String[]{"L000000001","DE0000000001","invalid"}){
   Fixture f=new Fixture();try{f.apply(UUID.randomUUID().toString(),false,lpn,40L);throw new AssertionError("Invalid label accepted");}catch(com.garyzhangscm.cwms.inventory.exception.InventoryException expected){}
   require(f.stock.splits==0&&f.stock.moves==0,"Invalid label must not change stock");
  }
  Fixture unissued=new Fixture();unissued.issued=false;try{unissued.apply(UUID.randomUUID().toString(),false,"L000000002",40L);throw new AssertionError("Unissued label accepted");}catch(com.garyzhangscm.cwms.inventory.exception.InventoryException expected){}
  require(unissued.stock.splits==0,"Reject unissued before splitting");
  Fixture over=new Fixture();try{over.apply(UUID.randomUUID().toString(),false,"L000000002",325L);throw new AssertionError("Overpick accepted");}catch(com.garyzhangscm.cwms.inventory.exception.InventoryException expected){}
  require(over.stock.splits==0,"Cannot exceed allocated demand");
  System.out.println("PASS: both LPN roles, 40+285 conservation, source location, released remainder, row lock, idempotent retry, changed retry, invalid/DE/used-source labels, unissued labels and demand limit. External dependencies mocked.");
 }
}
