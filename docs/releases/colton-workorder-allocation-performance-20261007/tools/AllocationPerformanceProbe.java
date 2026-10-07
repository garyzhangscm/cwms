import com.garyzhangscm.cwms.workorder.service.*;
import com.garyzhangscm.cwms.workorder.model.*;
import com.garyzhangscm.cwms.workorder.exception.WorkOrderException;
import java.lang.reflect.*;
public class AllocationPerformanceProbe {
 public static class Lines extends WorkOrderLineService {
  int reads,saves;WorkOrderLine line;
  public WorkOrderLine findById(Long id,boolean details){if(details)throw new AssertionError("Heavy lookup");reads++;return line;}
  public WorkOrderLine findById(Long id){throw new AssertionError("Heavy lookup");}
  public WorkOrderLine save(WorkOrderLine line,boolean details){if(details)throw new AssertionError("Heavy save");saves++;return line;}
  public WorkOrderLine save(WorkOrderLine line){throw new AssertionError("Heavy save");}
 }
 static Pick pick(long item,long qty){Pick p=new Pick();p.setWorkOrderLineId(11901L);p.setItemId(item);p.setQuantity(qty);p.setNumber("TEST");return p;}
 public static void main(String[] args)throws Exception{
  WorkOrderService svc=new WorkOrderService();Lines lines=new Lines();Field f=WorkOrderService.class.getDeclaredField("workOrderLineService");f.setAccessible(true);f.set(svc,lines);
  WorkOrder order=new WorkOrder();order.setStatus(WorkOrderStatus.INPROCESS);order.setId(2586L);
  WorkOrderLine line=new WorkOrderLine();line.setId(11901L);line.setItemId(6402L);line.setOpenQuantity(50000L);line.setInprocessQuantity(10L);line.setNumber("0");lines.line=line;
  AllocationResult r=new AllocationResult();for(int i=0;i<45;i++)r.getPicks().add(pick(6402,1000));r.getPicks().add(pick(6402,5000));
  Method m=WorkOrderService.class.getDeclaredMethod("processAllocateResult",WorkOrder.class,AllocationResult.class);m.setAccessible(true);m.invoke(svc,order,r);
  if(lines.reads!=1||lines.saves!=1||line.getOpenQuantity()!=0||line.getInprocessQuantity()!=50010)throw new AssertionError("46 Pick aggregation failed");
  r=new AllocationResult();r.getPicks().add(pick(99,1));try{m.invoke(svc,order,r);throw new AssertionError("unknown material allowed");}catch(InvocationTargetException ex){if(!(ex.getCause() instanceof WorkOrderException))throw ex;}
  if(lines.saves!=1)throw new AssertionError("invalid result saved");
  System.out.println("PASS actual packaged JAR: 46 Picks => one lean read, one lean save, correct 50000 quantity; unknown material blocked. No network or database calls.");
 }
}
