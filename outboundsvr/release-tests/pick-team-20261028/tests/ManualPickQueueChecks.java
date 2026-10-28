import com.garyzhangscm.cwms.outbound.service.*;
import com.garyzhangscm.cwms.outbound.quantity.*;
import com.garyzhangscm.cwms.outbound.model.*;
import org.h2.jdbcx.JdbcDataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import java.util.*;import java.nio.file.*;import java.lang.reflect.*;import java.util.concurrent.*;

public class ManualPickQueueChecks {
 static int passed; static void check(boolean b,String m){if(!b)throw new AssertionError(m);passed++;}
 static void rejects(Runnable r,String m){try{r.run();throw new AssertionError(m);}catch(AssertionError e){throw e;}catch(RuntimeException expected){passed++;}}
 static void field(Object o,String n,Object v)throws Exception{Field f=o.getClass().getDeclaredField(n);f.setAccessible(true);f.set(o,v);}
 static class Quantities extends QuantityAllocationService {
  Quantities(JdbcDataSource ds,DataSourceTransactionManager tm){super(ds,tm);}
  @Override public void requireQueueOrderAccess(long w,long o,ClientRestriction r){if(w!=1||o!=11)throw new IllegalStateException("scope");}
  @Override public boolean enrolled(long id){return id==11;}
 }
 static class Prepared extends PreprintedManualPickService {
  JdbcTemplate db; int moves;boolean fail;CountDownLatch inMove,release;
  Prepared(JdbcTemplate db){this.db=db;}
  @Override public ManualPickOperation prepare(String id,Long w,Long o,String l,String n,boolean keep,Long q,Long expected,String d){
   List<Map<String,Object>> old=db.queryForList("SELECT * FROM manual_pick_operation WHERE request_id=?",id);
   if(!old.isEmpty()){if(!old.get(0).get("source_lpn").equals(l)||((Number)old.get(0).get("quantity")).longValue()!=q)throw new IllegalStateException("changed retry");}
   else{
    if(db.queryForObject("SELECT COUNT(*) FROM manual_pick_operation WHERE source_lpn=?",Long.class,l)>0)throw new IllegalStateException("duplicate source");
    if(db.update("UPDATE balance SET held=held-?,committed=committed+? WHERE held>=?",q,q,q)!=1)throw new IllegalStateException("insufficient hold");
    db.update("INSERT INTO manual_pick_operation VALUES(?,?,?, ?,FALSE)",id,l,q,91L);
   }
   ManualPickOperation op=new ManualPickOperation();op.requestId=id;op.completed=(Boolean)db.queryForMap("SELECT completed FROM manual_pick_operation WHERE request_id=?",id).get("completed");return op;
  }
  @Override public ManualPickOperation finish(String id){
   if(inMove!=null){inMove.countDown();try{release.await(4,TimeUnit.SECONDS);}catch(Exception e){throw new RuntimeException(e);}}
   if(fail)throw new IllegalStateException("simulated uncertain movement");
   if(db.update("UPDATE manual_pick_operation SET completed=TRUE WHERE request_id=? AND completed=FALSE",id)==1)moves++;
   ManualPickOperation op=new ManualPickOperation();op.completed=true;return op;
  }
 }
 static ManualPickQueueService service(JdbcDataSource ds,DataSourceTransactionManager tm,Prepared p,Quantities q)throws Exception{
  ManualPickQueueService s=new ManualPickQueueService(ds,tm);field(s,"enabled",true);field(s,"picks",p);field(s,"quantities",q);field(s,"users",new UserService());return s;
 }
 static void login(){SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("operator",null,List.of()));}
 static Map<String,Object> submit(ManualPickQueueService s,String id,String l,long q){login();return s.accept(1,11,null,id,l,"",false,q,q,"@SHIP_STAGE");}
 public static void main(String[] args)throws Exception{
  JdbcDataSource ds=new JdbcDataSource();ds.setURL("jdbc:h2:mem:queue;MODE=MySQL;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=5000");
  JdbcTemplate db=new JdbcTemplate(ds);DataSourceTransactionManager tm=new DataSourceTransactionManager(ds);
  String schema=Files.readString(Path.of(args[0]));String queue=schema.substring(schema.indexOf("CREATE TABLE"),schema.indexOf("CREATE TABLE IF NOT EXISTS cwms.quantity"));db.execute(queue.replace("cwms.",""));
  db.execute("CREATE TABLE manual_pick_operation(request_id VARCHAR(36) PRIMARY KEY,source_lpn VARCHAR(12),quantity BIGINT,pick_id BIGINT,completed BOOLEAN)");
  db.execute("CREATE TABLE balance(held BIGINT,committed BIGINT)");db.update("INSERT INTO balance VALUES(500,0)");
  db.execute("CREATE TABLE quantity_allocation_mutex(warehouse_id BIGINT PRIMARY KEY)");
  Quantities quantities=new Quantities(ds,tm);Field enabled=QuantityAllocationService.class.getDeclaredField("enabled");enabled.setAccessible(true);enabled.set(quantities,true);
  Prepared p=new Prepared(db);ManualPickQueueService s=service(ds,tm,p,quantities);
  SecurityContextHolder.clearContext();
  rejects(()->s.accept(1,11,null,UUID.randomUUID().toString(),"LPN-A","",false,325L,325L,"@SHIP_STAGE"),"missing identity rejected before quantity changes");
  check(db.queryForObject("SELECT held FROM balance",Long.class)==500,"missing identity keeps ledger unchanged");
  javax.servlet.http.HttpServletRequest request=(javax.servlet.http.HttpServletRequest)Proxy.newProxyInstance(ManualPickQueueChecks.class.getClassLoader(),new Class[]{javax.servlet.http.HttpServletRequest.class},(proxy,method,values)->method.getName().equals("getHeader")&&"username".equals(values[0])?"gateway-operator":null);
  UserService gatewayUser=new UserService();field(gatewayUser,"request",request);field(s,"users",gatewayUser);
  org.springframework.web.context.request.RequestContextHolder.setRequestAttributes(new org.springframework.web.context.request.ServletRequestAttributes(request));
  String a=UUID.randomUUID().toString();check(s.accept(1,11,null,a,"LPN-A","",false,325L,325L,"@SHIP_STAGE").get("state").equals("QUEUED"),"accepted is queued, not completed");
  check(db.queryForObject("SELECT username FROM manual_pick_queue WHERE request_id=?",String.class,a).equals("gateway-operator"),"gateway identity stored with empty SecurityContext");
  check(s.list(1,11,null).stream().anyMatch(row -> "gateway-operator".equals(row.get("submittedBy"))),"list returns persisted submitter");
  org.springframework.web.context.request.RequestContextHolder.resetRequestAttributes();
  check(db.queryForObject("SELECT held FROM balance",Long.class)==175&&p.moves==0,"admission reserves quantity before background movement");
  submit(s,a,"LPN-A",325);check(db.queryForObject("SELECT COUNT(*) FROM manual_pick_operation",Long.class)==1,"lost acknowledgement reuses saved UUID");
  rejects(()->submit(s,a,"LPN-B",325),"same UUID payload mutation rejected");
  rejects(()->submit(s,UUID.randomUUID().toString(),"LPN-A",100),"duplicate LPN rejected");
  rejects(()->submit(s,UUID.randomUUID().toString(),"LPN-B",176),"in-flight quantity cannot be promised twice");
  check(db.queryForObject("SELECT held FROM balance",Long.class)==175,"rejected admission keeps ledger intact");
  String b=UUID.randomUUID().toString();submit(s,b,"LPN-B",175);
  check(s.list(1,11,null).size()==2,"multiple pallets accepted before any movement");
  p.fail=true;s.drain();check(s.list(1,11,null).stream().anyMatch(row->row.get("state").equals("NEEDS_REVIEW")),"error persists for user recovery");
  s.drain();check(p.moves==0,"failed order pauses later tasks");
  rejects(()->submit(s,UUID.randomUUID().toString(),"LPN-C",1),"failed order blocks further admissions");
  String failed=(String)db.queryForMap("SELECT request_id FROM manual_pick_queue WHERE state='NEEDS_REVIEW'").get("request_id");
  p.fail=false;s.retry(1,11,null,failed);
  ManualPickQueueService restarted=service(ds,tm,p,quantities);restarted.drain();restarted.drain();restarted.drain();
  check(p.moves==2&&db.queryForObject("SELECT COUNT(*) FROM manual_pick_queue WHERE state='COMPLETED'",Long.class)==2,"service restart drains saved work exactly once");
  check(db.queryForObject("SELECT committed FROM balance",Long.class)==500,"retry never consumes reservation again");
  rejects(()->restarted.list(2,11,null),"cross-warehouse task access rejected");
  rejects(()->restarted.retry(1,12,null,a),"cross-order retry rejected");
  // Two operators racing for the same remaining quantity.
  db.update("UPDATE balance SET held=100,committed=0");ExecutorService workers=Executors.newFixedThreadPool(2);CountDownLatch start=new CountDownLatch(1);java.util.concurrent.atomic.AtomicInteger won=new java.util.concurrent.atomic.AtomicInteger();List<Future<?>> futures=new ArrayList<>();
  for(String l:List.of("LPN-C","LPN-D"))futures.add(workers.submit(()->{try{start.await();submit(s,UUID.randomUUID().toString(),l,70);won.incrementAndGet();}catch(RuntimeException expected){}catch(Exception e){throw new RuntimeException(e);}}));
  start.countDown();for(Future<?> f:futures)f.get();workers.shutdownNow();check(won.get()==1&&db.queryForObject("SELECT held FROM balance",Long.class)==30,"concurrent operators cannot overpick");
  // Two workers cannot both perform the same task while the first owns the queue row.
  p.inMove=new CountDownLatch(1);p.release=new CountDownLatch(1);int before=p.moves;ExecutorService drains=Executors.newFixedThreadPool(2);
  Future<?> first=drains.submit(s::drain);check(p.inMove.await(3,TimeUnit.SECONDS),"first worker holds task");Future<?> second=drains.submit(restarted::drain);second.get(3,TimeUnit.SECONDS);p.release.countDown();first.get(3,TimeUnit.SECONDS);drains.shutdownNow();check(p.moves==before+1,"SKIP LOCKED prevents duplicate worker execution");
  System.out.println("PASS "+passed+" durable queue checks: concurrent admission, restart, recovery, duplicate protection and worker exclusion");
 }
}
