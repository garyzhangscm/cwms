import java.util.*;import java.lang.reflect.*;
import com.garyzhangscm.cwms.outbound.model.*;import com.garyzhangscm.cwms.outbound.service.*;import com.garyzhangscm.cwms.outbound.reportfix.*;
public class OrderPrintPolicyCheck {
 static List<Shipment> shipments=new ArrayList<>();static List<Pick> picks=new ArrayList<>();static boolean shortage=false;static int checks=0;
 static void check(boolean b,String m){checks++;if(!b)throw new AssertionError(m);}
 static void field(Object o,String n,Object v)throws Exception{Field f=o.getClass().getDeclaredField(n);f.setAccessible(true);f.set(o,v);}
 static Shipment shipment(long id,boolean ready){Shipment s=new Shipment();s.setId(id);s.setNumber("S"+id);s.setStatus(ShipmentStatus.INPROCESS);ShipmentLine l=new ShipmentLine();l.setId(id);l.setShipment(s);l.setOpenQuantity(0L);l.setQuantity(36L);s.setShipmentLines(List.of(l));Pick p=new Pick();p.setShipmentLine(l);p.setQuantity(36L);p.setPickedQuantity(ready?36L:12L);picks.add(p);shipments.add(s);return s;}
 public static void main(String[] a)throws Exception{
  OrderPrintPolicy policy=new OrderPrintPolicy();
  PickService pickService=new PickService(){public List<Pick> findByShipment(Shipment s){return picks.stream().filter(p->p.getShipmentLine().getShipment()==s).collect(java.util.stream.Collectors.toList());}public List<Pick> findByOrder(Order o){return picks;}};
  field(policy,"shipmentService",new ShipmentService(){public List<Shipment> findByOrder(Order o,boolean details){return shipments;}});field(policy,"pickService",pickService);field(policy,"shortAllocationService",new ShortAllocationService(){public List<ShortAllocation> findByShipment(Shipment s){return shortage?List.of(new ShortAllocation()):List.of();}});
  Order o=new Order();o.setStatus(OrderStatus.OPEN);o.setAllowForManualPick(true);OrderLine line=new OrderLine();line.setId(1L);line.setOpenQuantity(72L);o.setOrderLines(List.of(line));
  check(Boolean.TRUE.equals(policy.evaluate(o).get("manualPick")),"Open order manual sheet");check(Boolean.FALSE.equals(policy.evaluate(o).get("shipping")),"No empty-shipment shipping");
  Shipment first=shipment(1,false);o.setStatus(OrderStatus.INPROCESS);line.setOpenQuantity(0L);
  check(Boolean.TRUE.equals(policy.evaluate(o).get("manualPick")),"Partial pick still manual");check(Boolean.FALSE.equals(policy.evaluate(o).get("shipping")),"Partial pick cannot ship-print");
  picks.get(0).setPickedQuantity(36L);check(Boolean.FALSE.equals(policy.evaluate(o).get("manualPick")),"Fully picked manual denied");check(policy.require(o,false,null)==1L,"Ready print automatically scoped");
  shortage=true;check(Boolean.FALSE.equals(policy.evaluate(o).get("shipping")),"Short allocation blocks");shortage=false;
  first.getShipmentLines().get(0).setOpenQuantity(1L);check(Boolean.FALSE.equals(policy.evaluate(o).get("shipping")),"Unallocated shipment line blocks");first.getShipmentLines().get(0).setOpenQuantity(0L);
  Shipment second=shipment(2,false);check(policy.require(o,false,1L)==1L,"Ready shipment allowed with another unfinished");
  try{policy.require(o,false,2L);throw new AssertionError("Unready allowed");}catch(com.garyzhangscm.cwms.outbound.exception.OrderOperationException expected){checks++;}
  try{policy.require(o,false,999L);throw new AssertionError("Foreign shipment allowed");}catch(com.garyzhangscm.cwms.outbound.exception.OrderOperationException expected){checks++;}
  try(ShippingReportScope scope=new ShippingReportScope(1L)){check(ShippingReportScope.picks(pickService,o).size()==1,"Filter other shipment stock");}check(ShippingReportScope.current()==null,"Scope cleaned");
  picks.get(1).setPickedQuantity(36L);try{policy.require(o,false,null);throw new AssertionError("Ambiguous shipment allowed");}catch(com.garyzhangscm.cwms.outbound.exception.OrderOperationException expected){checks++;}
  first.setStatus(ShipmentStatus.DISPATCHED);o.setStatus(OrderStatus.COMPLETE);check(policy.require(o,false,1L)==1L,"Completed reprint");check(Boolean.FALSE.equals(policy.evaluate(o).get("manualPick")),"Closed manual denied");
  o.setStatus(OrderStatus.CANCELLED);check(Boolean.FALSE.equals(policy.evaluate(o).get("shipping")),"Cancelled denied");
  o.setStatus(OrderStatus.INPROCESS);line.setOpenQuantity(4L);picks.get(0).getShipmentLine().setOrderLine(line);picks.get(0).setPickedQuantity(30L);
  check(((Long)ManualPickDemandRows.build(o,List.of(picks.get(0))).get(0).get("openQuantity"))==10L,"Manual demand includes allocated but unpicked quantity");
  System.out.println("PASS "+checks+" policy/scoping/demand checks");
 }
}
