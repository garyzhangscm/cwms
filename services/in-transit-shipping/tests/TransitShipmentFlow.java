import com.garyzhangscm.cwms.outbound.transit.*;
import com.garyzhangscm.cwms.outbound.clients.*;
import com.garyzhangscm.cwms.outbound.model.*;
import com.garyzhangscm.cwms.outbound.service.*;
import java.nio.file.*;
import java.util.*;

/** Executes real shipment routing and completion, with isolated in-memory remote boundaries. */
public class TransitShipmentFlow {
 static class Layout extends TransitRegression.Layout {
  int releases;
  Layout(){normal.setName("ORDER-fixture");LocationGroup g=new LocationGroup();LocationGroupType t=new LocationGroupType();t.setVirtual(true);t.setFourWallInventory(false);g.setLocationGroupType(t);normal.setLocationGroup(g);}
  public List<Location> releaseLocations(Long id,Shipment shipment){releases++;return Collections.emptyList();}
 }
 static class Flow extends ShipmentService {
  List<Inventory> staged=new ArrayList<>();int saves;
  public List<Inventory> getStagedInventory(Shipment s){return new ArrayList<>(staged);}
  public Shipment save(Shipment s){saves++;return s;}
 }
 static class Stock extends InventoryServiceRestemplateClient {
  int calls;
  public Inventory shipInventory(Inventory i,Location d){calls++;i.setLocation(d);i.setLocationId(d.getId());return i;}
 }
 static class Picks extends PickService { Pick pick;public Pick findById(Long id){return pick;} }
 static class Lines extends ShipmentLineService { public ShipmentLine save(ShipmentLine l){return l;} }
 static class Orders extends OrderLineService {public OrderLine saveOrUpdate(OrderLine l){return l;}}
 static class Billing extends BillableActivityService {
  long total;
  public void sendBillableActivity(Long company,Long warehouse,Long client,Long amount,String doc,String item,BillableCategory category){total+=amount;}
 }
 static int checks;
 static void check(boolean ok,String text){checks++;if(!ok)throw new AssertionError(text);}
 static void run(long customer,boolean enabled,long warehouse,boolean transit)throws Exception {
  Path dir=Files.createTempDirectory("transit-flow-");Path file=dir.resolve("rules.json");
  try {
   TransitRegression.seed(file,TransitRegression.config(0,true));Layout layout=new Layout();TransitShippingService routing=new TransitShippingService();
   TransitRegression.set(routing,"enabled",enabled);TransitRegression.set(routing,"companyId",1L);TransitRegression.set(routing,"warehouseId",1L);TransitRegression.set(routing,"customerIds","1,25");TransitRegression.set(routing,"locationIds","16169");TransitRegression.set(routing,"configFile",file.toString());TransitRegression.set(routing,"layout",layout);TransitRegression.set(routing,"common",new TransitRegression.Common());
   Order order=new Order();order.setWarehouseId(warehouse);order.setShipToCustomerId(customer);order.setNumber("fixture-order");
   OrderLine ol=new OrderLine();ol.setId(11L);ol.setOrder(order);ol.setWarehouseId(warehouse);ol.setInprocessQuantity(100L);ol.setShippedQuantity(0L);
   ShipmentLine sl=new ShipmentLine();sl.setId(12L);sl.setOrderLine(ol);sl.setInprocessQuantity(100L);sl.setLoadedQuantity(0L);sl.setShippedQuantity(0L);
   Picks picks=new Picks();picks.pick=new Pick();picks.pick.setShipmentLine(sl);
   Stock stock=new Stock();Billing billing=new Billing();Flow service=new Flow();
   for(long q:new long[]{40,60}){Inventory i=new Inventory();i.setId(q);i.setLpn("FIXTURE-"+q);i.setPickId(13L);i.setQuantity(q);service.staged.add(i);}
   for(Object[] pair:new Object[][]{{"transitShippingService",routing},{"warehouseLayoutServiceRestemplateClient",layout},{"inventoryServiceRestemplateClient",stock},{"pickService",picks},{"shipmentLineService",new Lines()},{"orderLineService",new Orders()},{"billableActivityService",billing}})TransitRegression.set(service,(String)pair[0],pair[1]);
   Shipment shipment=new Shipment();shipment.setWarehouseId(warehouse);shipment.setNumber("fixture-shipment");service.completeShipmentByOrder(shipment,order);
   check(stock.calls==2,"Both LPNs moved once");check(service.staged.stream().allMatch(i->i.getLocation()==(transit?layout.transit:layout.normal)),"Correct destination for customer/site");
   check(service.staged.stream().mapToLong(Inventory::getQuantity).sum()==100,"Stock quantity retained");
   check(sl.getShippedQuantity()==100 && sl.getLoadedQuantity()==100 && sl.getInprocessQuantity()==0,"Shipment line quantities preserved");
   check(ol.getShippedQuantity()==100 && ol.getInprocessQuantity()==0,"Order line quantities preserved");
   check(shipment.getStatus()==ShipmentStatus.DISPATCHED && shipment.getCompleteTime()!=null,"Shipment dispatch and time preserved");
   check(billing.total==100 && layout.releases==1 && service.saves==1,"Billing and completion side effects preserved");
   check(layout.originals==(transit?0:1),"Only selected FAY customers replace virtual destination");
   check(Boolean.TRUE.equals(service.staged.get(0).getLocation().getLocationGroup().getLocationGroupType().getVirtual())!=transit,"Physical versus virtual stock classified correctly");
  } finally {try(java.util.stream.Stream<Path> p=Files.walk(dir)){p.sorted(Comparator.reverseOrder()).forEach(x->{try{Files.delete(x);}catch(Exception e){throw new RuntimeException(e);}});}}
 }
 public static void main(String[] args)throws Exception {
  run(1,true,1,true);run(25,true,1,true);run(99,true,1,false);run(1,false,1,false);run(1,true,2,false);
  System.out.println("PASS: "+checks+" isolated shipment-flow checks; actual completion methods, no production writes or remote requests.");
 }
}
