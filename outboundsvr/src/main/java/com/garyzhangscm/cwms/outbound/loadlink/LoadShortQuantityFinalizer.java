package com.garyzhangscm.cwms.outbound.loadlink;

import java.util.*;
import javax.sql.DataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/** Finalizes only a verified, already-shipped, single-shipment order line.
 * Existing inventory batch remains the authority for shipment completion.
 * No inventory movement, reservation release, or order-demand adjustment occurs here.
 */
public final class LoadShortQuantityFinalizer {
 private final JdbcTemplate db;
 private final TransactionTemplate tx;
 public LoadShortQuantityFinalizer(DataSource ds) {
  this(ds,new DataSourceTransactionManager(ds));
 }
 public LoadShortQuantityFinalizer(DataSource ds,org.springframework.transaction.PlatformTransactionManager manager) {
  db=new JdbcTemplate(ds); tx=new TransactionTemplate(manager);
 }
 public void finish(long warehouse,long order,long shipment,long line,long expectedShipped,long expectedShort,String actor) {
  require(actor!=null&&!actor.trim().isEmpty()&&actor.length()<=100,"An authenticated operator is required.");
  require(expectedShipped>0&&expectedShort>0,"Explicit actual and short quantities are required.");
  tx.execute(status->{
   Map<String,Object> o=one("SELECT * FROM outbound_order WHERE warehouse_id=? AND outbound_order_id=? FOR UPDATE",warehouse,order);
   require("COMPLETE".equals(o.get("status")),"Order shipment completion has not finished.");
   Map<String,Object> ol=one("SELECT * FROM outbound_order_line WHERE warehouse_id=? AND outbound_order_id=? AND outbound_order_line_id=? FOR UPDATE",warehouse,order,line);
   Map<String,Object> s=one("SELECT * FROM shipment WHERE warehouse_id=? AND shipment_id=? FOR UPDATE",warehouse,shipment);
   require("DISPATCHED".equals(s.get("status")),"Shipment completion has not finished.");
   Map<String,Object> sl=one("SELECT * FROM shipment_line WHERE warehouse_id=? AND shipment_id=? AND outbound_order_line_id=? FOR UPDATE",warehouse,shipment,line);
   require(db.queryForObject("SELECT COUNT(*) FROM shipment_line WHERE warehouse_id=? AND outbound_order_line_id=?",Long.class,warehouse,line)==1L,"Multiple shipment lines require separate reconciliation.");
   require(one("SELECT completed_at FROM shipment_inventory_batch WHERE warehouse_id=? AND shipment_id=? FOR UPDATE",warehouse,shipment).get("completed_at")!=null,"Inventory shipment batch is not complete.");
   long demand=Math.addExact(expectedShipped,expectedShort);
   require(n(ol,"expected_quantity")==demand&&n(sl,"quantity")==demand,"Original requested quantity changed.");
   require(n(ol,"shipped_quantity")==expectedShipped&&n(sl,"shipped_quantity")==expectedShipped,"Actual shipped quantity changed.");
   require(n(ol,"open_quantity")==0&&n(sl,"inprocess_quantity")==0&&n(sl,"loaded_quantity")==0,"Unexpected outstanding shipment quantities.");
   List<Map<String,Object>> reserved=db.queryForList("SELECT * FROM quantity_allocation_line WHERE warehouse_id=? AND order_line_id=? FOR UPDATE",warehouse,line);
   for(Map<String,Object> r:reserved) require(n(r,"held")==0,"Release remaining quantity reservations through the existing allocation flow first.");
   String key="LOAD-SHORT-"+warehouse+"-"+shipment+"-"+line;
   long recorded=db.queryForObject("SELECT COUNT(*) FROM order_activity WHERE warehouse_id=? AND number=?",Long.class,warehouse,key);
   if(recorded>0) {
    require(recorded==1&&n(ol,"inprocess_quantity")==0&&n(sl,"open_quantity")==0,"Short completion record and quantities disagree.");
    return null;
   }
   require(n(ol,"inprocess_quantity")==expectedShort&&n(sl,"open_quantity")==expectedShort,"Remaining quantity differs from the confirmed shortage.");
   db.update("INSERT INTO order_activity(number,type,quantity,warehouse_id,outbound_order_id,outbound_order_number,outbound_order_line_id,outbound_order_line_number,shipment_id,shipment_number,shipment_line_id,shipment_line_number,username,transaction_group_id,created_by,last_modified_by,created_time,last_modified_time,activity_datetime,old_order_line_expected_quantity,new_order_line_expected_quantity,old_order_line_open_quantity,new_order_line_open_quantity,old_order_line_inprocess_quantity,new_order_line_inprocess_quantity,old_order_line_shipped_quantity,new_order_line_shipped_quantity,old_shipment_line_quantity,new_shipment_line_quantity,old_shipment_line_open_quantity,new_shipment_line_open_quantity,old_shipment_line_inprocess_quantity,new_shipment_line_inprocess_quantity,old_shipment_line_loaded_quantity,new_shipment_line_loaded_quantity,old_shipment_line_shipped_quantity,new_shipment_line_shipped_quantity) VALUES(?,'ORDER_COMPLETE',?,?,?,?,?,?,?,?,?,?,?,?,?,?,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,?,?,0,0,?,0,?,?,?,?,?,0,0,0,0,0,?,?)",
    key,expectedShort,warehouse,order,o.get("number"),line,ol.get("number"),shipment,s.get("number"),n(sl,"shipment_line_id"),sl.get("number"),actor,key,actor,actor,demand,demand,expectedShort,expectedShipped,expectedShipped,demand,demand,expectedShort,expectedShipped,expectedShipped);
   db.update("UPDATE outbound_order_line SET inprocess_quantity=0,last_modified_by=?,last_modified_time=CURRENT_TIMESTAMP WHERE outbound_order_line_id=? AND warehouse_id=?",actor,line,warehouse);
   db.update("UPDATE shipment_line SET open_quantity=0,last_modified_by=?,last_modified_time=CURRENT_TIMESTAMP WHERE shipment_line_id=? AND warehouse_id=?",actor,n(sl,"shipment_line_id"),warehouse);
   return null;
  });
 }
 private Map<String,Object> one(String q,Object...args){List<Map<String,Object>> rows=db.queryForList(q,args);require(rows.size()==1,"Expected one warehouse-scoped record.");return rows.get(0);}
 private static long n(Map<String,Object> m,String k){Object v=m.get(k);require(v instanceof Number,"Required quantity is missing: "+k);return ((Number)v).longValue();}
 private static void require(boolean ok,String message){if(!ok)throw new IllegalArgumentException(message);}
}
