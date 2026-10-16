import com.garyzhangscm.cwms.outbound.allocation.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.*;
import org.springframework.transaction.support.TransactionTemplate;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
public class AllocationTaskCheck {
 static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
 static class Gateway implements AllocationOrderGateway {
  final JdbcTemplate db;int executions=0;String failure="";AllocationTaskStore store;
  Gateway(JdbcTemplate db){this.db=db;}
  public String lockAndGetState(long w,long id){List<String> rows=db.queryForList("select state from fake_order where id=? and warehouse_id=? for update",String.class,id,w);if(rows.isEmpty())throw new AllocationValidationFailure("ORDER_NOT_FOUND","Wrong warehouse");return rows.get(0);}
  public void validate(long w,long id){if(!db.queryForObject("select valid from fake_order where id=?",Boolean.class,id))throw new AllocationValidationFailure("INVALID_STRATEGY","Set a strategy for line 2.");}
  public void markAllocating(long w,long id){db.update("update fake_order set state='ALLOCATING' where id=?",id);}
  public void execute(long id){executions++;check("RUNNING".equals(store.active(1,id).state),"persist running before work");if(failure.equals("before"))throw new IllegalStateException("synthetic failure");db.update("update fake_order set planned=true where id=?",id);if(failure.equals("partial"))throw new IllegalStateException("synthetic partial failure");}
  public boolean hasPlannedWork(long id){return db.queryForObject("select planned from fake_order where id=?",Boolean.class,id);}
  public void finish(long w,long id,String original,boolean success){db.update("update fake_order set state=? where id=? and state='ALLOCATING'",success||hasPlannedWork(id)?"INPROCESS":original,id);}
 }
 public static void main(String[] args)throws Exception {
  DriverManagerDataSource ds=new DriverManagerDataSource("jdbc:h2:mem:allocation;MODE=MySQL;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000","sa","");JdbcTemplate db=new JdbcTemplate(ds);
  for(String sql:Files.readString(Path.of(args[0])).split(";"))if(!sql.isBlank())db.execute(sql);
  db.execute("create table fake_order(id bigint primary key,warehouse_id bigint,state varchar(24),valid boolean,planned boolean)");
  for(int i=1;i<=12;i++)db.update("insert into fake_order values(?,1,'OPEN',true,false)",i);
  AllocationTaskStore store=new AllocationTaskStore(db);Gateway gateway=new Gateway(db);gateway.store=store;
  List<Runnable> queue=Collections.synchronizedList(new ArrayList<>());TransactionTemplate tx=new TransactionTemplate(new DataSourceTransactionManager(ds));
  AllocationTaskService svc=new AllocationTaskService(store,gateway,tx,queue::add,Clock.systemUTC());
  db.update("update fake_order set valid=false where id=1");AllocationTask invalid=svc.submit(1,1,"invalid001",true);check(invalid.state.equals("FAILED")&&invalid.errorCode.equals("INVALID_STRATEGY")&&queue.isEmpty(),"missing strategy rejects before enqueue");check(gateway.lockAndGetState(1,1).equals("OPEN"),"validation does not mark allocating");
  AllocationTask queued=svc.submit(1,2,"success001",true);check(queued.state.equals("QUEUED"),"submitted state");check(svc.submit(1,2,"success001",true).id.equals(queued.id),"same request dedupe");check(svc.submit(1,2,"different001",true).id.equals(queued.id)&&queue.size()==1,"active task dedupe");queue.remove(0).run();check(svc.get(1,queued.id).state.equals("SUCCEEDED")&&gateway.lockAndGetState(1,2).equals("INPROCESS"),"success final state");check(svc.submit(1,2,"success001",true).id.equals(queued.id)&&queue.isEmpty(),"terminal request no repeat");
  gateway.failure="before";AllocationTask before=svc.submit(1,3,"failure001",true);queue.remove(0).run();check(svc.get(1,before.id).state.equals("FAILED")&&!svc.get(1,before.id).requiresReview&&gateway.lockAndGetState(1,3).equals("OPEN"),"failure before effects restored and persisted");
  gateway.failure="partial";AllocationTask partial=svc.submit(1,4,"partial001",true);queue.remove(0).run();check(svc.get(1,partial.id).requiresReview&&gateway.lockAndGetState(1,4).equals("INPROCESS"),"partial failure requires review and clears allocating");check(svc.submit(1,4,"partial002",true).id.equals(partial.id)&&queue.isEmpty(),"partial work cannot blindly repeat");
  gateway.failure="";AllocationTask changed=svc.submit(1,5,"changed001",true);db.update("update fake_order set valid=false where id=5");int n=gateway.executions;queue.remove(0).run();check(gateway.executions==n&&svc.get(1,changed.id).state.equals("FAILED"),"worker revalidates strategy");
  AllocationTaskService rejected=new AllocationTaskService(store,gateway,tx,r->{throw new RejectedExecutionException();},Clock.systemUTC());AllocationTask full=rejected.submit(1,6,"queuefull01",true);check(full.state.equals("FAILED")&&full.errorCode.equals("QUEUE_FULL")&&gateway.lockAndGetState(1,6).equals("OPEN"),"queue full does not strand allocating");rejected.close();
  AllocationTask stale=svc.submit(1,7,"stale0001",true);db.update("update outbound_allocation_task set heartbeat_at=? where id=?",java.sql.Timestamp.from(Instant.now().minusSeconds(600)),stale.id);svc.close();AllocationTaskService restarted=new AllocationTaskService(store,gateway,tx,queue::add,Clock.systemUTC());restarted.recoverStaleTasks();check(restarted.get(1,stale.id).state.equals("REVIEW_REQUIRED")&&gateway.lockAndGetState(1,7).equals("OPEN"),"restart recovers stale state without retry");n=gateway.executions;queue.remove(0).run();check(gateway.executions==n,"stale queued work cannot execute");
  boolean scoped=false;try{restarted.get(2,queued.id);}catch(AllocationValidationFailure expected){scoped=true;}check(scoped,"task tenant isolation");
  AllocationTask cancelled=restarted.submit(1,8,"cancelled01",true);db.update("update fake_order set state='CANCELLED' where id=8");queue.remove(0).run();check(gateway.lockAndGetState(1,8).equals("CANCELLED"),"preserve newer business state");
  ExecutorService threads=Executors.newFixedThreadPool(2);CountDownLatch ready=new CountDownLatch(1);Future<AllocationTask> a=threads.submit(()->{ready.await();return restarted.submit(1,9,"concurrent1",true);});Future<AllocationTask> b=threads.submit(()->{ready.await();return restarted.submit(1,9,"concurrent2",true);});ready.countDown();check(a.get().id.equals(b.get().id)&&queue.size()==1,"concurrent requests yield one task");threads.shutdownNow();queue.remove(0).run();
  check(store.latest(1,Arrays.asList(1L,2L,9L)).size()==3,"batch latest task lookup");
  restarted.close();System.out.println("PASS: strategy, queued/running/success, failures, partial safety, dedupe, concurrency, rejection, restart, warehouse scope and latest lookup");
 }
}
