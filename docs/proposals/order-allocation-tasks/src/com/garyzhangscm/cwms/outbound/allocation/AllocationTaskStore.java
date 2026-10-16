package com.garyzhangscm.cwms.outbound.allocation;
import java.sql.*;
import java.time.Instant;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
public class AllocationTaskStore {
 public final JdbcTemplate jdbc;
 public AllocationTaskStore(JdbcTemplate jdbc) { this.jdbc = jdbc; }
 private static Instant time(ResultSet r,String name) throws SQLException { Timestamp t=r.getTimestamp(name); return t==null?null:t.toInstant(); }
 private final RowMapper<AllocationTask> mapper=(r,n)->{
  AllocationTask t=new AllocationTask(); t.id=r.getString("id"); t.warehouseId=r.getLong("warehouse_id"); t.orderId=r.getLong("order_id");
  t.requestId=r.getString("request_id"); t.activeOrderKey=r.getString("active_order_key"); t.state=r.getString("state"); t.originalOrderState=r.getString("original_order_state");
  t.errorCode=r.getString("error_code"); t.message=r.getString("message"); t.requiresReview=r.getBoolean("requires_review");
  t.createdAt=time(r,"created_at"); t.startedAt=time(r,"started_at"); t.finishedAt=time(r,"finished_at"); t.heartbeatAt=time(r,"heartbeat_at"); return t;
 };
 public AllocationTask one(String sql,Object...args) { List<AllocationTask> rows=jdbc.query(sql,mapper,args); return rows.isEmpty()?null:rows.get(0); }
 public AllocationTask get(long warehouseId,String id) { return one("select * from outbound_allocation_task where warehouse_id=? and id=?",warehouseId,id); }
 public AllocationTask request(long warehouseId,long orderId,String requestId) { return one("select * from outbound_allocation_task where warehouse_id=? and order_id=? and request_id=?",warehouseId,orderId,requestId); }
 public AllocationTask active(long warehouseId,long orderId) { return one("select * from outbound_allocation_task where active_order_key=?",warehouseId+":"+orderId); }
 public void insert(AllocationTask t) {
  jdbc.update("insert into outbound_allocation_task (id,warehouse_id,order_id,request_id,active_order_key,state,original_order_state,error_code,message,requires_review,created_at,heartbeat_at,finished_at) values (?,?,?,?,?,?,?,?,?,?,?,?,?)",
   t.id,t.warehouseId,t.orderId,t.requestId,t.activeOrderKey,t.state,t.originalOrderState,t.errorCode,t.message==null?null:t.message.substring(0,Math.min(1000,t.message.length())),t.requiresReview,Timestamp.from(t.createdAt),Timestamp.from(t.heartbeatAt),t.finishedAt==null?null:Timestamp.from(t.finishedAt));
 }
 public boolean start(AllocationTask t,Instant now) { return jdbc.update("update outbound_allocation_task set state='RUNNING',started_at=?,heartbeat_at=? where id=? and state='QUEUED'",Timestamp.from(now),Timestamp.from(now),t.id)==1; }
 public void heartbeat(String id,Instant now) { jdbc.update("update outbound_allocation_task set heartbeat_at=? where id=? and state='RUNNING'",Timestamp.from(now),id); }
 public void finish(AllocationTask t,String state,String code,String message,boolean review,Instant now) {
  jdbc.update("update outbound_allocation_task set state=?,error_code=?,message=?,requires_review=?,finished_at=?,heartbeat_at=?,active_order_key=? where id=? and state in ('QUEUED','RUNNING')",
   state,code,message==null?null:message.substring(0,Math.min(1000,message.length())),review,Timestamp.from(now),Timestamp.from(now),review?t.warehouseId+":"+t.orderId:null,t.id);
 }
 public List<AllocationTask> stale(Instant before) { return jdbc.query("select * from outbound_allocation_task where state in ('QUEUED','RUNNING') and heartbeat_at<?",mapper,Timestamp.from(before)); }
 public List<AllocationTask> latest(long warehouseId,List<Long> orders) {
  if(orders.isEmpty())return Collections.emptyList();
  if(orders.size()>100)throw new AllocationValidationFailure("TOO_MANY_ORDERS","At most 100 order IDs are allowed.");
  String markers=String.join(",",Collections.nCopies(orders.size(),"?")); List<Object> args=new ArrayList<>();args.add(warehouseId);args.addAll(orders);
  return jdbc.query("select t.* from outbound_allocation_task t where t.warehouse_id=? and t.order_id in ("+markers+") and not exists (select 1 from outbound_allocation_task newer where newer.warehouse_id=t.warehouse_id and newer.order_id=t.order_id and newer.sequence_id>t.sequence_id)",mapper,args.toArray());
 }
}
