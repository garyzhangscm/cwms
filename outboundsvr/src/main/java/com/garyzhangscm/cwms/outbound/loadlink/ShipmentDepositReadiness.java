package com.garyzhangscm.cwms.outbound.loadlink;
import java.util.*;
import javax.sql.DataSource;
import org.springframework.jdbc.core.JdbcTemplate;
/** Readiness from actual Pick + inventory location, not just shipment status. */
public final class ShipmentDepositReadiness implements java.util.function.BiConsumer<Long,Long> {
 private final JdbcTemplate db;
 public ShipmentDepositReadiness(DataSource ds){db=new JdbcTemplate(ds);}
 private void require(boolean ok,String msg){if(!ok)throw new IllegalArgumentException(msg);}
 private long n(Map<String,Object> m,String k){Object v=m.get(k);return v==null?0:v instanceof Boolean?((Boolean)v?1:0):((Number)v).longValue();}
 public void accept(Long warehouse,Long shipment){ validate(warehouse,shipment,false); }
 /** Explicit short-shipment path; ordinary linkage keeps its full-quantity requirement. */
 public Map<String,Long> validate(Long warehouse,Long shipment,boolean allowShort){
  return validate(warehouse,shipment,allowShort,true);
 }
 public Map<String,Long> validate(Long warehouse,Long shipment,boolean allowShort,boolean lock){
  String lockClause=lock?" FOR UPDATE":"";
  long totalOrdered=0,totalPicked=0,totalShort=0;
  String status=db.queryForObject("SELECT status FROM shipment WHERE shipment_id=? AND warehouse_id=?",String.class,shipment,warehouse);
  require(List.of("INPROCESS","STAGED").contains(status),"Shipment is not awaiting loading.");
  List<Map<String,Object>> lines=db.queryForList("SELECT * FROM shipment_line WHERE shipment_id=? AND warehouse_id=?"+lockClause,shipment,warehouse);
  require(!lines.isEmpty(),"Shipment has no order lines.");
  for(Map<String,Object> l:lines){
   long q=n(l,"quantity"),lid=n(l,"shipment_line_id");
   long open=n(l,"open_quantity"), inprocess=n(l,"inprocess_quantity");
   require(q>0&&open>=0&&inprocess>=0&&Math.addExact(open,inprocess)==q&&n(l,"loaded_quantity")==0&&n(l,"shipped_quantity")==0&&(allowShort||open==0),"Pick quantities incomplete, or loading/shipping has started.");
   List<Map<String,Object>> picks=db.queryForList("SELECT pick_id,quantity,picked_quantity,destination_location_id FROM pick WHERE shipment_line_id=? AND warehouse_id=? ORDER BY pick_id"+lockClause,lid,warehouse);
   require(!picks.isEmpty()||inprocess==0,"No Pick records.");long picked=0,deposited=0;
   for(Map<String,Object> p:picks){
    require(n(p,"quantity")==n(p,"picked_quantity"),"Pick is not complete.");picked=Math.addExact(picked,n(p,"picked_quantity"));
    List<Map<String,Object>> inv=db.queryForList("SELECT i.quantity,i.location_id,l.enabled,l.locked,t.shipping_stage_locations FROM inventory i LEFT JOIN location l ON l.location_id=i.location_id AND l.warehouse_id=i.warehouse_id LEFT JOIN location_group g ON g.location_group_id=l.location_group_id LEFT JOIN location_group_type t ON t.location_group_type_id=g.location_group_type_id WHERE i.pick_id=? AND i.warehouse_id=?",n(p,"pick_id"),warehouse);
    long pickDeposited=0;
    for(Map<String,Object> i:inv){
     require(n(i,"quantity")>0&&n(i,"shipping_stage_locations")==1&&n(i,"enabled")==1&&n(i,"locked")==0&&n(i,"location_id")==n(p,"destination_location_id"),"Inventory has not been deposited at the Pick shipping stage.");
     pickDeposited=Math.addExact(pickDeposited,n(i,"quantity"));
    }
    require(pickDeposited==n(p,"picked_quantity"),"Deposited inventory does not match this Pick quantity.");
    deposited=Math.addExact(deposited,pickDeposited);
   }
   require(picked==inprocess&&deposited==inprocess,"Picked/deposited inventory does not match Shipment quantity.");
   totalOrdered=Math.addExact(totalOrdered,q);totalPicked=Math.addExact(totalPicked,picked);totalShort=Math.addExact(totalShort,open);
  }
  require(totalPicked>0,"No deposited quantity to ship.");
  Map<String,Long> result=new LinkedHashMap<>();result.put("ordered",totalOrdered);result.put("toShip",totalPicked);result.put("shortQuantity",totalShort);return result;
 }
}
