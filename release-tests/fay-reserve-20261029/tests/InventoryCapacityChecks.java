import com.garyzhangscm.cwms.inventory.controller.*;
import com.garyzhangscm.cwms.inventory.model.*;
import java.util.*;

public class InventoryCapacityChecks {
 static int passed;
 static void check(boolean value,String name){if(!value)throw new AssertionError(name);passed++;}
 static void rejects(Runnable action,String name){try{action.run();throw new AssertionError(name);}catch(AssertionError e){throw e;}catch(RuntimeException expected){passed++;}}
 static Inventory row(long item,long qty){Inventory r=new Inventory();r.setWarehouseId(1L);Item i=new Item();i.setId(item);r.setItem(i);InventoryStatus s=new InventoryStatus();s.setId(1L);r.setInventoryStatus(s);r.setQuantity(qty);return r;}
 static long reference(List<Inventory> rows){long sum=0;for(Inventory r:rows)if(Objects.equals(r.getWarehouseId(),1L)&&r.getItem()!=null&&Objects.equals(r.getItem().getId(),9L)&&r.getInventoryStatus()!=null&&Objects.equals(r.getInventoryStatus().getId(),1L)&&(r.getClientId()==null?0:r.getClientId())==0&&r.getPickId()==null&&r.getAllocatedByPickId()==null&&!Boolean.TRUE.equals(r.getVirtual()))sum=Math.addExact(sum,Math.max(0,r.getQuantity()));return sum;}
 static class Delegate extends InventoryController {
  List<Inventory> rows;boolean called;
  @Override public List<Inventory> findPickableInventories(Long i,Long s,String l,String c,String size,String style,String a1,String a2,String a3,String a4,String a5,String receipt,Long location,String skip,Boolean details,int limit){
   check(i==9&&s==1&&l.isEmpty()&&c.isEmpty()&&size.isEmpty()&&style.isEmpty()&&a1.isEmpty()&&a2.isEmpty()&&a3.isEmpty()&&a4.isEmpty()&&a5.isEmpty()&&receipt.isEmpty()&&location==null&&skip.isEmpty()&&!details&&limit==100000,"exact existing candidate selector arguments");called=true;return rows;
  }
 }
 public static void main(String[] args)throws Exception {
  Delegate d=new Delegate();d.rows=List.of(row(9,40),row(9,60));Map<String,Object> result=new QuantityCapacityController(d).capacity(1L,9L,1L,0L,null);check(d.called,"reuse current selector, including QC/location/lock rules");check(result.get("physicalQuantity").equals(100L),"sum physical quantity");
  check(result.keySet().equals(Set.of("schema","warehouseId","itemId","inventoryStatusId","clientId","physicalQuantity","candidateCount","eligibleCount")),"bounded scalar-only wire payload");
  Random random=new Random(7135);
  for(int round=0;round<200;round++){
   List<Inventory> rows=new ArrayList<>();for(int j=0;j<40;j++){Inventory r=row(random.nextBoolean()?9:10,random.nextInt(100)-10);if(random.nextBoolean())r.setWarehouseId(2L);if(random.nextBoolean())r.setClientId(4L);if(random.nextInt(5)==0)r.setPickId(3L);if(random.nextInt(5)==0)r.setAllocatedByPickId(4L);if(random.nextInt(5)==0)r.setVirtual(true);if(random.nextInt(8)==0)r.setItem(null);if(random.nextInt(8)==0)r.setInventoryStatus(null);rows.add(r);}
   check(QuantityCapacityController.summarize(1,9,1,0,rows).get("physicalQuantity").equals(reference(rows)),"same legacy scope/exclusion arithmetic");
  }
  check(QuantityCapacityController.summarize(1,9,1,0,List.of()).get("physicalQuantity").equals(0L),"empty stock");
  rejects(()->QuantityCapacityController.summarize(1,9,1,0,Collections.nCopies(100000,row(9,1))),"reject truncated candidate set");
  rejects(()->QuantityCapacityController.summarize(1,9,1,0,List.of(row(9,Long.MAX_VALUE),row(9,1))),"quantity overflow fails closed");
  Inventory missing=row(9,1);missing.setQuantity(null);rejects(()->QuantityCapacityController.summarize(1,9,1,0,List.of(missing)),"missing quantity fails closed");
  rejects(()->QuantityCapacityController.validate(0L,9L,1L,0L,null),"invalid warehouse");
  ClientRestriction restricted=new ClientRestriction(true,false,false,"4, 5");
  rejects(()->QuantityCapacityController.validate(1L,9L,1L,0L,restricted),"null-owner access protected");rejects(()->QuantityCapacityController.validate(1L,9L,1L,6L,restricted),"other client protected");
  QuantityCapacityController.validate(1L,9L,1L,4L,restricted);check(true,"allowed client");
  System.out.println("PASS "+passed+" inventory summary checks; existing selector delegation and legacy-equivalent arithmetic");
 }
}
