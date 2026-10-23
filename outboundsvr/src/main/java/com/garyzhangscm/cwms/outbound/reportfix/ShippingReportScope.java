package com.garyzhangscm.cwms.outbound.reportfix;
import java.util.*;
import com.garyzhangscm.cwms.outbound.model.*;
import com.garyzhangscm.cwms.outbound.service.PickService;
public final class ShippingReportScope implements AutoCloseable {
 private static final ThreadLocal<Long> SHIPMENT=new ThreadLocal<>();private final Long prior;
 public ShippingReportScope(Long id){prior=SHIPMENT.get();if(id==null)SHIPMENT.remove();else SHIPMENT.set(id);}
 public static Long current(){return SHIPMENT.get();}
 public static List<Pick> picks(PickService service,Order order){Long id=current();return service.findByOrder(order).stream().filter(p->id==null||(p.getShipmentLine()!=null&&p.getShipmentLine().getShipment()!=null&&id.equals(p.getShipmentLine().getShipment().getId()))).collect(java.util.stream.Collectors.toList());}
 public void close(){if(prior==null)SHIPMENT.remove();else SHIPMENT.set(prior);}
}
