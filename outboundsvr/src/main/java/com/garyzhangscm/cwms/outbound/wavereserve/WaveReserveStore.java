package com.garyzhangscm.cwms.outbound.wavereserve;

import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
/** All mutations run under QuantityAllocationService.locked(warehouse). Claims partition held; never debit stock. */
public final class WaveReserveStore {
 public final JdbcTemplate db;
 public WaveReserveStore(JdbcTemplate db){this.db=db;}
 public static long n(Map<String,Object> r,String k){Object v=r.get(k);return v==null?0:((Number)v).longValue();}
 public static void require(boolean ok,String message){if(!ok)throw new IllegalArgumentException(message);}
 public boolean managed(long w,long wave){return db.queryForObject("SELECT COUNT(*) FROM wave_reserve_run WHERE warehouse_id=? AND wave_id=?",Long.class,w,wave)>0;}
 public List<Map<String,Object>> lines(long w,long wave){return db.queryForList("SELECT sl.shipment_line_id,sl.outbound_order_line_id order_line_id,ol.outbound_order_id order_id,ol.item_id,sl.quantity,sl.open_quantity,sl.inprocess_quantity,sl.shipped_quantity,ol.number line_number,o.number order_number,s.number shipment_number,COALESCE(c.remaining,0) wave_reserved,COALESCE(q.held,0) held,COALESCE(q.committed,0) committed FROM shipment_line sl JOIN shipment s ON s.shipment_id=sl.shipment_id AND s.warehouse_id=sl.warehouse_id JOIN outbound_order_line ol ON ol.outbound_order_line_id=sl.outbound_order_line_id AND ol.warehouse_id=sl.warehouse_id JOIN outbound_order o ON o.outbound_order_id=ol.outbound_order_id AND o.warehouse_id=sl.warehouse_id LEFT JOIN quantity_allocation_line q ON q.order_line_id=ol.outbound_order_line_id LEFT JOIN wave_reserve_claim c ON c.shipment_line_id=sl.shipment_line_id AND c.warehouse_id=sl.warehouse_id WHERE sl.warehouse_id=? AND sl.wave_id=? AND sl.status<>'CANCELLED' ORDER BY ol.outbound_order_id,sl.shipment_line_id",w,wave);}
 public long claimed(long w,long line){return db.queryForObject("SELECT COALESCE(SUM(remaining),0) FROM wave_reserve_claim WHERE warehouse_id=? AND order_line_id=?",Long.class,w,line);}
 public Map<String,Object> claim(long w,long sl){List<Map<String,Object>> r=db.queryForList("SELECT * FROM wave_reserve_claim WHERE warehouse_id=? AND shipment_line_id=?",w,sl);return r.isEmpty()?null:r.get(0);}
 public Map<Long,Long> targets(long w,long wave){
  var rows=lines(w,wave);require(!rows.isEmpty(),"Wave has no active Shipment lines");var byLine=new LinkedHashMap<Long,Long>();
  for(var r:rows){require(n(r,"open_quantity")>=0,"Invalid open quantity");byLine.merge(n(r,"order_line_id"),n(r,"open_quantity")-n(r,"wave_reserved"),Math::addExact);}
  var targets=new LinkedHashMap<Long,Long>();
  for(var entry:byLine.entrySet()){
   var ledger=db.queryForMap("SELECT * FROM quantity_allocation_line WHERE warehouse_id=? AND order_line_id=?",w,entry.getKey());
   long required=Math.addExact(claimed(w,entry.getKey()),entry.getValue());
   long target=Math.max(n(ledger,"held"),required);
   require(target<=n(ledger,"demand")-n(ledger,"committed"),"Wave demands exceed the remaining order demand");targets.put(entry.getKey(),target);
  }return targets;
 }
 public void assign(long w,long wave){
  for(var r:lines(w,wave)){
   long line=n(r,"order_line_id"),remaining=n(r,"open_quantity"),sl=n(r,"shipment_line_id");
   var existing=claim(w,sl);require(existing==null||n(existing,"wave_id")==wave,"Shipment line belongs to another Wave reservation");
   long required=claimed(w,line)-n(r,"wave_reserved")+remaining;
   require(required<=n(r,"held"),"Insufficient available inventory to reserve all Wave lines. No reservations were changed.");
   db.update("INSERT INTO wave_reserve_claim(warehouse_id,wave_id,shipment_line_id,order_line_id,remaining) VALUES(?,?,?,?,?) ON DUPLICATE KEY UPDATE remaining=VALUES(remaining)",w,wave,sl,line,remaining);
  }
 }
 public void validateHeld(long w,long line,long target){require(target>=claimed(w,line),"This quantity is assigned to a Wave. Release its reservation before reducing the order reservation.");}
 public void validateConversion(long w,long line,long sl,long quantity,Long scopedWave){
  var ledger=db.queryForMap("SELECT held FROM quantity_allocation_line WHERE warehouse_id=? AND order_line_id=?",w,line);
  if(scopedWave==null){require(claim(w,sl)==null,"Pick this Shipment from its Wave scan screen");require(quantity<=n(ledger,"held")-claimed(w,line),"Reserved quantity belongs to a Wave; use its scan screen");}
  else {var c=claim(w,sl);require(c!=null&&n(c,"wave_id")==scopedWave&&n(c,"order_line_id")==line&&quantity>0&&quantity<=n(c,"remaining"),"Pick exceeds this Wave's reserved Shipment quantity");}
 }
 public void converted(long w,long sl,long quantity){int changed=db.update("UPDATE wave_reserve_claim SET remaining=remaining-? WHERE warehouse_id=? AND shipment_line_id=? AND remaining>=?",quantity,w,sl,quantity);require(changed==1,"Wave reservation changed during picking");}
 public void restored(long w,long sl,long quantity){if(quantity>0)db.update("UPDATE wave_reserve_claim SET remaining=remaining+? WHERE warehouse_id=? AND shipment_line_id=?",quantity,w,sl);}
 public void release(long w,long wave){db.update("DELETE FROM wave_reserve_claim WHERE warehouse_id=? AND wave_id=?",w,wave);}
}
