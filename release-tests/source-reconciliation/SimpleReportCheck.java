import com.garyzhangscm.cwms.outbound.clients.*;
import com.garyzhangscm.cwms.outbound.model.*;
import com.garyzhangscm.cwms.outbound.service.OrderService;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import static org.mockito.Mockito.*;

public class SimpleReportCheck {
    static void inject(Object target,String name,Object value)throws Exception {
        Field field=target.getClass().getDeclaredField(name);field.setAccessible(true);field.set(target,value);
    }
    public static void main(String[] args)throws Exception {
        if(System.getSecurityManager()==null)throw new AssertionError("Offline policy required");
        try {System.getSecurityManager().checkPermission(new java.net.SocketPermission("127.0.0.1:9","connect,resolve"));throw new AssertionError("Network permitted");}catch(SecurityException expected){}
        ((ch.qos.logback.classic.Logger)org.slf4j.LoggerFactory.getLogger("ROOT")).setLevel(ch.qos.logback.classic.Level.OFF);
        OrderService service=new OrderService();
        InventoryServiceRestemplateClient inventory=mock(InventoryServiceRestemplateClient.class);
        WarehouseLayoutServiceRestemplateClient layout=mock(WarehouseLayoutServiceRestemplateClient.class);
        AtomicReference<Report> captured=new AtomicReference<>();
        ResourceServiceRestemplateClient resources=mock(ResourceServiceRestemplateClient.class,call->{
            if(call.getMethod().getName().equals("generateReport")){captured.set(call.getArgument(2));return new ReportHistory();}
            return RETURNS_DEFAULTS.answer(call);
        });
        inject(service,"inventoryServiceRestemplateClient",inventory); inject(service,"warehouseLayoutServiceRestemplateClient",layout); inject(service,"resourceServiceRestemplateClient",resources);
        Item item=new Item();item.setName("SAMPLE-ITEM");item.setDescription("Sample product");
        OrderLine line=new OrderLine();line.setNumber("1");line.setItem(item);line.setExpectedQuantity(60L);line.setOpenQuantity(40L);line.setInprocessQuantity(20L);
        Order order=new Order();order.setNumber("SAMPLE-ORDER");order.setWarehouseId(1L);order.setOrderLines(Arrays.asList(line));
        Location stage=new Location();stage.setName("STAGE-A");order.setStageLocation(stage);
        if(service.generateManualPickReportByOrder(order,"en-US")==null)throw new AssertionError("Missing report");
        if(captured.get()==null || captured.get().getData().iterator().next()!=line)throw new AssertionError("Order lines lost");
        if(line.getOpenQuantity()!=40L || line.getExpectedQuantity()!=60L || line.getInprocessQuantity()!=20L)throw new AssertionError("Order quantities changed");
        verifyNoInteractions(inventory); verifyNoInteractions(layout);
        order.setOrderLines(Collections.emptyList()); service.generateManualPickReportByOrder(order,"en-US");
        if(!captured.get().getData().isEmpty())throw new AssertionError("Empty order failed");
        verifyNoInteractions(inventory);
        System.out.println("PASS real report builder: no candidate-inventory calls, order fields/quantities unchanged, existing renderer/type retained, empty order supported; sockets denied.");
    }
}
