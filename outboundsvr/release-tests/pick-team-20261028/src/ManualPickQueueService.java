package com.garyzhangscm.cwms.outbound.service;

import com.garyzhangscm.cwms.outbound.model.*;
import com.garyzhangscm.cwms.outbound.quantity.QuantityAllocationService;
import com.garyzhangscm.cwms.outbound.exception.OrderOperationException;
import com.garyzhangscm.cwms.outbound.usercontext.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.stereotype.Service;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.*;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import javax.sql.DataSource;
import java.util.*;

/** Durable continuation of an already prepared Pick. No tokens are stored in the queue. */
@Service
public class ManualPickQueueService {
 private final java.util.concurrent.ScheduledExecutorService worker=java.util.concurrent.Executors.newSingleThreadScheduledExecutor(r->{Thread t=new Thread(r,"manual-pick-queue");t.setDaemon(true);return t;});
 @javax.annotation.PostConstruct public void startWorker(){
  if(enabled)worker.scheduleWithFixedDelay(()->{try{drain();}catch(Exception e){org.slf4j.LoggerFactory.getLogger(getClass()).warn("Manual Pick queue polling failed: {}",e.getClass().getSimpleName());}},150,150,java.util.concurrent.TimeUnit.MILLISECONDS);
 }
 @javax.annotation.PreDestroy public void stopWorker(){
  worker.shutdown();try{if(!worker.awaitTermination(20,java.util.concurrent.TimeUnit.SECONDS))worker.shutdownNow();}catch(InterruptedException e){worker.shutdownNow();Thread.currentThread().interrupt();}
 }
 private static final ThreadLocal<String> workerActor=new ThreadLocal<>();
 public static String workerUsername(){return workerActor.get();}
 @Value("${manual.pick.queue-enabled:false}") private boolean enabled;
 @Autowired private PreprintedManualPickService picks;
 @Autowired private UserService users;
 @Autowired private QuantityAllocationService quantities;
 private final JdbcTemplate db;
 private final TransactionTemplate tx;
 private final TransactionTemplate isolated;
 public ManualPickQueueService(DataSource source,PlatformTransactionManager manager){
  db=new JdbcTemplate(source);tx=new TransactionTemplate(manager);isolated=new TransactionTemplate(manager);
  isolated.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
 }
 public Map<String,Object> capabilities(){return Map.of("schema","manual-pick-queue-v1","enabled",enabled);}
 private void access(long warehouse,long order,ClientRestriction restriction){
  if(!enabled)throw bad("Continuous picking is not enabled on this server");
  quantities.requireQueueOrderAccess(warehouse,order,restriction); // validate scope without loading the entire order graph
 }
 public Map<String,Object> accept(long warehouse,long order,ClientRestriction restriction,String requestId,String sourceLpn,String newLpn,boolean keepOriginal,Long quantity,Long expectedQuantity,String destination){
  access(warehouse,order,restriction);
  // Only reservation orders use this queue; classic workflows stay synchronous.
  if(!quantities.enrolled(order))throw bad("Reserve quantity in Allocate before using continuous picking");
  UUID.fromString(requestId);
  String username=users.getCurrentUserName();
  if(username==null || username.trim().isEmpty())throw bad("Unable to identify the signed-in user. Sign in again before picking");
  UserContext context=UserContextHolder.getContext();
  return quantities.locked(warehouse,()->{
   List<Map<String,Object>> existing=db.queryForList("SELECT * FROM manual_pick_queue WHERE request_id=?",requestId);
   if(existing.isEmpty()){
    if(db.queryForObject("SELECT COUNT(*) FROM manual_pick_queue WHERE warehouse_id=? AND order_id=? AND state='NEEDS_REVIEW'",Long.class,warehouse,order)>0)
     throw bad("Resolve the failed Pick for this order before scanning another pallet");
    if(db.queryForObject("SELECT COUNT(*) FROM manual_pick_queue WHERE warehouse_id=? AND order_id=? AND state='QUEUED'",Long.class,warehouse,order)>=30)
     throw bad("30 pallets are awaiting processing. Wait for them to finish before scanning more");
   }else if(((Number)existing.get(0).get("warehouse_id")).longValue()!=warehouse||((Number)existing.get(0).get("order_id")).longValue()!=order)
    throw bad("Request belongs to another order");
   // Creates the real Pick and consumes held quantity atomically before acknowledging acceptance.
   // Same UUID replays validate the full payload and cannot allocate twice.
   ManualPickOperation op=picks.prepare(requestId,warehouse,order,sourceLpn,newLpn,keepOriginal,quantity,expectedQuantity,destination);
   if(existing.isEmpty())db.update("INSERT INTO manual_pick_queue (request_id,warehouse_id,order_id,username,rf_code,company_id,state) VALUES (?,?,?,?,?,?,?)",
    requestId,warehouse,order,username,clean(context.getRfCode()),clean(context.getCompanyId()),op.completed?"COMPLETED":"QUEUED");
   return result(requestId);
  });
 }
 public List<Map<String,Object>> list(long warehouse,long order,ClientRestriction restriction){
  access(warehouse,order,restriction);
  return db.queryForList("SELECT q.request_id AS requestId,q.username AS submittedBy,q.state,q.error_message AS errorMessage,o.source_lpn AS sourceLpn,o.quantity,o.pick_id AS pickId,q.created_at AS createdAt FROM manual_pick_queue q JOIN manual_pick_operation o ON o.request_id=q.request_id WHERE q.warehouse_id=? AND q.order_id=? ORDER BY q.created_at DESC,q.request_id DESC LIMIT 100",warehouse,order);
 }
 public Map<String,Object> retry(long warehouse,long order,ClientRestriction restriction,String id){
  access(warehouse,order,restriction);
  return tx.execute(s->{
   List<Map<String,Object>> rows=db.queryForList("SELECT request_id FROM manual_pick_queue WHERE request_id=? AND warehouse_id=? AND order_id=? FOR UPDATE",id,warehouse,order);
   if(rows.isEmpty())throw bad("Saved task not found for this order");
   db.update("UPDATE manual_pick_queue SET state='QUEUED',error_message=NULL,updated_at=CURRENT_TIMESTAMP WHERE request_id=? AND state='NEEDS_REVIEW'",id);
   return result(id);
  });
 }
 private Map<String,Object> result(String id){return db.queryForMap("SELECT q.request_id AS requestId,q.state,o.source_lpn AS sourceLpn,o.quantity,o.pick_id AS pickId FROM manual_pick_queue q JOIN manual_pick_operation o ON o.request_id=q.request_id WHERE q.request_id=?",id);}
 public void drain(){
  if(!enabled)return;
  // SKIP LOCKED permits multiple replicas without executing the same task twice.
  // An error blocks later tasks for that order; other orders can still proceed.
  tx.execute(s->{
   List<Map<String,Object>> rows=db.queryForList("SELECT q.* FROM manual_pick_queue q WHERE q.state='QUEUED' AND NOT EXISTS (SELECT 1 FROM manual_pick_queue prior WHERE prior.order_id=q.order_id AND (prior.state='NEEDS_REVIEW' OR (prior.state='QUEUED' AND (prior.created_at<q.created_at OR (prior.created_at=q.created_at AND prior.request_id<q.request_id))))) ORDER BY q.created_at,q.request_id LIMIT 1 FOR UPDATE SKIP LOCKED");
   if(rows.isEmpty())return null;
   Map<String,Object> row=rows.get(0);String id=(String)row.get("request_id");
   UserContext previous=UserContextHolder.getContext();
   org.springframework.security.core.context.SecurityContext previousSecurity=SecurityContextHolder.getContext();
   UserContext context=new UserContext();context.setWarehouseId(row.get("warehouse_id").toString());context.setCompanyId((String)row.get("company_id"));context.setRfCode((String)row.get("rf_code"));context.setCorrelationId(id);
   try{
    workerActor.set((String)row.get("username"));
    UserContextHolder.setContext(context);
    org.springframework.security.core.context.SecurityContext security=SecurityContextHolder.createEmptyContext();
    security.setAuthentication(new UsernamePasswordAuthenticationToken(row.get("username"),null,Collections.emptyList()));SecurityContextHolder.setContext(security);
    isolated.execute(inner->{picks.finish(id);return null;});
    db.update("UPDATE manual_pick_queue SET state='COMPLETED',error_message=NULL,updated_at=CURRENT_TIMESTAMP WHERE request_id=?",id);
   }catch(Exception error){
    // Never release the reservation or create another Pick on an uncertain movement response.
    // Retrying this saved UUID uses Inventory's idempotent movement and final confirmation.
    db.update("UPDATE manual_pick_queue SET state='NEEDS_REVIEW',error_message=?,updated_at=CURRENT_TIMESTAMP WHERE request_id=?", "Pick could not be confirmed. Retry this saved task; do not rescan or move the pallet again. Reference: "+id,id);
    org.slf4j.LoggerFactory.getLogger(getClass()).warn("Queued Pick {} requires recovery: {}",id,error.getClass().getSimpleName());
   }finally{workerActor.remove();UserContextHolder.setContext(previous);SecurityContextHolder.setContext(previousSecurity);}
   return null;
  });
 }
 private static String clean(String s){return s==null?"":s;}
 private static OrderOperationException bad(String s){return OrderOperationException.raiseException(s);}
}
