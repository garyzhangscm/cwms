import com.garyzhangscm.cwms.outbound.model.*;
import com.garyzhangscm.cwms.outbound.service.OrderService;
import com.garyzhangscm.cwms.outbound.clients.WarehouseLayoutServiceRestemplateClient;
import com.garyzhangscm.cwms.outbound.repository.OrderRepository;
import java.lang.reflect.*;
import java.util.*;
public class TransferAddressChecks {
 static class Layout extends WarehouseLayoutServiceRestemplateClient {
  public Warehouse getWarehouseById(Long id){
   Warehouse w=new Warehouse();
   if(id==1L){w.setAddressLine1("101 Source Street"); w.setAddressCity("Source City");w.setAddressPostcode("72701");}
   else if(id==4L){w.setAddressLine1("202 Destination Road");w.setAddressCity("Destination City");w.setAddressPostcode("72761");}
   else throw new AssertionError(id);
   w.setAddressCountry("US");w.setAddressState("AR");return w;
  }
 }
 static class AtRepository extends RuntimeException {}
 static void eq(Object a,Object b){if(!Objects.equals(a,b))throw new AssertionError(a+" vs "+b);}
 static void inject(OrderService s,String field,Object value)throws Exception {Field f=OrderService.class.getDeclaredField(field);f.setAccessible(true);f.set(s,value);}
 static Order order(OrderCategory cat){Order o=new Order();o.setCategory(cat);o.setWarehouseId(1L);o.setTransferReceiptWarehouseId(4L);return o;}
 static void save(OrderService s,Order o){try{s.save(o,false);throw new AssertionError();}catch(AtRepository expected){}}
 public static void main(String[] args)throws Exception {
  OrderService s=new OrderService();inject(s,"warehouseLayoutServiceRestemplateClient",new Layout());
  inject(s,"orderRepository",Proxy.newProxyInstance(TransferAddressChecks.class.getClassLoader(),new Class[]{OrderRepository.class},(p,m,a)->{if(!m.getName().equals("save"))throw new AssertionError();throw new AtRepository();}));
  Order o=order(OrderCategory.WAREHOUSE_TRANSFER_ORDER);save(s,o);
  eq(o.getShipToAddressLine1(),"202 Destination Road");eq(o.getShipToAddressCountry(),"US");eq(o.getShipToAddressPostcode(),"72761");
  for(OrderCategory cat:OrderCategory.values()){Order x=order(cat);x.setShipToAddressLine2("Suite B");save(s,x);eq(x.getShipToAddressLine1(),null);eq(x.getShipToAddressLine2(),"Suite B");}
  for(OrderCategory cat:new OrderCategory[]{OrderCategory.SALES_ORDER,OrderCategory.OUTSOURCING_ORDER}){Order x=order(cat);save(s,x);eq(x.getShipToAddressLine1(),null);}
  Order legacy=order(OrderCategory.WAREHOUSE_TRANSFER_ORDER);legacy.setId(105L);legacy.setNumber("TEST-TRANSFER");legacy.setShipToAddressLine1(" ");save(s,legacy);eq(legacy.getShipToAddressLine1()," ");
  for(String n:new String[]{"setupOrderPackingListParameters","setupOrderBillOfLadingParameters"}){
   Method m=OrderService.class.getDeclaredMethod(n,Report.class,Order.class);m.setAccessible(true);
   Report r=new Report();m.invoke(s,r,legacy);Map<String,Object> v=r.getParameters();
   eq(v.get("ship_from_address_line1"),"101 Source Street");eq(v.get("ship_from_address_city_state_zipcode"),"Source City, AR 72701");
   eq(v.get("ship_to_address_line1"),"202 Destination Road");eq(v.get("ship_to_address_city_state_zipcode"),"Destination City, AR 72761");eq(legacy.getShipToAddressLine1()," ");
   Order override=order(OrderCategory.WAREHOUSE_TRANSFER_ORDER);override.setShipToAddressLine1("Alternate destination");r=new Report();m.invoke(s,r,override);eq(r.getParameters().get("ship_to_address_line1"),"Alternate destination");
  }
  System.out.println("PASS creation at repository boundary, partial/explicit addresses, other categories, legacy read-only reprint, both reports and both address blocks");
 }
}
