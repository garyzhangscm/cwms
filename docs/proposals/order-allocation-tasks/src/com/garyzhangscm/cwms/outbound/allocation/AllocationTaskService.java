package com.garyzhangscm.cwms.outbound.allocation;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import org.springframework.transaction.support.TransactionTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
public class AllocationTaskService implements AutoCloseable {
 private static final Logger log=LoggerFactory.getLogger(AllocationTaskService.class);
 private final AllocationTaskStore store;
 private final AllocationOrderGateway orders;
 private final TransactionTemplate tx;
 private final Executor executor;
 private final Clock clock;
 private final ScheduledExecutorService heartbeats=Executors.newSingleThreadScheduledExecutor(r->{Thread t=new Thread(r,"allocation-heartbeat");t.setDaemon(true);return t;});
 public AllocationTaskService(AllocationTaskStore store,AllocationOrderGateway orders,TransactionTemplate tx,Executor executor,Clock clock) {
  this.store=store;this.orders=orders;this.tx=tx;this.executor=executor;this.clock=clock;
 }
 public AllocationTask submit(long warehouseId,long orderId,String requestId,boolean asynchronous) {
  if(requestId==null || !requestId.matches("[A-Za-z0-9_-]{8,64}"))throw new AllocationValidationFailure("INVALID_REQUEST_ID","A stable request ID (8–64 letters, digits, underscores or hyphens) is required.");
  final boolean[] created={false};
  AllocationTask task=tx.execute(status->{
   String original=orders.lockAndGetState(warehouseId,orderId);
   AllocationTask old=store.request(warehouseId,orderId,requestId);if(old!=null)return old;
   old=store.active(warehouseId,orderId);if(old!=null)return old;
   AllocationTask t=new AllocationTask();t.id=UUID.randomUUID().toString();t.requestId=requestId;t.warehouseId=warehouseId;t.orderId=orderId;t.originalOrderState=original;t.createdAt=clock.instant();t.heartbeatAt=t.createdAt;
   try {
    if(!Arrays.asList("OPEN","INPROCESS").contains(original))throw new AllocationValidationFailure("ORDER_NOT_READY","Order state "+original+" does not allow a new task. Review any earlier allocation first.");
    orders.validate(warehouseId,orderId);
   } catch(AllocationValidationFailure invalid) {
    t.state="FAILED";t.errorCode=invalid.code;t.message=invalid.getMessage();t.finishedAt=t.createdAt;store.insert(t);return t;
   }
   t.state="QUEUED";t.activeOrderKey=warehouseId+":"+orderId;store.insert(t);orders.markAllocating(warehouseId,orderId);created[0]=true;return t;
  });
  if(created[0]) {
   if(asynchronous)try{executor.execute(()->run(task));}catch(RejectedExecutionException full){finish(task,false,"QUEUE_FULL","Allocation queue is full. No allocation work was started.",false);}
   else run(task);
  }
  return store.get(warehouseId,task.id);
 }
 private void run(AllocationTask task) {
  boolean started=Boolean.TRUE.equals(tx.execute(s->{orders.lockAndGetState(task.warehouseId,task.orderId);return store.start(task,clock.instant());}));
  if(!started)return;
  ScheduledFuture<?> beat=heartbeats.scheduleWithFixedDelay(()->{try{store.heartbeat(task.id,clock.instant());}catch(Exception e){log.error("Allocation heartbeat failed for task {}",task.id,e);}},5,5,TimeUnit.SECONDS);
  try {
   tx.execute(s->{String state=orders.lockAndGetState(task.warehouseId,task.orderId);if(!"ALLOCATING".equals(state))throw new AllocationValidationFailure("ORDER_CHANGED","Order state changed before allocation started.");orders.validate(task.warehouseId,task.orderId);return null;});
   orders.execute(task.orderId);
   finish(task,true,null,null,false);
  } catch(Exception failure) {
   log.error("Allocation task {} failed for order {}",task.id,task.orderId,failure);
   Throwable reason=failure;
   for(int i=0;i<10 && reason.getCause()!=null && reason.getCause()!=reason;i++)reason=reason.getCause();
   String code=failure instanceof AllocationValidationFailure?((AllocationValidationFailure)failure).code:"ALLOCATION_FAILED";
   String message=failure instanceof AllocationValidationFailure?failure.getMessage():"Allocation failed ("+reason.getClass().getSimpleName()+"). Review existing picks and shipment lines before retrying. Reference: "+task.id;
   if(reason instanceof com.garyzhangscm.cwms.outbound.exception.AllocationException || reason instanceof com.garyzhangscm.cwms.outbound.exception.OrderOperationException) {
    code="ALLOCATION_REJECTED";message=reason.getMessage();
   } else if(reason instanceof java.sql.SQLException || reason instanceof org.springframework.dao.DataAccessException) {
    code="DATABASE_ERROR";message="Allocation could not finish because a database operation failed. Reference: "+task.id;
   } else if(reason instanceof org.springframework.web.client.RestClientException || reason instanceof java.net.SocketTimeoutException) {
    code="UPSTREAM_ERROR";message="An allocation dependency was unavailable or timed out. Reference: "+task.id;
   }
   finish(task,false,code,message,true);
  } finally {beat.cancel(false);}
 }
 private void finish(AllocationTask task,boolean success,String code,String message,boolean workStarted) {
  tx.execute(s->{
   orders.lockAndGetState(task.warehouseId,task.orderId);
   AllocationTask current=store.get(task.warehouseId,task.id);
   if(current==null || !current.active())return null;
   boolean review=!success && workStarted && orders.hasPlannedWork(task.orderId);
   orders.finish(task.warehouseId,task.orderId,task.originalOrderState,success);
   store.finish(task,success?"SUCCEEDED":"FAILED",code,message,review,clock.instant());return null;
  });
 }
 public void recoverStaleTasks() {
  for(AllocationTask candidate:store.stale(clock.instant().minusSeconds(300)))tx.execute(s->{
   orders.lockAndGetState(candidate.warehouseId,candidate.orderId);
   AllocationTask current=store.get(candidate.warehouseId,candidate.id);
   if(current!=null && current.active() && current.heartbeatAt.isBefore(clock.instant().minusSeconds(300))) {
    // Keep the unique active key: uncertain work must never be automatically repeated.
    orders.finish(current.warehouseId,current.orderId,current.originalOrderState,false);
    store.finish(current,"REVIEW_REQUIRED","WORKER_LOST","Allocation worker stopped reporting progress. Review existing work before retrying. Reference: "+current.id,true,clock.instant());
   }return null;
  });
 }
 public AllocationTask get(long warehouseId,String id) { AllocationTask t=store.get(warehouseId,id);if(t==null)throw new AllocationValidationFailure("TASK_NOT_FOUND","Allocation task was not found in this warehouse.");return t; }
 public List<AllocationTask> latest(long warehouseId,List<Long> ids) {return store.latest(warehouseId,ids);}
 public void close(){heartbeats.shutdownNow();}
}
