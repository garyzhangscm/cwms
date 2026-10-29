import java.lang.reflect.*;import java.util.*;import net.bytebuddy.ByteBuddy;import net.bytebuddy.implementation.InvocationHandlerAdapter;import static net.bytebuddy.matcher.ElementMatchers.*;
import com.garyzhangscm.cwms.outbound.service.*;import com.garyzhangscm.cwms.outbound.model.*;
public class ShipmentDeleteChecks {
 static List<String> calls=new ArrayList<>();static boolean fail=false,busy=false;
 static Object stub(Class<?> type,String label)throws Exception{return new ByteBuddy().subclass(type).method(isPublic().and(not(isDeclaredBy(Object.class)))).intercept(InvocationHandlerAdapter.of((o,m,a)->{calls.add(label+"."+m.getName());if(label.equals("layout")&&m.getName().equals("releaseLocations")){if(fail)throw new IllegalStateException("release failed");if(!a[0].equals(1L)||a[1]==null)throw new AssertionError();}if(List.class.isAssignableFrom(m.getReturnType()))return busy&&label.equals("pick")?Arrays.asList(new Pick()):new ArrayList<>();return null;})).make().load(type.getClassLoader()).getLoaded().getDeclaredConstructor().newInstance();}
 public static void main(String[] args)throws Exception{
 ShipmentService svc=new ByteBuddy().subclass(ShipmentService.class).method(named("delete").and(takesArguments(Shipment.class))).intercept(InvocationHandlerAdapter.of((o,m,a)->{calls.add("DELETE");return null;})).make().load(ShipmentService.class.getClassLoader()).getLoaded().getDeclaredConstructor().newInstance();
 String[][] fields={{"pickService","pick"},{"shortAllocationService","short"},{"cancelledPickService","cancelled"},{"cancelledShortAllocationService","cancelledShort"},{"allocationTransactionHistoryService","history"},{"warehouseLayoutServiceRestemplateClient","layout"}};
 for(String[] f:fields){Field x=ShipmentService.class.getDeclaredField(f[0]);x.setAccessible(true);x.set(svc,stub(x.getType(),f[1]));}
 Shipment s=new Shipment();s.setWarehouseId(1L);s.setNumber("TEST-SHIP");
 svc.removeShipment(s);if(calls.indexOf("layout.releaseLocations")<0||calls.indexOf("layout.releaseLocations")>calls.indexOf("DELETE"))throw new AssertionError(calls);
 calls.clear();fail=true;try{svc.removeShipment(s);throw new AssertionError();}catch(IllegalStateException expected){}if(calls.contains("DELETE"))throw new AssertionError();
 calls.clear();fail=false;busy=true;try{svc.removeShipment(s);throw new AssertionError();}catch(RuntimeException expected){}if(calls.contains("DELETE")||calls.contains("layout.releaseLocations"))throw new AssertionError();
 System.out.println("PASS: null stage ID still releases by shipment; release before delete; failed release blocks delete; outstanding Pick blocks release/delete");
 }
}
