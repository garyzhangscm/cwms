package com.garyzhangscm.cwms.outbound.service;
import java.util.*;import java.lang.reflect.*;import javax.persistence.*;import net.bytebuddy.ByteBuddy;import net.bytebuddy.implementation.InvocationHandlerAdapter;import static net.bytebuddy.matcher.ElementMatchers.*;import com.garyzhangscm.cwms.outbound.model.*;import com.garyzhangscm.cwms.outbound.clients.*;import com.garyzhangscm.cwms.outbound.repository.*;
public class OutboundBatchChecks {
 static int gets,posts,lineSaves,orderSaves,bills;static List<Pick> work;static List<Map> rows;
 static void set(Object s,String n,Object v)throws Exception{Field f=s.getClass().getDeclaredField(n);f.setAccessible(true);f.set(s,v);}
 static Object stub(Class c,InvocationHandler h)throws Exception{if(c.isInterface())return Proxy.newProxyInstance(c.getClassLoader(),new Class[]{c},h);return new ByteBuddy().subclass(c).method(isPublic().and(not(isDeclaredBy(Object.class)))).intercept(InvocationHandlerAdapter.of(h)).make().load(c.getClassLoader()).getLoaded().getDeclaredConstructor().newInstance();}
 public static void main(String[] args)throws Exception {
  Shipment s=new Shipment();s.setId(7L);s.setWarehouseId(1L);s.setStatus(ShipmentStatus.INPROCESS);Order order=new Order();order.setId(9L);order.setWarehouseId(1L);order.setNumber("TESTORDER");s.setOrder(order);
  OrderLine ol=new OrderLine();ol.setId(10L);ol.setWarehouseId(1L);ol.setOrder(order);ol.setInprocessQuantity(330L);ol.setShippedQuantity(0L);
  ShipmentLine sl=new ShipmentLine();sl.setId(11L);sl.setShipment(s);sl.setOrderLine(ol);sl.setInprocessQuantity(330L);sl.setShippedQuantity(0L);
  work=new ArrayList<>();rows=new ArrayList<>();
  for(long id=1;id<=33;id++){Pick p=new Pick();p.setId(id);p.setItemId(3L);p.setQuantity(10L);p.setPickedQuantity(10L);p.setShipmentLine(sl);work.add(p);rows.add(new HashMap<>(Map.of("id",id,"quantity",10L,"pickId",id,"itemId",3L,"locationId",2L,"location",Map.of("locationGroup",Map.of("locationGroupType",Map.of("shippingStage",true,"fourWallInventory",true))))));}
  ShipmentCompletionBatchService service=new ShipmentCompletionBatchService();set(service,"em",Proxy.newProxyInstance(OutboundBatchChecks.class.getClassLoader(),new Class[]{EntityManager.class},(o,m,a)->null));
  set(service,"picks",stub(PickService.class,(o,m,a)->work));set(service,"shorts",stub(ShortAllocationService.class,(o,m,a)->Collections.emptyList()));
  set(service,"rest",stub(RestTemplateProxy.class,(o,m,a)->{if(m.getName().equals("exchangeList")){gets++;return rows;}posts++;return Map.of("schema","shipment-inventory-batch-v1","rows",((Map)a[3]).get("rows"));}));
  Location dest=new Location();dest.setId(20L);set(service,"layout",stub(WarehouseLayoutServiceRestemplateClient.class,(o,m,a)->m.getName().equals("createOrderLocation")?dest:Collections.emptyList()));
  set(service,"shipmentLines",stub(ShipmentLineService.class,(o,m,a)->{lineSaves++;return a[0];}));set(service,"orderLines",stub(OrderLineService.class,(o,m,a)->{orderSaves++;return a[0];}));set(service,"shipments",stub(ShipmentRepository.class,(o,m,a)->a[0]));set(service,"billing",stub(BillableActivityService.class,(o,m,a)->{bills++;return null;}));
  service.complete(s);if(gets!=1||posts!=1||lineSaves!=1||orderSaves!=1||bills!=33||ol.getShippedQuantity()!=330||sl.getShippedQuantity()!=330||ol.getInprocessQuantity()!=0)throw new AssertionError("aggregation/count mismatch "+gets+" "+posts+" "+bills);
  service.complete(s);if(posts!=1||bills!=33)throw new AssertionError("duplicate dispatched completion");
  s.setStatus(ShipmentStatus.INPROCESS);work.get(0).setPickedQuantity(0L);try{service.complete(s);throw new AssertionError("unfinished Pick accepted");}catch(IllegalArgumentException expected){}if(posts!=1)throw new AssertionError();
  System.out.println("PASS: 33 inventory rows => one snapshot, one batch POST, one shipment/order line save, 33 original billing events; completed replay skipped; unfinished Pick blocks batch");
 }
}
