package com.garyzhangscm.cwms.inventory.service;
import com.garyzhangscm.cwms.inventory.model.*;
import com.garyzhangscm.cwms.inventory.clients.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.http.HttpMethod;
import java.util.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/** Durable receipt and stock movement commit in the same database transaction. */
@Service
public class ShipmentInventoryBatchService {
 @Autowired private JdbcTemplate db;
 @PersistenceContext private EntityManager em;
 @Autowired private InventoryService inventory;
 @Autowired private WarehouseLayoutServiceRestemplateClient layout;
 @Autowired private RestTemplateProxy rest;
 @Autowired private org.springframework.transaction.PlatformTransactionManager transactionManager;
 private final ObjectMapper json=new ObjectMapper();
 static long n(Object v){if(!(v instanceof Number))throw new IllegalArgumentException("Missing numeric batch field");return ((Number)v).longValue();}
 static void require(boolean b,String message){if(!b)throw new IllegalArgumentException(message);}
 void access(Long client,ClientRestriction r){
  // ClientValidationEndpoint intentionally returns null for non-3PL warehouses.
  if(r==null)return;
  if(!Boolean.TRUE.equals(r.getThreePartyLogisticsFlag())||Boolean.TRUE.equals(r.getAllClientAccess()))return;
  if(client==null){require(Boolean.TRUE.equals(r.getNonClientDataAccessible()),"Client access denied");return;}
  require(r.getClientAccesses()!=null&&Arrays.stream(r.getClientAccesses().split(",")).map(String::trim).anyMatch(x->x.equals(client.toString())),"Client access denied");
 }
 @SuppressWarnings("unchecked") List<Map<String,Object>> decode(String value)throws Exception{return json.readValue(value,List.class);}
 List<Map<String,Object>> receipt(long warehouse,long shipment,boolean lock){return db.queryForList("SELECT destination_id,request_hash,snapshot_json,completed_at FROM shipment_inventory_batch WHERE warehouse_id=? AND shipment_id=?"+(lock?" FOR UPDATE":""),warehouse,shipment);}
 void scope(long w,long s){require(w>0&&s>0,"Invalid shipment scope");require(db.queryForObject("SELECT COUNT(*) FROM shipment WHERE shipment_id=? AND warehouse_id=?",Long.class,s,w)==1,"Shipment not found in warehouse");}
 @Transactional(readOnly=true)
 public List<Map<String,Object>> preview(long w,long s,ClientRestriction restriction)throws Exception{
  scope(w,s);List<Map<String,Object>> prior=receipt(w,s,false);
  if(!prior.isEmpty()){List<Map<String,Object>> rows=decode((String)prior.get(0).get("snapshot_json"));authorize(rows,restriction);return rows;}
  List<Map<String,Object>> raw=db.queryForList("SELECT i.inventory_id id,i.lpn,i.quantity,i.pick_id pickId,i.item_id itemId,i.location_id locationId,i.client_id clientId,i.warehouse_id warehouseId FROM inventory i JOIN pick p ON p.pick_id=i.pick_id JOIN shipment_line sl ON sl.shipment_line_id=p.shipment_line_id WHERE sl.shipment_id=? AND i.warehouse_id=? ORDER BY i.inventory_id",s,w);
  require(raw.size()<=5000,"Shipment exceeds batch limit");authorize(raw,restriction);
  Set<Long> ids=new TreeSet<>();for(Map<String,Object> row:raw)ids.add(n(row.get("locationId")));
  Map<Long,Location> locations=new HashMap<>();List<Long> keys=new ArrayList<>(ids);
  for(int start=0;start<keys.size();start+=200){String joined=keys.subList(start,Math.min(start+200,keys.size())).stream().map(String::valueOf).collect(java.util.stream.Collectors.joining(","));for(Location l:rest.exchangeList(Location.class,"http://apigateway:5555/api/layout/locations?warehouseId="+w+"&includeDisabledLocation=true&ids="+joined,HttpMethod.GET,null))locations.put(l.getId(),l);}
  for(Map<String,Object> row:raw){Location l=locations.get(n(row.get("locationId")));require(l!=null&&l.getLocationGroup()!=null&&l.getLocationGroup().getLocationGroupType()!=null,"Inventory location missing");Map<String,Object> location=new LinkedHashMap<>();location.put("id",l.getId());location.put("name",l.getName());Map<String,Object> group=new LinkedHashMap<>();Map<String,Object> type=new LinkedHashMap<>();LocationGroupType t=l.getLocationGroup().getLocationGroupType();type.put("shippingStage",t.getShippingStage());type.put("fourWallInventory",t.getFourWallInventory());type.put("dock",t.getDock());type.put("yard",t.getYard());type.put("virtual",t.getVirtual());group.put("locationGroupType",type);location.put("locationGroup",group);row.put("location",location);}
  return raw;
 }
 void authorize(List<Map<String,Object>> rows,ClientRestriction r){for(Map<String,Object> row:rows)access(row.get("clientId")==null?null:n(row.get("clientId")),r);}
 static String fingerprint(long w,long s,long d,List<Map<String,Object>> rows)throws Exception{
  StringBuilder text=new StringBuilder(w+"|"+s+"|"+d);Set<Long> seen=new HashSet<>();
  for(Map<String,Object> r:rows){require(seen.add(n(r.get("id"))),"Duplicate inventory in batch");for(String key:Arrays.asList("id","quantity","pickId","itemId","locationId"))text.append('|').append(n(r.get(key)));text.append('|').append(r.get("lpn")).append('|').append(r.get("clientId"));}
  return java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(text.toString().getBytes(StandardCharsets.UTF_8)));
 }
 public Map<String,Object> ship(long w,long s,Map<String,Object> request,ClientRestriction restriction)throws Exception{
  long started=System.nanoTime();scope(w,s);long destinationId=n(request.get("destinationId"));
  @SuppressWarnings("unchecked") List<Map<String,Object>> rows=(List<Map<String,Object>>)request.get("rows");
  require(rows!=null&&!rows.isEmpty()&&rows.size()<=5000,"Invalid batch size");rows=new ArrayList<>(rows);rows.sort(Comparator.comparingLong(r->n(r.get("id"))));authorize(rows,restriction);
  String hash=fingerprint(w,s,destinationId,rows),snapshot=json.writeValueAsString(rows);
  Location destination=layout.getLocationById(destinationId);
  require(destination!=null&&destination.getWarehouse()!=null&&Objects.equals(destination.getWarehouse().getId(),w)&&destination.getLocationGroup()!=null&&Boolean.TRUE.equals(destination.getLocationGroup().getLocationGroupType().getVirtual()),"Invalid batch shipping destination");
  List<String> orders=db.queryForList("SELECT DISTINCT o.number FROM shipment_line sl JOIN outbound_order_line ol ON ol.outbound_order_line_id=sl.outbound_order_line_id JOIN outbound_order o ON o.outbound_order_id=ol.outbound_order_id WHERE sl.shipment_id=?",String.class,s);
  require(orders.size()==1&&orders.get(0).equals(destination.getName()),"Destination does not match shipment order");
  final List<Map<String,Object>> submitted=rows;
  org.springframework.transaction.support.TransactionTemplate tx=new org.springframework.transaction.support.TransactionTemplate(transactionManager);
  tx.execute(status->{
   db.update("INSERT INTO shipment_inventory_batch(warehouse_id,shipment_id,destination_id,request_hash,snapshot_json) VALUES(?,?,?,?,?) ON DUPLICATE KEY UPDATE shipment_id=VALUES(shipment_id)",w,s,destinationId,hash,snapshot);
   Map<String,Object> prior=receipt(w,s,true).get(0);
   require(hash.equals(prior.get("request_hash"))&&n(prior.get("destination_id"))==destinationId,"Shipment batch changed; recovery requires original snapshot");
   if(prior.get("completed_at")==null&&!((String)prior.get("snapshot_json")).contains("\"batchShipped\":true")){
    List<Map<String,Object>> current=db.queryForList("SELECT i.inventory_id id,i.lpn,i.client_id clientId,i.quantity,i.pick_id pickId,i.item_id itemId,i.location_id locationId FROM inventory i JOIN pick p ON p.pick_id=i.pick_id JOIN shipment_line sl ON sl.shipment_line_id=p.shipment_line_id JOIN location l ON l.location_id=i.location_id JOIN location_group g ON g.location_group_id=l.location_group_id JOIN location_group_type t ON t.location_group_type_id=g.location_group_type_id WHERE sl.shipment_id=? AND i.warehouse_id=? AND t.shipping_stage_locations=TRUE ORDER BY i.inventory_id",s,w);
    try{require(fingerprint(w,s,destinationId,current).equals(hash),"Staged inventory set or quantities changed; refresh before completing");}catch(Exception e){throw new IllegalArgumentException("Invalid batch snapshot",e);}
   }
   return null;
  });
  Map<String,Object> prior=receipt(w,s,false).get(0);
  if(prior.get("completed_at")!=null)return result(decode((String)prior.get("snapshot_json")),true);
  long prepared=System.nanoTime();
  // Each existing stock move and its progress receipt commit together. A later
  // pallet failure leaves a resumable batch, never an unrecorded earlier move.
  for(Map<String,Object> row:submitted){
   tx.execute(status->{
    try {
     Map<String,Object> currentReceipt=receipt(w,s,true).get(0);
     require(hash.equals(currentReceipt.get("request_hash")),"Batch scope changed");
     List<Map<String,Object>> saved=decode((String)currentReceipt.get("snapshot_json"));
     Map<String,Object> savedRow=saved.stream().filter(r->n(r.get("id"))==n(row.get("id"))).findFirst().orElseThrow();
     if(Boolean.TRUE.equals(savedRow.get("batchShipped")))return null;
     Inventory i=em.find(Inventory.class,n(row.get("id")),LockModeType.PESSIMISTIC_WRITE);require(i!=null,"Inventory no longer exists");em.refresh(i,LockModeType.PESSIMISTIC_WRITE);
     require(Objects.equals(i.getWarehouseId(),w)&&Objects.equals(i.getQuantity(),n(row.get("quantity")))&&i.getQuantity()>0&&Objects.equals(i.getPickId(),n(row.get("pickId")))&&Objects.equals(i.getItemId(),n(row.get("itemId")))&&Objects.equals(i.getLocationId(),n(row.get("locationId"))),"Inventory changed; refresh before completing");
     access(i.getClientId(),restriction);require(Objects.equals(i.getLpn(),row.get("lpn"))&&Objects.equals(i.getClientId(),row.get("clientId")==null?null:n(row.get("clientId"))),"Inventory owner or LPN changed");
     Location source=layout.getLocationById(i.getLocationId());require(source!=null&&source.getLocationGroup()!=null&&Boolean.TRUE.equals(source.getLocationGroup().getLocationGroupType().getShippingStage()),"Inventory is no longer in shipping stage");
     require(db.queryForObject("SELECT COUNT(*) FROM pick p JOIN shipment_line sl ON sl.shipment_line_id=p.shipment_line_id WHERE p.pick_id=? AND sl.shipment_id=? AND p.quantity=p.picked_quantity",Long.class,i.getPickId(),s)==1,"Pick is not complete for this shipment");
     Inventory moved=inventory.shipInventory(i.getId(),destination);require(Objects.equals(moved.getId(),n(row.get("id")))&&Objects.equals(moved.getQuantity(),n(row.get("quantity")))&&Objects.equals(moved.getLocationId(),destinationId),"Shipping result mismatch");
     em.flush();savedRow.put("batchShipped",true);db.update("UPDATE shipment_inventory_batch SET snapshot_json=? WHERE warehouse_id=? AND shipment_id=?",json.writeValueAsString(saved),w,s);return null;
    }catch(Exception error){throw new IllegalStateException("Shipment batch stopped; retry with the same shipment to resume",error);}
   });
  }
  Map<String,Object> completed=tx.execute(status->{try{
   Map<String,Object> r=receipt(w,s,true).get(0);List<Map<String,Object>> saved=decode((String)r.get("snapshot_json"));require(saved.stream().allMatch(row->Boolean.TRUE.equals(row.get("batchShipped"))),"Batch has unfinished inventory");
   // Refuse to complete if new staged inventory appeared after the snapshot.
   require(db.queryForObject("SELECT COUNT(*) FROM inventory i JOIN pick p ON p.pick_id=i.pick_id JOIN shipment_line sl ON sl.shipment_line_id=p.shipment_line_id JOIN location l ON l.location_id=i.location_id JOIN location_group g ON g.location_group_id=l.location_group_id JOIN location_group_type t ON t.location_group_type_id=g.location_group_type_id WHERE sl.shipment_id=? AND i.warehouse_id=? AND t.shipping_stage_locations=TRUE",Long.class,s,w)==0,"Additional staged inventory exists; batch requires review");
   db.update("UPDATE shipment_inventory_batch SET completed_at=COALESCE(completed_at,UTC_TIMESTAMP(3)) WHERE warehouse_id=? AND shipment_id=?",w,s);return result(saved,false);
  }catch(Exception error){throw new IllegalStateException("Batch finalization failed; retry same shipment",error);}});
  org.slf4j.LoggerFactory.getLogger(getClass()).info("shipment-inventory-batch shipment={} rows={} prepareMs={} moveAndReceiptMs={} totalMs={}",s,submitted.size(),(prepared-started)/1000000,(System.nanoTime()-prepared)/1000000,(System.nanoTime()-started)/1000000);return completed;
 }
 Map<String,Object> result(List<Map<String,Object>> rows,boolean replay){Map<String,Object> r=new LinkedHashMap<>();r.put("schema","shipment-inventory-batch-v1");r.put("rows",rows);r.put("replayed",replay);return r;}
}
