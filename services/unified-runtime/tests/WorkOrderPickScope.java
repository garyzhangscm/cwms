import com.garyzhangscm.cwms.outbound.model.*;
import com.garyzhangscm.cwms.outbound.service.PickService;
import java.util.*;

/** Exercise the actual scoped method without a repository, HTTP calls or business writes. */
public class WorkOrderPickScope {
 static class Capture extends PickService {
  int calls;Long warehouse;String lines;
  @Override public List<Pick> findAll(Long warehouseId,Long clientId,String number,Long orderId,String orderIds,String orderNumber,
    Long shipmentId,Long waveId,Long listId,Long cartonizationId,String ids,Long itemId,Long sourceLocationId,Long destinationLocationId,
    Long workOrderLineId,String workOrderLineIds,Long shortAllocationId,Long inventoryStatusId,String shipmentNumber,String workOrderNumber,
    String waveNumber,String cartonizationNumber,String itemNumber,String sourceLocationName,String destinationLocationName,Long trailerAppointmentId,
    Boolean openPickOnly,Boolean includeCompletedPick,Boolean includeCancelledPick,ClientRestriction restriction) {
   calls++;warehouse=warehouseId;lines=workOrderLineIds;return Arrays.asList(new Pick());
  }
 }
 static WorkOrderLine line(Long id){WorkOrderLine l=new WorkOrderLine();l.setId(id);return l;}
 public static void main(String[] args) {
  Capture service=new Capture();WorkOrder order=new WorkOrder();order.setWarehouseId(77L);int cases=0;
  List<List<WorkOrderLine>> emptyCases=Arrays.asList(null,Collections.emptyList(),Arrays.asList((WorkOrderLine)null),Arrays.asList(line(null)),Arrays.asList(line(0L)),Arrays.asList(line(-1L)));
  for(List<WorkOrderLine> lines:emptyCases){order.setWorkOrderLines(lines);if(!service.findByWorkOrder(order).isEmpty()||service.calls!=0)throw new AssertionError("An empty order queried warehouse picks");cases++;}
  order.setWorkOrderLines(Arrays.asList(line(11L)));if(service.findByWorkOrder(order).size()!=1||service.calls!=1||!"11".equals(service.lines)||!Long.valueOf(77).equals(service.warehouse))throw new AssertionError("Valid line scope changed");cases++;
  order.setWorkOrderLines(Arrays.asList(null,line(null),line(0L),line(11L),line(22L)));service.findByWorkOrder(order);if(service.calls!=2||!"11,22".equals(service.lines))throw new AssertionError("Mixed order scope is not bounded");cases++;
  System.out.println("RESULT "+cases+" work-order pick scope cases passed; no remote or repository calls.");
 }
}
