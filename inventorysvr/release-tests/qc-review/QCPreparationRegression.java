import com.garyzhangscm.cwms.inventory.model.*;
import com.garyzhangscm.cwms.inventory.service.*;
import jakarta.persistence.*;
import java.lang.reflect.*;
import java.util.*;
public class QCPreparationRegression extends QCFailureReviewServiceRegression {
 static class Store extends FakeRequests {
  QCInspectionRequest row; String rf;
  @Override public QCInspectionRequest findById(Long id){return row;}
  @Override public List<QCInspectionRequest> savePendingQCInspectionRequest(Long w,List<QCInspectionRequest> rs,String code){rf=code;return super.savePendingQCInspectionRequest(w,rs,code);}
 }
 static QCReviewPreparationService prep(Store store,QCInspectionRequest row)throws Exception {
  store.row=row;QCReviewPreparationService s=new QCReviewPreparationService(store);
  EntityManager em=(EntityManager)Proxy.newProxyInstance(EntityManager.class.getClassLoader(),new Class[]{EntityManager.class},(p,m,a)->{
   if(m.getName().equals("find")){if(a[2]!=LockModeType.PESSIMISTIC_WRITE)throw new AssertionError();return row;}
   if(m.getName().equals("flush")){for(QCInspectionRequestItem g:row.getQcInspectionRequestItems())for(QCInspectionRequestItemOption o:g.getQcInspectionRequestItemOptions())if(o.getId()==null)o.setId(999L);return null;}
   throw new UnsupportedOperationException(m.getName());});
  Field f=QCReviewPreparationService.class.getDeclaredField("entityManager");f.setAccessible(true);f.set(s,em);return s;
 }
 static QCInspectionRequest legacy(){QCInspectionRequest r=fixture(1);r.setType(QCInspectionRequestType.INBOUND_BY_INVENTORY);r.setQcInspectionResult(QCInspectionResult.PASS);Inventory i=new Inventory();i.setId(7L);i.setWarehouseId(1L);i.setInboundQCRequired(true);r.setInventory(i);return r;}
 public static void main(String[] args)throws Exception {
  Store st=new Store();prep(st,fixture(1)).prepare(1L,1L);if(st.saved!=0||st.completed!=0)throw new AssertionError("Normal prepare must be read only");
  QCInspectionRequest r=legacy();Store ls=new Store();prep(ls,r).prepare(1L,1L);if(r.getQcInspectionResult()!=QCInspectionResult.PENDING||ls.saved!=1||ls.completed!=0)throw new AssertionError("Legacy recovery");
  for(String guard:List.of("false","zero","notes","fail","warehouse","done")){
   QCInspectionRequest bad=legacy();switch(guard){case "false":option(bad).setBooleanValue(false);break;case "zero":option(bad).setDoubleValue(0D);break;case "notes":option(bad).setExceptionNotes("recorded");break;case "fail":bad.setQcInspectionResult(QCInspectionResult.FAIL);break;case "warehouse":bad.setWarehouseId(2L);break;case "done":option(bad).setQcInspectionResult(QCInspectionResult.PASS);break;}
   Store bs=new Store();QCReviewPreparationService ps=prep(bs,bad);rejected(()->ps.prepare(1L,1L));if(bs.saved+bs.completed!=0)throw new AssertionError(guard);
  }
  QCInspectionRequest empty=fixture(1);QCInspectionRequestItem g=empty.getQcInspectionRequestItems().get(0);g.getQcInspectionRequestItemOptions().clear();QCRuleItem active=new QCRuleItem();active.setId(88L);active.setEnabled(true);g.getQcRule().setQcRuleItems(List.of(active));Store es=new Store();prep(es,empty).prepare(1L,1L);if(g.getQcInspectionRequestItemOptions().size()!=1||g.getQcInspectionRequestItemOptions().get(0).getId()==null||es.completed!=0)throw new AssertionError("Must persist generated check IDs without completing QC");
  Store done=new Store();QCInspectionRequest input=fixture(9);option(input).setBooleanValue(false);input.setQcQuantity(7L);QCInspectionRequest stored=fixture(9);stored.setWorkOrderQCSampleId(99L);service(done,Map.of(9L,stored)).submit(1L,List.of(input),false,"RF-TEST");if(!"RF-TEST".equals(done.rf)||stored.getQcQuantity()!=7L)throw new AssertionError("rfCode and sample quantity");
  System.out.println("PASS: preparation lock, legacy guards, real IDs, no QC side effects, rfCode and sample quantity");
 }
}
