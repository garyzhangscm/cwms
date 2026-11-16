package com.garyzhangscm.cwms.workorder.qcproduction;

import java.util.*;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.Duration;
import java.util.function.Supplier;
import javax.sql.DataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;

/** Only qc_production_* tables are writable. Native production/inventory facts are read-only. */
public final class QcProductionStore {
 public final JdbcTemplate db;
 private final TransactionTemplate tx;
 private final ObjectMapper json=new ObjectMapper().findAndRegisterModules();
 public QcProductionStore(DataSource ds,PlatformTransactionManager manager){db=new JdbcTemplate(ds);tx=new TransactionTemplate(manager);tx.setTimeout(30);tx.setIsolationLevel(org.springframework.transaction.TransactionDefinition.ISOLATION_READ_COMMITTED);}
 static void require(boolean valid,String message){if(!valid)throw new IllegalArgumentException(message);}
 static long n(Map<String,Object> r,String key){Object v=r.get(key);return v==null?0:((Number)v).longValue();}
 static String str(Map<String,Object> r,String key){Object v=r.get(key);return v==null?"":v.toString();}
 public static String lpn(String value){require(value!=null&&value.trim().matches("[A-Za-z0-9._/\\-]{1,100}"),"Invalid LPN");return value.trim();}
 static Timestamp timestamp(Object value){if(value instanceof Timestamp)return (Timestamp)value;if(value instanceof java.time.LocalDateTime)return Timestamp.from(((java.time.LocalDateTime)value).toInstant(java.time.ZoneOffset.UTC));throw new IllegalStateException("Unsupported database date");}
 public Timestamp enabledAt(long w){var a=db.queryForList("SELECT enabled_at FROM qc_production_config WHERE warehouse_id=?",w);require(a.size()==1,"QC production confirmation is not enabled in this warehouse");return timestamp(a.get(0).get("enabled_at"));}
 private String factSql(){return "SELECT t.work_order_produce_transaction_id transaction_id,w.work_order_id order_id,w.number order_number,w.item_id,t.production_line_id,pl.name production_line,t.created_time production_time,t.created_by producer,p.lpn,MAX(COALESCE(q.confirmed_quantity,0)) qc_quantity,MAX(COALESCE(qs.state,'PENDING')) qc_state,SUM(p.quantity) registered_quantity,COALESCE((SELECT SUM(r.quantity) FROM work_order_reversed_production_inventory r WHERE r.work_order_produce_transaction_id=t.work_order_produce_transaction_id AND BINARY r.lpn=BINARY p.lpn),0) reversed_quantity,MAX(CASE WHEN EXISTS(SELECT 1 FROM work_order_production_receipt_job j WHERE j.detail_id=p.work_order_produced_inventory_id) OR NOT EXISTS(SELECT 1 FROM work_order_produced_inventory_result ir WHERE ir.work_order_produced_inventory_id=p.work_order_produced_inventory_id AND ir.result=TRUE) THEN 1 ELSE 0 END) receipt_pending FROM work_order_produced_inventory p JOIN work_order_produce_transaction t ON t.work_order_produce_transaction_id=p.work_order_produce_transaction_id JOIN work_order w ON w.work_order_id=t.work_order_id LEFT JOIN production_line pl ON pl.production_line_id=t.production_line_id LEFT JOIN qc_production_confirmation q ON q.warehouse_id=w.warehouse_id AND q.transaction_id=t.work_order_produce_transaction_id AND BINARY q.lpn=BINARY p.lpn LEFT JOIN qc_production_lpn qs ON qs.warehouse_id=w.warehouse_id AND BINARY qs.lpn=BINARY p.lpn WHERE w.warehouse_id=? ";}
 private String groupSql(){return " GROUP BY t.work_order_produce_transaction_id,w.work_order_id,w.number,w.item_id,t.production_line_id,pl.name,t.created_time,t.created_by,p.lpn ORDER BY t.work_order_produce_transaction_id";}
 private List<Map<String,Object>> facts(long w,String lpn){return db.queryForList(factSql()+" AND BINARY p.lpn=BINARY ?"+groupSql(),w,lpn);}
 private long net(Map<String,Object> r){return Math.max(0,n(r,"registered_quantity")-n(r,"reversed_quantity"));}
 private boolean eligible(Map<String,Object> r,Timestamp cutoff){return !timestamp(r.get("production_time")).before(cutoff);}
 private long confirmed(long w,Map<String,Object> r){return Math.min(net(r),n(r,"qc_quantity"));}
 public Map<String,Object> view(long w,String raw){String lpn=lpn(raw);Timestamp cutoff=enabledAt(w);var rows=facts(w,lpn);require(!rows.isEmpty(),"No production record found for this LPN in this warehouse");var state=db.queryForList("SELECT * FROM qc_production_lpn WHERE warehouse_id=? AND lpn=?",w,lpn);boolean blocked=!state.isEmpty()&&"EXCEPTION".equals(state.get(0).get("state"));long registered=0,accepted=0,historical=0;var details=new ArrayList<Map<String,Object>>();
  for(var r:rows){long qty=net(r);if(!eligible(r,cutoff)){historical=Math.addExact(historical,qty);continue;}long ok=blocked?0:confirmed(w,r);registered=Math.addExact(registered,qty);accepted=Math.addExact(accepted,ok);var d=new LinkedHashMap<>(r);d.put("production_time",timestamp(r.get("production_time")).toInstant().toString());d.put("net_quantity",qty);d.put("confirmed_quantity",ok);details.add(d);}
  var out=new LinkedHashMap<String,Object>();out.put("lpn",lpn);out.put("registeredQuantity",registered);out.put("confirmedQuantity",accepted);out.put("pendingQuantity",registered-accepted);out.put("historicalQuantity",historical);out.put("state",blocked?"EXCEPTION":details.isEmpty()?"HISTORICAL":registered>0&&accepted==registered?"CONFIRMED":accepted>0?"NEW_QUANTITY_PENDING":"PENDING");out.put("version",state.isEmpty()?0:n(state.get(0),"version"));out.put("reason",state.isEmpty()?"":str(state.get(0),"reason"));out.put("updatedBy",state.isEmpty()?"":str(state.get(0),"updated_by"));out.put("updatedAt",state.isEmpty()?null:(state.get(0).get("updated_at")==null?null:timestamp(state.get(0).get("updated_at")).toInstant().toString()));out.put("details",details);return out;
 }
 public Map<String,Object> act(long w,String actor,String action,String raw,String requestId,String reason,Long version,Supplier<Map<String,Object>> stock){
  String lpn=lpn(raw);UUID.fromString(requestId);require(Set.of("confirm","exception","resolve").contains(action),"Invalid action");String why=reason==null?"":reason.trim();require(why.length()<=240,"Reason is too long");if(!action.equals("confirm"))require(!why.isEmpty(),"Choose or enter a reason");String fp=action+"|"+lpn+"|"+why+"|"+version;
  return tx.execute(status->{
   enabledAt(w);db.queryForMap("SELECT warehouse_id FROM qc_production_config WHERE warehouse_id=? FOR UPDATE",w);
   var previous=db.queryForList("SELECT * FROM qc_production_request WHERE warehouse_id=? AND request_id=?",w,requestId);
   if(!previous.isEmpty()){var p=previous.get(0);require(actor.equals(p.get("actor"))&&fp.equals(p.get("fingerprint")),"Request ID was reused with different details");try{var original=json.readValue(str(p,"result_json"),new TypeReference<LinkedHashMap<String,Object>>(){});var replay=new LinkedHashMap<>(view(w,lpn));replay.put("outcome","REPLAYED");replay.put("originalOutcome",original.get("outcome"));replay.put("quantityDelta",0);replay.put("requestId",requestId);return replay;}catch(Exception e){throw new IllegalStateException(e);}}
   var before=view(w,lpn);String state=str(before,"state");require(!state.equals("HISTORICAL"),"This LPN has only production from before QC confirmation was enabled; historical totals remain unchanged");
   db.update("INSERT INTO qc_production_lpn(warehouse_id,lpn) VALUES(?,?) ON DUPLICATE KEY UPDATE lpn=lpn",w,lpn);
   long delta=0;String outcome;
   if(action.equals("confirm")){
    require(!state.equals("EXCEPTION"),"LPN is marked as an exception. Resolve it in Exceptions / Withdraw before confirming");
    if(n(before,"pendingQuantity")==0){outcome="ALREADY_CONFIRMED";}
    else {
     var snapshot=stock.get();require(Boolean.TRUE.equals(snapshot.get("exists")),"No current pallet inventory was found. Nothing was confirmed");
     var rows=facts(w,lpn);Timestamp cutoff=enabledAt(w);var orderIds=new HashSet<Long>();for(var r:rows)if(eligible(r,cutoff)&&net(r)>0)orderIds.add(n(r,"order_id"));
     require(orderIds.size()==1,"LPN is linked to multiple work orders; review it before confirming");
     @SuppressWarnings("unchecked") var stockOrders=(Set<Long>)snapshot.get("orderIds");require(stockOrders!=null&&stockOrders.containsAll(orderIds),"Current LPN inventory does not match the production work order");
     for(var r:rows){if(!eligible(r,cutoff))continue;require(n(r,"receipt_pending")==0,"Production inventory is still processing. Retry this LPN later");long qty=net(r);delta=Math.addExact(delta,qty-confirmed(w,r));db.update("INSERT INTO qc_production_confirmation(warehouse_id,transaction_id,lpn,confirmed_quantity,confirmed_by,confirmed_at) VALUES(?,?,?,?,?,UTC_TIMESTAMP(6)) ON DUPLICATE KEY UPDATE confirmed_quantity=VALUES(confirmed_quantity),confirmed_by=VALUES(confirmed_by),confirmed_at=VALUES(confirmed_at)",w,n(r,"transaction_id"),lpn,qty,actor);}
     db.update("UPDATE qc_production_lpn SET state='CONFIRMED',version=version+1,reason='',updated_by=?,updated_at=UTC_TIMESTAMP(6) WHERE warehouse_id=? AND lpn=?",actor,w,lpn);outcome="CONFIRMED";
    }
   }else if(action.equals("exception")){
    delta=-n(before,"confirmedQuantity");db.update("UPDATE qc_production_confirmation SET confirmed_quantity=0 WHERE warehouse_id=? AND lpn=?",w,lpn);db.update("UPDATE qc_production_lpn SET state='EXCEPTION',version=version+1,reason=?,updated_by=?,updated_at=UTC_TIMESTAMP(6) WHERE warehouse_id=? AND lpn=?",why,actor,w,lpn);outcome="EXCEPTION";
   }else{
    require(state.equals("EXCEPTION"),"This LPN is not currently an exception");require(version!=null&&version==n(before,"version"),"Exception changed; refresh it before resolving");db.update("UPDATE qc_production_lpn SET state='PENDING',version=version+1,reason='',updated_by=?,updated_at=UTC_TIMESTAMP(6) WHERE warehouse_id=? AND lpn=?",actor,w,lpn);outcome="RESOLVED";
   }
   if(!outcome.equals("ALREADY_CONFIRMED"))db.update("INSERT INTO qc_production_event(warehouse_id,lpn,action,actor,quantity_delta,reason,created_at) VALUES(?,?,?,?,?,?,UTC_TIMESTAMP(6))",w,lpn,action,actor,delta,why);
   var result=new LinkedHashMap<>(view(w,lpn));result.put("outcome",outcome);result.put("quantityDelta",delta);result.put("requestId",requestId);
   try{db.update("INSERT INTO qc_production_request(warehouse_id,request_id,actor,fingerprint,result_json,created_at) VALUES(?,?,?,?,?,UTC_TIMESTAMP(6))",w,requestId,actor,fp,json.writeValueAsString(result));}catch(Exception e){throw new IllegalStateException(e);}
   return result;
  });
 }
 public List<Map<String,Object>> exceptions(long w){enabledAt(w);return db.queryForList("SELECT lpn,state,version,reason,updated_by,updated_at FROM qc_production_lpn WHERE warehouse_id=? AND state='EXCEPTION' ORDER BY updated_at DESC LIMIT 500",w);}
 public List<Map<String,Object>> history(long w,String raw){enabledAt(w);return db.queryForList("SELECT event_id,lpn,action,actor,quantity_delta,reason,created_at FROM qc_production_event WHERE warehouse_id=? AND lpn=? ORDER BY event_id DESC LIMIT 100",w,lpn(raw));}
 public Map<String,Object> report(long w,Instant start,Instant end){
  var cutoff=enabledAt(w);
  require(start.isBefore(end)&&Duration.between(start,end).compareTo(Duration.ofDays(31))<=0,"Select a period of at most 31 days");
  Instant first=start.isBefore(cutoff.toInstant())?cutoff.toInstant():start;
  var rows=db.queryForList(factSql()+" AND t.created_time>=? AND t.created_time<?"+groupSql()+" LIMIT 20001",w,
      java.time.LocalDateTime.ofInstant(first,java.time.ZoneOffset.UTC),
      java.time.LocalDateTime.ofInstant(end,java.time.ZoneOffset.UTC));
  require(rows.size()<=20000,"Too many records; choose a shorter period");
  var result=new ArrayList<Map<String,Object>>();long registered=0,formal=0;
  for(var r:rows){
   long qty=net(r);boolean exception="EXCEPTION".equals(r.get("qc_state"));
   long ok=exception?0:confirmed(w,r);
   registered=Math.addExact(registered,qty);formal=Math.addExact(formal,ok);
   var d=new LinkedHashMap<>(r);
   d.put("production_time",timestamp(r.get("production_time")).toInstant().toString());
   d.put("registered_quantity",qty);d.put("formal_quantity",ok);d.put("pending_quantity",qty-ok);
   d.put("state",exception?"EXCEPTION":ok==qty?"CONFIRMED":ok>0?"NEW_QUANTITY_PENDING":"PENDING");
   result.add(d);
  }
  return Map.of("enabledAt",cutoff.toInstant().toString(),"registeredQuantity",registered,"formalQuantity",formal,"pendingQuantity",registered-formal,"rows",result);

 }
}
