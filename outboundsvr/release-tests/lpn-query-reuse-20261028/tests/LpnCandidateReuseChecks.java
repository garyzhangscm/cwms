import com.garyzhangscm.cwms.outbound.model.*;
import com.garyzhangscm.cwms.outbound.service.*;
import com.garyzhangscm.cwms.outbound.clients.*;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;
import org.springframework.http.HttpMethod;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.h2.jdbcx.JdbcDataSource;
import java.lang.reflect.*;import java.util.*;import java.util.concurrent.*;import java.util.concurrent.atomic.*;
public class LpnCandidateReuseChecks {
 static int passed;static void check(boolean b,String msg){if(!b)throw new AssertionError(msg);passed++;}
 static void set(Object o,String n,Object v)throws Exception{Field f=o.getClass().getDeclaredField(n);f.setAccessible(true);f.set(o,v);}
 static Inventory row(){Inventory r=new Inventory();r.setId(1L);r.setWarehouseId(7L);r.setLpn("L000000001");r.setQuantity(325L);r.setLocationId(9L);Item i=new Item();i.setId(10L);r.setItem(i);InventoryStatus s=new InventoryStatus();s.setId(1L);r.setInventoryStatus(s);return r;}
 static class Network extends RestTemplateProxy {AtomicInteger candidate=new AtomicInteger();volatile boolean empty,multi,fail;
  @Override public <T>List<T> exchangeList(Class<T> t,String u,HttpMethod m,Object body){boolean pick=u.contains("pickable");if(pick){candidate.incrementAndGet();if(fail)throw new IllegalStateException("read failed");if(empty)return List.of();}return (List<T>)(List<?>)(pick&&multi?List.of(row(),row()):List.of(row()));}
 }
 static String source="http://apigateway:5555/api/inventory/inventories?warehouseId=7&lpn=L000000001";
 static String first="http://apigateway:5555/api/inventory/inventories/pickable?includeDetails=false&itemId=10&inventoryStatusId=1&locationId=9&lpn=L000000001";
 static String second=first+"&lpnLimit=500";
 static RestTemplateProxy proxy;static ManualPickPreparationScope scope;static TransactionTemplate tx;static Network net;
 static List<Inventory> read(String url){return proxy.exchangeList(Inventory.class,url,HttpMethod.GET,null);}
 static void run(Runnable a){tx.execute(s->scope.within(7L,"L000000001",()->{read(source);a.run();return null;}));}
 public static void main(String[] args)throws Exception{
  scope=new ManualPickPreparationScope();set(scope,"enabled",true);net=new Network();CompactCandidateReadAspect compact=new CompactCandidateReadAspect();set(compact,"enabled",true);
  // Real compact aspect must not steal the second read before request-scope reuse.
  AspectJProxyFactory f=new AspectJProxyFactory(net);f.setProxyTargetClass(true);f.addAspect(compact);f.addAspect(scope);java.util.List<org.springframework.aop.Advisor> advisors=new ArrayList<>(Arrays.asList(f.getAdvisors()));org.springframework.core.annotation.AnnotationAwareOrderComparator.sort(advisors);for(var advisor:f.getAdvisors())f.removeAdvisor(advisor);for(var advisor:advisors)f.addAdvisor(advisor);proxy=(RestTemplateProxy)f.getProxy();set(scope,"rest",proxy);
  JdbcDataSource ds=new JdbcDataSource();ds.setURL("jdbc:h2:mem:reuse;DB_CLOSE_DELAY=-1");tx=new TransactionTemplate(new DataSourceTransactionManager(ds));
  run(()->{read(first);check(read(second).get(0).getQuantity()==325,"same candidate quantity");});check(net.candidate.get()==1,"real aspect chain merges availability and allocation query");
  set(compact,"enabled",false);
  run(()->{read(first);read(second);read(second);});check(net.candidate.get()==3,"reuse consumed once; third read rechecks");
  int before=net.candidate.get();run(()->{read(first);read(second);});check(net.candidate.get()==before+1,"new request always reads current inventory");
  for(String changed:List.of(second+"&color=BLUE",second+"&receiptNumber=R1",second+"&clientId=4",second+"&skipLocationIds=9",second.replace("itemId=10","itemId=11"),second.replace("locationId=9","locationId=8"),second.replace("StatusId=1","StatusId=2"),second.replace("L000000001","L000000002"),second+"&lpn=L000000001",second.replace("lpnLimit=500","lpnLimit=1"),second.replace("apigateway","other-host"))){
   int n=net.candidate.get();run(()->{read(first);read(changed);});check(net.candidate.get()==n+2,"different filter cannot reuse: "+changed);
  }
  for(boolean multiple:new boolean[]{false,true}){net.empty=!multiple;net.multi=multiple;int n=net.candidate.get();run(()->{read(first);read(second);});check(net.candidate.get()==n+2,"empty/mixed rows never reused");}net.empty=false;net.multi=false;
  before=net.candidate.get();scope.within(7L,"L000000001",()->{read(source);read(first);read(second);return null;});check(net.candidate.get()==before+2,"nontransactional preview does not cache candidates");
  before=net.candidate.get();try{run(()->{read(first);throw new IllegalStateException("rollback");});}catch(IllegalStateException expected){}read(second);check(net.candidate.get()==before+2,"failed preparation cannot leak snapshot to movement");
  net.fail=true;try{run(()->read(first));throw new AssertionError("failed read accepted");}catch(IllegalStateException expected){passed++;}net.fail=false;
  before=net.candidate.get();ExecutorService pool=Executors.newFixedThreadPool(2);List<Future<?>> futures=new ArrayList<>();for(int i=0;i<2;i++)futures.add(pool.submit(()->run(()->{read(first);read(second);})));for(Future<?> v:futures)v.get();pool.shutdownNow();check(net.candidate.get()==before+2,"concurrent requests never share candidate results");
  before=net.candidate.get();read(second);check(net.candidate.get()==before+1,"background movement has no candidate cache");
  System.out.println("PASS "+passed+" LPN candidate reuse checks; real Spring aspect chain, transaction/thread isolation and guarded fallback");
 }
}
