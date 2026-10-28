package com.garyzhangscm.cwms.outbound.quantity;
import com.garyzhangscm.cwms.outbound.model.*;
import com.garyzhangscm.cwms.outbound.service.PreprintedManualPickService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import javax.sql.DataSource;
import java.util.*;

/** Scalar view of the exact saved operation and its Pick; no full entity graph is loaded. */
@RestController
public class ManualSplitGuardController {
 private final JdbcTemplate db;
 @Autowired private PreprintedManualPickService workflow;
 public ManualSplitGuardController(DataSource source){db=new JdbcTemplate(source);}
 @GetMapping("/manual-pick-fast/capabilities")
 public Map<String,Object> capabilities(){return Map.of("schema","manual-pick-fast-v1","enabled",workflow.fastEnabled());}
 @GetMapping("/picks/{id}/manual-split-check") @ClientValidationEndpoint
 public Map<String,Object> view(@PathVariable Long id,@RequestParam Long warehouseId,@RequestParam String requestId,ClientRestriction restriction){
  UUID.fromString(requestId);if(id==null||id<=0||warehouseId==null||warehouseId<=0)throw bad("Invalid split scope");
  List<Map<String,Object>> rows=db.queryForList("SELECT p.pick_id,p.number,p.warehouse_id,p.item_id,p.inventory_status_id,p.source_location_id,p.destination_location_id,p.work_order_line_id,p.quantity,p.picked_quantity,op.order_id,op.source_inventory_id,op.source_lpn,op.source_quantity,op.new_lpn,op.keep_original,op.destination_id,o.client_id FROM manual_pick_operation op JOIN pick p ON p.pick_id=op.pick_id JOIN shipment_line sl ON sl.shipment_line_id=p.shipment_line_id JOIN outbound_order_line ol ON ol.outbound_order_line_id=sl.outbound_order_line_id JOIN outbound_order o ON o.outbound_order_id=op.order_id AND o.outbound_order_id=ol.outbound_order_id WHERE op.request_id=? AND op.pick_id=? AND op.warehouse_id=? AND p.warehouse_id=op.warehouse_id AND o.warehouse_id=op.warehouse_id AND (p.quantity-p.picked_quantity)=op.quantity AND op.completed=FALSE AND op.cancelled=FALSE",requestId,id,warehouseId);
  if(rows.size()!=1)throw bad("No matching active saved manual pick operation");
  Map<String,Object> row=rows.get(0);Long client=row.get("client_id")==null?null:number(row,"client_id");
  if(restriction!=null&&Boolean.TRUE.equals(restriction.getThreePartyLogisticsFlag())&&!Boolean.TRUE.equals(restriction.getAllClientAccess())) {
   boolean allowed=client==null?Boolean.TRUE.equals(restriction.getNonClientDataAccessible()):restriction.getClientAccesses()!=null&&Arrays.stream(restriction.getClientAccesses().split(",")).anyMatch(v->v.trim().equals(client.toString()));
   if(!allowed)throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Client scope does not permit this pick");
  }
  Map<String,Object> pick=new LinkedHashMap<>();
  for(String[] pair:new String[][]{{"id","pick_id"},{"warehouseId","warehouse_id"},{"itemId","item_id"},{"sourceLocationId","source_location_id"},{"destinationLocationId","destination_location_id"},{"quantity","quantity"},{"pickedQuantity","picked_quantity"}})pick.put(pair[0],number(row,pair[1]));
  pick.put("number",row.get("number"));if(row.get("work_order_line_id")!=null)pick.put("workOrderLineId",number(row,"work_order_line_id"));
  List<Map<String,Object>> hops=db.queryForList("SELECT pick_movement_id,location_id,sequence FROM pick_movement WHERE pick_id=? ORDER BY sequence,pick_movement_id",id);List<Map<String,Object>> movements=new ArrayList<>();
  for(Map<String,Object> hop:hops)movements.add(Map.of("id",number(hop,"pick_movement_id"),"locationId",number(hop,"location_id"),"sequence",number(hop,"sequence")));
  pick.put("pickMovements",movements);
  Map<String,Object> result=new LinkedHashMap<>();result.put("schema","manual-split-guard-v1");result.put("requestId",requestId);result.put("pick",pick);
  for(String[] pair:new String[][]{{"warehouseId","warehouse_id"},{"orderId","order_id"},{"sourceInventoryId","source_inventory_id"},{"sourceQuantity","source_quantity"},{"destinationId","destination_id"}})result.put(pair[0],number(row,pair[1]));
  result.put("inventoryStatusId",number(row,"inventory_status_id"));result.put("clientId",client==null?0L:client);result.put("sourceLpn",row.get("source_lpn"));result.put("newLpn",Objects.toString(row.get("new_lpn"),""));result.put("keepOriginal",bool(row.get("keep_original")));return result;
 }
 private long number(Map<String,Object> row,String key){Object value=row.get(key);if(!(value instanceof Number))throw bad("Missing split field: "+key);return ((Number)value).longValue();}
 private boolean bool(Object value){if(value instanceof Boolean)return (Boolean)value;if(value instanceof Number)return ((Number)value).intValue()!=0;if(value instanceof byte[]&&((byte[])value).length==1)return ((byte[])value)[0]!=0;throw bad("Missing split mode");}
 private ResponseStatusException bad(String message){return new ResponseStatusException(HttpStatus.CONFLICT,message);}
}
