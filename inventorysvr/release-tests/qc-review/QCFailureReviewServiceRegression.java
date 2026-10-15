import com.garyzhangscm.cwms.inventory.model.*;
import com.garyzhangscm.cwms.inventory.service.*;
import com.fasterxml.jackson.databind.*;
import jakarta.persistence.*;
import java.lang.reflect.*;
import java.util.*;

public class QCFailureReviewServiceRegression {
 static final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
 static QCInspectionRequest fixture(long id) {
  return mapper.convertValue(Map.of("id",id,"number","QC-TEST-"+id,"warehouseId",1,"type","BY_ITEM","inventories",List.of(),"qcInspectionResult","PENDING",
   "qcInspectionRequestItems",List.of(Map.of("id",id,"qcRule",Map.of("id",1,"name","Packaging"),"qcInspectionResult","PENDING",
   "qcInspectionRequestItemOptions",List.of(Map.of("id",id*10,"qcInspectionResult","PENDING","qcRuleItem",Map.of("id",1,"checkPoint","Packaging problem?","enabled",true,"qcRuleItemType","YESNO","qcRuleItemComparator","EQUAL","expectedValue","false")))))),QCInspectionRequest.class);
 }
 static QCInspectionRequestItemOption option(QCInspectionRequest r){return r.getQcInspectionRequestItems().get(0).getQcInspectionRequestItemOptions().get(0);}
 static class FakeRequests extends QCInspectionRequestService {
  int saved=0, completed=0;
  @Override public QCInspectionRequest save(QCInspectionRequest r){saved++;return r;}
  @Override public List<QCInspectionRequest> savePendingQCInspectionRequest(Long w,List<QCInspectionRequest> rs,String rf){completed+=rs.size();return new ArrayList<>(rs);}
 }
 static QCFailureReviewService service(FakeRequests requests, Map<Long,QCInspectionRequest> db)throws Exception {
  QCFailureReviewService s=new QCFailureReviewService(requests);
  EntityManager em=(EntityManager)Proxy.newProxyInstance(EntityManager.class.getClassLoader(),new Class[]{EntityManager.class},(p,m,a)->{
   if(m.getName().equals("find")){if(a[2]!=LockModeType.PESSIMISTIC_WRITE)throw new AssertionError("Lock required");return db.get(a[1]);}
   throw new UnsupportedOperationException(m.getName());
  });
  Field f=QCFailureReviewService.class.getDeclaredField("entityManager");f.setAccessible(true);f.set(s,em);return s;
 }
 static void rejected(Runnable r){try{r.run();throw new AssertionError("Expected rejection");}catch(com.garyzhangscm.cwms.inventory.exception.QCException good){}}
 public static void main(String[] args)throws Exception {
  FakeRequests calls=new FakeRequests();QCInspectionRequest stored=fixture(1),input=fixture(1);
  QCFailureReviewService s=service(calls,Map.of(1L,stored));
  option(input).setBooleanValue(true);option(input).setQcInspectionResult(QCInspectionResult.PASS);option(input).getQcRuleItem().setExpectedValue("true");
  rejected(()->s.submit(1L,List.of(input),true));if(calls.saved+calls.completed!=0)throw new AssertionError();
  option(input).setExceptionNotes(" damaged wrap ");QCFailureReviewService s2=service(calls,Map.of(1L,fixture(1)));rejected(()->s2.submit(1L,List.of(input),false));if(calls.completed!=0)throw new AssertionError();
  QCFailureReviewService s3=service(calls,Map.of(1L,fixture(1)));List<QCInspectionRequest> result=s3.submit(1L,List.of(input),true);
  if(result.get(0).getQcInspectionResult()!=QCInspectionResult.FAIL || !option(result.get(0)).getExceptionNotes().equals("damaged wrap") || calls.completed!=1)throw new AssertionError();
  FakeRequests draftCalls=new FakeRequests();QCFailureReviewService draft=service(draftCalls,Map.of(2L,fixture(2)));
  draft.submit(1L,List.of(fixture(2)),false);if(draftCalls.saved!=1 || draftCalls.completed!=0)throw new AssertionError("Pending must not change QC quantity");
  FakeRequests invalid=new FakeRequests();QCFailureReviewService boundary=service(invalid,Map.of(3L,fixture(3)));
  rejected(()->boundary.submit(2L,List.of(fixture(3)),true));
  rejected(()->boundary.submit(1L,List.of(fixture(3),fixture(3)),true));
  QCInspectionRequest unknown=fixture(3);option(unknown).setId(999L);rejected(()->boundary.submit(1L,List.of(unknown),true));
  if(invalid.saved+invalid.completed!=0)throw new AssertionError();
  QCInspectionRequest pass=fixture(4);option(pass).setBooleanValue(false);FakeRequests passCalls=new FakeRequests();
  if(service(passCalls,Map.of(4L,fixture(4))).submit(1L,List.of(pass),false).get(0).getQcInspectionResult()!=QCInspectionResult.PASS)throw new AssertionError();
  if(!QCInspectionRequestItemOption.class.getDeclaredField("exceptionNotes").getAnnotation(Column.class).name().equals("exception_notes"))throw new AssertionError();
  System.out.println("PASS: online-baseline service, locked reads, authoritative rules, failure acknowledgement, notes, pending without quantity updates, warehouse/duplicate/check boundaries, passing No and column mapping");
 }
}
