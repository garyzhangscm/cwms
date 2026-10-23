import com.garyzhangscm.cwms.outbound.transit.*;
import com.garyzhangscm.cwms.outbound.clients.*;
import com.garyzhangscm.cwms.outbound.model.*;
import com.garyzhangscm.cwms.outbound.service.ShipmentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.server.ResponseStatusException;
import java.nio.file.*;
import java.lang.reflect.*;
import java.util.*;

/** Runs the actual patched routing method with fake remote clients; performs no inventory writes. */
public class TransitRegression {
 static int checks;
 static void check(boolean yes,String label){checks++;if(!yes)throw new AssertionError(label);}
 static void set(Object object,String name,Object value)throws Exception{
  Class<?> type=object.getClass();while(type!=null){try{Field f=type.getDeclaredField(name);f.setAccessible(true);f.set(object,value);return;}catch(NoSuchFieldException e){type=type.getSuperclass();}}
  throw new NoSuchFieldException(name);
 }
 static void rejects(Runnable work,int status,String label){try{work.run();throw new AssertionError(label);}catch(ResponseStatusException e){check(e.getStatus().value()==status,label);}}
 static class Layout extends WarehouseLayoutServiceRestemplateClient {
  final Warehouse warehouse=new Warehouse();final Location transit=new Location();final Location normal=new Location();int originals;
  Layout(){warehouse.setId(1L);warehouse.setCompanyId(1L);transit.setId(16169L);transit.setName("Trans_shipping");transit.setWarehouse(warehouse);transit.setEnabled(true);
   LocationGroup group=new LocationGroup();group.setPickable(false);LocationGroupType type=new LocationGroupType();type.setVirtual(false);type.setFourWallInventory(true);group.setLocationGroupType(type);transit.setLocationGroup(group);normal.setId(999L);}
  public Warehouse getWarehouseById(Long id){return warehouse;}
  public Location getLocationById(Long id){return transit;}
  public Location createOrderLocation(Long id,Order order){originals++;return normal;}
 }
 static class Common extends CommonServiceRestemplateClient {
  public Customer getCustomerById(Long id){Customer c=new Customer();c.setId(id);c.setName("Olivet");c.setCompanyId(1L);return c;}
 }
 static class Capture extends ShipmentService {
  Location destination;
  public void completeShipment(Shipment shipment,Location location){destination=location;}
 }
 static TransitShippingConfiguration config(long revision,boolean enabled){TransitShippingConfiguration c=new TransitShippingConfiguration();c.revision=revision;c.rules=new ArrayList<>();c.rules.add(new TransitShippingConfiguration.Rule(1L,16169L,enabled));c.rules.add(new TransitShippingConfiguration.Rule(25L,16169L,enabled));return c;}
 static void seed(Path path,TransitShippingConfiguration value)throws Exception{new ObjectMapper().writeValue(path.toFile(),value);}
 public static void main(String[] args)throws Exception{
  Path dir=Files.createTempDirectory("transit-regression-");Path path=dir.resolve("rules.json");Layout layout=new Layout();TransitShippingService service=new TransitShippingService();
  set(service,"enabled",true);set(service,"companyId",1L);set(service,"warehouseId",1L);set(service,"customerIds","1,25");set(service,"locationIds","16169");set(service,"configFile",path.toString());set(service,"layout",layout);set(service,"common",new Common());
  Capture shipment=new Capture();set(shipment,"warehouseLayoutServiceRestemplateClient",layout);set(shipment,"transitShippingService",service);
  Order order=new Order();order.setWarehouseId(1L);order.setShipToCustomerId(99L);
  shipment.completeShipmentByOrder(new Shipment(),order);check(shipment.destination==layout.normal,"Other customers ignore missing config");
  order.setShipToCustomerId(25L);rejects(()->shipment.completeShipmentByOrder(new Shipment(),order),503,"Selected customer blocked on missing config");
  seed(path,config(0,true));shipment.completeShipmentByOrder(new Shipment(),order);check(shipment.destination==layout.transit,"Fortsmith retains actual location");
  order.setShipToCustomerId(1L);shipment.completeShipmentByOrder(new Shipment(),order);check(shipment.destination==layout.transit,"Olivet global customer retains location");
  order.setWarehouseId(2L);shipment.completeShipmentByOrder(new Shipment(),order);check(shipment.destination==layout.normal,"Different warehouse unchanged");order.setWarehouseId(1L);
  set(service,"enabled",false);shipment.completeShipmentByOrder(new Shipment(),order);check(shipment.destination==layout.normal,"Default-off behavior unchanged");set(service,"enabled",true);
  seed(path,config(0,false));shipment.completeShipmentByOrder(new Shipment(),order);check(shipment.destination==layout.normal,"Explicit disabled rule unchanged");seed(path,config(0,true));
  layout.transit.getLocationGroup().getLocationGroupType().setVirtual(true);rejects(()->shipment.completeShipmentByOrder(new Shipment(),order),409,"Virtual destination forbidden");layout.transit.getLocationGroup().getLocationGroupType().setVirtual(false);
  layout.transit.getLocationGroup().setPickable(true);rejects(()->shipment.completeShipmentByOrder(new Shipment(),order),409,"Pickable transit forbidden");layout.transit.getLocationGroup().setPickable(false);
  layout.transit.setEnabled(false);rejects(()->shipment.completeShipmentByOrder(new Shipment(),order),409,"Disabled location forbidden");layout.transit.setEnabled(true);
  Warehouse wrong=new Warehouse();wrong.setId(2L);layout.transit.setWarehouse(wrong);rejects(()->shipment.completeShipmentByOrder(new Shipment(),order),409,"Cross warehouse destination forbidden");layout.transit.setWarehouse(layout.warehouse);
  Files.writeString(path,"broken");rejects(()->shipment.completeShipmentByOrder(new Shipment(),order),503,"Corrupt config blocks selected only");order.setShipToCustomerId(99L);shipment.completeShipmentByOrder(new Shipment(),order);check(shipment.destination==layout.normal,"Other customer unaffected by corrupt config");order.setShipToCustomerId(1L);
  seed(path,config(0,true));TransitShippingConfiguration saved=service.save(1L,config(0,true),"admin-test");check(saved.revision==1 && "admin-test".equals(saved.modifiedBy) && saved.modifiedAt!=null,"Revision and audit saved");rejects(()->service.save(1L,config(0,true),"stale-admin"),409,"Stale save rejected");
  TransitShippingConfiguration duplicate=config(1,true);duplicate.rules.add(new TransitShippingConfiguration.Rule(1L,16169L,true));rejects(()->service.save(1L,duplicate,"admin"),400,"Duplicate customer forbidden");
  TransitShippingConfiguration outside=config(1,true);outside.rules.get(0).customerId=99L;rejects(()->service.save(1L,outside,"admin"),400,"Other customer cannot be configured");
  rejects(()->service.save(2L,config(1,true),"admin"),403,"Wrong warehouse cannot save");
  TransitConfigurationStore store=new TransitConfigurationStore(path);java.util.concurrent.ExecutorService pool=java.util.concurrent.Executors.newFixedThreadPool(2);
  java.util.concurrent.Callable<Boolean> write=()->{try{store.write(config(1,true),"concurrent");return true;}catch(ResponseStatusException e){if(e.getStatus().value()!=409)throw e;return false;}};
  java.util.List<java.util.concurrent.Future<Boolean>> results=pool.invokeAll(Arrays.asList(write,write));int winners=0;for(java.util.concurrent.Future<Boolean> result:results)if(result.get())winners++;pool.shutdown();check(winners==1 && store.read().revision==2,"Concurrent saves have one winner");
  check(((Boolean)service.metadata(1L).get("featureEnabled")),"Configured metadata enabled");check(!((Boolean)service.metadata(2L).get("featureEnabled")),"Other warehouse metadata hidden");
  try(java.util.stream.Stream<Path> paths=Files.walk(dir)){paths.sorted(Comparator.reverseOrder()).forEach(p->{try{Files.delete(p);}catch(Exception e){throw new RuntimeException(e);}});}
  System.out.println("PASS: "+checks+" regression checks, including actual patched ShipmentService; no remote requests or inventory writes.");
 }
}
