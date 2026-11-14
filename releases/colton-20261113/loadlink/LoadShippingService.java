package com.garyzhangscm.cwms.outbound.loadlink;

import java.util.*;
import javax.sql.DataSource;
import javax.persistence.EntityManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.Scheduled;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.http.HttpMethod;
import com.garyzhangscm.cwms.outbound.model.*;
import com.garyzhangscm.cwms.outbound.service.OrderService;
import com.garyzhangscm.cwms.outbound.service.UserService;
import com.garyzhangscm.cwms.outbound.clients.RestTemplateProxy;
import com.garyzhangscm.cwms.outbound.clients.WarehouseLayoutServiceRestemplateClient;
import com.garyzhangscm.cwms.outbound.quantity.QuantityAllocationService;

/** Authenticated native shipping coordinator. No sandbox endpoints or copied-data writes. */
@Service
public class LoadShippingService {
 private static final Logger log = LoggerFactory.getLogger(LoadShippingService.class);
 @Value("${load.shipping.order-planning.enabled:false}") private boolean orderPlanningEnabled;
 private final LoadOrderPlanStore orderPlans;
 private final LoadShipmentLinkService planningLinks;
 @Autowired private OrderService orders;
 @Autowired private UserService users;
 @Autowired private WarehouseLayoutServiceRestemplateClient layout;
 @Autowired private RestTemplateProxy rest;
 @Autowired private QuantityAllocationService allocations;
 @Autowired private EntityManager em;
 @Value("${load.shipping.native.enabled:false}") private boolean enabled;
 private final LoadCreationStore loadCreation;
 private final YardVisitStore yard;
 private final DockAssignmentStore dockAssignments;
 private final JdbcTemplate db;
 private final TransactionTemplate tx;
 private final LoadShipmentLinkService links;
 private final ShipmentDepositReadiness readiness;
 private final LoadShortQuantityFinalizer shorts;
 public LoadShippingService(DataSource ds,PlatformTransactionManager manager){
  loadCreation=new LoadCreationStore(ds,manager);
  yard=new YardVisitStore(ds);
  dockAssignments=new DockAssignmentStore(ds);
  db=new JdbcTemplate(ds);tx=new TransactionTemplate(manager);
  readiness=new ShipmentDepositReadiness(ds);
  links=new LoadShipmentLinkService(ds,(w,s)->readiness.validate(w,s,true),manager);
  shorts=new LoadShortQuantityFinalizer(ds,manager);
  planningLinks=new LoadShipmentLinkService(ds,
      (warehouse, shipment) -> LoadOrderPlanStore.validatePlanningShipment(db,warehouse,shipment),manager);
  orderPlans=new LoadOrderPlanStore(db,planningLinks);
 }
 public Map<String,Object> getOrderPlans(long w,long id,ClientRestriction restriction) {
  actor(w,"allocate-order");
  load(w,id,false);
  if (!orderPlanningEnabled) return Map.of("enabled",false,"orders",List.of());
  access(w,rows(w,id),restriction);
  return Map.of("enabled",true,"orders",authorizedPlans(w,id,restriction));
 }
 private List<Map<String,Object>> authorizedPlans(long w,long id,ClientRestriction restriction) {
  var plans=orderPlans.list(w,id);
  for (var plan:plans) {
   if (plan.get("orderNumber")!=null) allocations.requireQueueOrderAccess(w,n(plan,"orderId"),restriction);
  }
  return plans;
 }
 public Map<String,Object> addOrderPlan(long w,long id,String orderNumber,ClientRestriction restriction) {
  String who=actor(w,"allocate-order");
  require(orderPlanningEnabled,"Sales Order planning is disabled.");
  require(orderNumber!=null && !orderNumber.isBlank() && orderNumber.length()<=100,"Enter a Sales Order number.");
  return tx.execute(st->{
   mutex(w);
   require("PLANNED".equals(load(w,id,true).get("status")),"Only a planned Load can change Sales Orders.");
   access(w,rows(w,id),restriction);
   authorizedPlans(w,id,restriction);
   long order=orderPlans.findOrder(w,orderNumber.trim());
   allocations.requireQueueOrderAccess(w,order,restriction);
   orderPlans.add(w,id,order,who);
   return Map.of("orders",authorizedPlans(w,id,restriction));
  });
 }
 public Map<String,Object> removeOrderPlan(long w,long id,long order,ClientRestriction restriction) {
  String who=actor(w,"allocate-order");
  require(orderPlanningEnabled,"Sales Order planning is disabled.");
  return tx.execute(st->{
   mutex(w);
   require("PLANNED".equals(load(w,id,true).get("status")),"Only a planned Load can change Sales Orders.");
   access(w,rows(w,id),restriction);
   authorizedPlans(w,id,restriction);
   orderPlans.remove(w,id,order,who);
   return Map.of("orders",authorizedPlans(w,id,restriction));
  });
 }
 @Scheduled(fixedDelayString="${load.shipping.order-planning.interval-ms:5000}")
 public void attachPlannedShipments() {
  if (!enabled || !orderPlanningEnabled) return;
  for (var plan:orderPlans.due()) {
   long w=n(plan,"warehouse_id"),id=n(plan,"load_id"),order=n(plan,"order_id");
   try {
    tx.executeWithoutResult(st->{
     mutex(w);
     if ("PLANNED".equals(load(w,id,true).get("status"))) orderPlans.reconcile(w,id,order,true);
    });
   } catch (RuntimeException failure) {
    orderPlans.defer(w,order);
    log.warn("Unable to attach Shipment for planned Sales Order {} on Load {}: {}",order,id,failure.getMessage());
   }
  }
 }
 public Map<String,Object> createUnassignedLoad(long warehouseId,String number) {
  String operator=actor(warehouseId,"allocate-order");
  long companyId=layout.getWarehouseById(warehouseId).getCompanyId();
  return loadCreation.create(warehouseId,companyId,number,operator);
 }
 public List<Map<String,Object>> yardVisits(long w,boolean history,ClientRestriction cr){
  actor(w,"allocate-order");List<Map<String,Object>> visits=yard.list(w,history);
  for(Map<String,Object> v:visits)if(n(v,"load_id")>0)access(w,rows(w,n(v,"load_id")),cr);return visits;
 }
 public Map<String,Object> yardCheckIn(long w,YardVisitStore.Request r,ClientRestriction cr){
  String who=actor(w,"allocate-order");long company=layout.getWarehouseById(w).getCompanyId();
  return tx.execute(st->{mutex(w);return yard.checkIn(w,company,r,who);});
 }
 public Map<String,Object> yardChange(long w,long id,String action,YardVisitStore.Request r,ClientRestriction cr){
  String who=actor(w,"allocate-order");require(r!=null,"Visit details are required.");
  return tx.execute(st->{mutex(w);
   Map<String,Object> v=one("SELECT * FROM yard_visit WHERE warehouse_id=? AND visit_id=? FOR UPDATE",w,id);
   if(n(v,"load_id")>0)access(w,rows(w,n(v,"load_id")),cr);
   if(r.loadId!=null){load(w,r.loadId,true);access(w,rows(w,r.loadId),cr);}
   return yard.change(w,id,action,r,who);
  });
 }
 public static class DockRequest { public long trailerId,revision; public Long dockId; }
 public List<Map<String,Object>> dockAssignments(long w,ClientRestriction cr){
  actor(w,"allocate-order");List<Map<String,Object>> result=dockAssignments.list(w);
  for(Map<String,Object> r:result)access(w,rows(w,n(r,"loadId")),cr);return result;
 }
 public Map<String,Object> assignDock(long w,long id,DockRequest r,ClientRestriction cr){
  String who=actor(w,"allocate-order");require(r!=null,"Dock selection is required.");
  return tx.execute(st->{mutex(w);load(w,id,true);access(w,rows(w,id),cr);require(db.queryForObject("SELECT COUNT(*) FROM yard_visit WHERE warehouse_id=? AND (active_trailer_id=? OR active_load_id=?)",Long.class,w,r.trailerId,id)==0,"This Trailer is checked in. Use the Yard visit to assign its Dock.");return dockAssignments.assign(w,id,r.trailerId,r.dockId,r.revision,who);});
 }
 public static class Line {
  public long shipmentId,orderId,orderLineId,ordered,toShip,shortQuantity;
  public String orderNumber,shipmentNumber;
 }
 public static class Confirmation {
  public boolean physicallyLoaded,acceptShort;
  public List<Line> lines;
 }
 public static class Links {public List<Long> expectedShipmentIds,shipmentIds;}
 private void require(boolean ok,String message){if(!ok)throw new IllegalArgumentException(message);}
 private long n(Map<String,Object> row,String field){Object v=row.get(field);return v==null?0:((Number)v).longValue();}
 private Map<String,Object> one(String sql,Object...args){List<Map<String,Object>> r=db.queryForList(sql,args);require(r.size()==1,"Record not found in this warehouse.");return r.get(0);}
 private String actor(long w,String permission){
  require(enabled,"Native Load shipping is not enabled.");
  String name=users.getCurrentUserName();require(name!=null&&!name.trim().isEmpty()&&name.length()<=50,"Authenticated operator is required.");
  Long company=layout.getWarehouseById(w).getCompanyId();User user=users.getCurrentUser(company);
  if(!Boolean.TRUE.equals(user.getAdmin())&&!Boolean.TRUE.equals(user.getSystemAdmin())){
   boolean allowed=false;
   for(Map p:rest.exchangeList(Map.class,"http://apigateway:5555/api/resource/users/permission/by-web-page?companyId="+company+"&warehouseId="+w+"&webPageUrl=/outbound/order",HttpMethod.GET,null)){
    if(p.get("permission") instanceof Map&&permission.equals(((Map)p.get("permission")).get("name"))) {
     require(Boolean.TRUE.equals(p.get("allowAccess")),"Permission required: "+permission);allowed=true;
    }
   }
   require(allowed,"Permission required: "+permission);
  }
  return name;
 }
 private void mutex(long w){
  db.update("INSERT IGNORE INTO quantity_allocation_mutex(warehouse_id) VALUES(?)",w);
  db.queryForObject("SELECT warehouse_id FROM quantity_allocation_mutex WHERE warehouse_id=? FOR UPDATE",Long.class,w);
 }
 private Map<String,Object> load(long w,long id,boolean lock){
  Map<String,Object> l=one("SELECT * FROM trailer_appointment WHERE warehouse_id=? AND trailer_appointment_id=?"+(lock?" FOR UPDATE":""),w,id);
  require("SHIPPING".equals(l.get("type")),"This is not an outbound Load.");return l;
 }
 private List<Map<String,Object>> rows(long w,long id){return rows(w,id,false);}
 private List<Map<String,Object>> rows(long w,long id,boolean lock){
  return db.queryForList("SELECT s.shipment_id,s.status shipment_status,sl.shipment_line_id,sl.outbound_order_line_id,sl.quantity,sl.open_quantity,sl.inprocess_quantity,sl.loaded_quantity,sl.shipped_quantity,ol.outbound_order_id,ol.expected_quantity,o.status order_status,o.number order_number,s.number shipment_number FROM stop t JOIN shipment s ON s.stop_id=t.stop_id AND s.warehouse_id=t.warehouse_id JOIN shipment_line sl ON sl.shipment_id=s.shipment_id AND sl.warehouse_id=s.warehouse_id JOIN outbound_order_line ol ON ol.outbound_order_line_id=sl.outbound_order_line_id AND ol.warehouse_id=sl.warehouse_id JOIN outbound_order o ON o.outbound_order_id=ol.outbound_order_id AND o.warehouse_id=ol.warehouse_id WHERE t.warehouse_id=? AND t.trailer_appointment_id=? ORDER BY s.shipment_id,sl.shipment_line_id"+(lock?" FOR UPDATE":""),w,id);
 }
 private void access(long w,List<Map<String,Object>> rows,ClientRestriction cr){
  Set<Long> ids=new TreeSet<>();
  for(Map<String,Object> r:rows)ids.add(n(r,"outbound_order_id"));
  for(long order:ids)allocations.requireQueueOrderAccess(w,order,cr);
 }
 private Line line(Map<String,Object> r){
  Line l=new Line();l.orderNumber=String.valueOf(r.get("order_number"));l.shipmentNumber=String.valueOf(r.get("shipment_number"));l.shipmentId=n(r,"shipment_id");l.orderId=n(r,"outbound_order_id");l.orderLineId=n(r,"outbound_order_line_id");l.ordered=n(r,"quantity");
  boolean done="DISPATCHED".equals(r.get("shipment_status"));
  l.toShip=done?n(r,"shipped_quantity"):n(r,"inprocess_quantity");l.shortQuantity=l.ordered-l.toShip;return l;
 }
 public Map<String,Object> preview(long w,long id,ClientRestriction cr){
  actor(w,"complete-order");Map<String,Object> l=load(w,id,false);List<Map<String,Object>> rows=rows(w,id);access(w,rows,cr);
  List<Line> lines=new ArrayList<>();for(Map<String,Object> r:rows)lines.add(line(r));
  Map<String,Object> result=new LinkedHashMap<>();result.put("loadId",id);result.put("number",l.get("number"));result.put("status",l.get("status"));result.put("lines",lines);
  if (orderPlanningEnabled && "PLANNED".equals(l.get("status"))) {
   var plans=authorizedPlans(w,id,cr);
   var linkedIds=new HashSet<Long>();
   for (var row:rows) linkedIds.add(n(row,"shipment_id"));
   var pending=new ArrayList<Map<String,Object>>();
   for (var plan:plans) {
    long shipmentId=n(plan,"shipmentId");
    if (linkedIds.contains(shipmentId)) {
     if (db.queryForObject("SELECT COUNT(*) FROM shipment_inventory_batch WHERE warehouse_id=? AND shipment_id=?",Long.class,w,shipmentId)>0) continue;
     try {
      readiness.validate(w,shipmentId,true,false);
      continue;
     } catch (IllegalArgumentException notReady) {
      // A linked Shipment still has to pass the same Deposit checks as confirmation.
     }
    }
    pending.add(plan);
   }
   result.put("pendingOrders",pending);
  }
  result.put("production",true);return result;
 }
 public Map<Long,Long> palletCounts(long warehouse,ClientRestriction restriction){
  actor(warehouse,"allocate-order");
  long company=layout.getWarehouseById(warehouse).getCompanyId();
  Map<Long,Long> counts=new LinkedHashMap<>();
  for(Map<String,Object> row:LoadPalletCounts.query(db,warehouse,company)){
   long loadId=n(row,"load_id");
   try {access(warehouse,rows(warehouse,loadId),restriction);}
   catch(com.garyzhangscm.cwms.outbound.exception.OrderOperationException e){continue;}
   counts.put(loadId,n(row,"pallet_count"));
  }
  return counts;
 }
 public Map<String,Object> shipmentChoices(long warehouse,long loadId,ClientRestriction restriction){
  actor(warehouse,"allocate-order");
  Map<String,Object> currentLoad=load(warehouse,loadId,false);
  require("PLANNED".equals(currentLoad.get("status")),"Only a planned Load can change shipments.");
  List<Map<String,Object>> stops=db.queryForList("SELECT stop_id,status FROM stop WHERE warehouse_id=? AND trailer_appointment_id=?",warehouse,loadId);
  require(stops.size()<=1,"This Load has multiple Stops. Use the legacy workflow.");
  Long stopId=stops.isEmpty()?null:n(stops.get(0),"stop_id");
  if(stopId!=null)require("PLANNED".equals(stops.get(0).get("status")),"Stop processing has started.");
  List<Map<String,Object>> linked=rows(warehouse,loadId);
  access(warehouse,linked,restriction);
  List<Long> selected=linked.stream().map(row->n(row,"shipment_id")).distinct().toList();
  List<Map<String,Object>> candidates=db.queryForList("""
    SELECT shipment_id,number,status FROM shipment
    WHERE warehouse_id=? AND (stop_id=? OR
      (stop_id IS NULL AND status IN ('INPROCESS','STAGED') AND EXISTS
        (SELECT 1 FROM shipment_line sl WHERE sl.warehouse_id=shipment.warehouse_id
         AND sl.shipment_id=shipment.shipment_id AND sl.inprocess_quantity>0)))
    ORDER BY number,shipment_id
    """,warehouse,stopId);
  List<Map<String,Object>> choices=new ArrayList<>();
  for(Map<String,Object> candidate:candidates){
   long shipmentId=n(candidate,"shipment_id");
   List<Map<String,Object>> ordersForShipment=db.queryForList("""
     SELECT DISTINCT o.outbound_order_id,o.number FROM shipment_line sl
     JOIN outbound_order_line ol ON ol.outbound_order_line_id=sl.outbound_order_line_id AND ol.warehouse_id=sl.warehouse_id
     JOIN outbound_order o ON o.outbound_order_id=ol.outbound_order_id AND o.warehouse_id=ol.warehouse_id
     WHERE sl.warehouse_id=? AND sl.shipment_id=? ORDER BY o.number
     """,warehouse,shipmentId);
   if(ordersForShipment.isEmpty())continue;
   try {
    for(Map<String,Object> order:ordersForShipment)
     allocations.requireQueueOrderAccess(warehouse,n(order,"outbound_order_id"),restriction);
   } catch(com.garyzhangscm.cwms.outbound.exception.OrderOperationException e) {
    if(selected.contains(shipmentId))throw e;
    continue;
   }
   Map<String,Long> quantity;
   try {quantity=readiness.validate(warehouse,shipmentId,true,false);}
   catch(IllegalArgumentException e){
    if(selected.contains(shipmentId) && orderPlanningEnabled && orderPlans.isPlannedShipment(warehouse,loadId,shipmentId)) {
     quantity=Map.of("toShip",0L,"shortQuantity",0L);
    } else {
     if(selected.contains(shipmentId))throw e;
     continue;
    }
   }
   choices.add(Map.of("id",shipmentId,"number",candidate.get("number"),"status",candidate.get("status"),
     "orderNumbers",ordersForShipment.stream().map(order->order.get("number")).toList(),
     "quantity",quantity.get("toShip"),"shortQuantity",quantity.get("shortQuantity")));
  }
  return Map.of("selectedIds",selected,"shipments",choices,"orderPlanning",orderPlanningEnabled);
 }
 public Map<String,Object> replace(long w,long id,Links request,ClientRestriction cr){
  String who=actor(w,"allocate-order");require(request!=null&&request.shipmentIds!=null&&request.expectedShipmentIds!=null,"Shipment selection is required.");
  return tx.execute(st->{mutex(w);load(w,id,true);
   Set<Long> all=new TreeSet<>();all.addAll(request.shipmentIds);all.addAll(request.expectedShipmentIds);
   require(all.size()<=500,"Select at most 500 Shipments.");
   for(Long sid:all){require(sid!=null,"Invalid Shipment ID.");for(Long oid:db.queryForList("SELECT DISTINCT ol.outbound_order_id FROM shipment_line sl JOIN outbound_order_line ol ON ol.outbound_order_line_id=sl.outbound_order_line_id WHERE sl.warehouse_id=? AND ol.warehouse_id=? AND sl.shipment_id=?",Long.class,w,w,sid))allocations.requireQueueOrderAccess(w,oid,cr);}
   if (orderPlanningEnabled) {
    for (long sid:all) {
     boolean wanted=request.shipmentIds.contains(sid);
     orderPlans.requireShipmentAssignment(w,id,sid,wanted);
     if (wanted && !request.expectedShipmentIds.contains(sid)) readiness.validate(w,sid,true);

    }
   }
   (orderPlanningEnabled ? planningLinks : links).replace(w,id,request.expectedShipmentIds,request.shipmentIds,who);return Map.of("loadId",id,"shipmentIds",request.shipmentIds);
  });
 }
 public Map<String,Object> confirm(long w,long id,Confirmation request,ClientRestriction cr){
  String who=actor(w,"complete-order");
  require(request!=null&&request.physicallyLoaded,"Confirm that the goods have physically been loaded.");
  require(request.lines!=null&&!request.lines.isEmpty()&&request.lines.size()<=500,"Review current Shipment quantities before confirming.");
  return tx.execute(st->{
   mutex(w);Map<String,Object> l=load(w,id,true);
   require(List.of("PLANNED","COMPLETED").contains(l.get("status")),"Load is not ready for confirmation.");
   List<Map<String,Object>> rows=rows(w,id,true);access(w,rows,cr);
   if (orderPlanningEnabled && "PLANNED".equals(l.get("status"))) {
    var linkedIds=new HashSet<Long>();
    for (var row:rows) linkedIds.add(n(row,"shipment_id"));
    require(authorizedPlans(w,id,cr).stream().allMatch(plan->linkedIds.contains(n(plan,"shipmentId"))),
        "Some Sales Orders are still awaiting a Shipment. Finish picking or remove them from this Load.");
   }
   require(rows.size()==request.lines.size(),"Load contents changed. Reload before confirming.");
   Map<Long,Line> expected=new TreeMap<>();for(Line e:request.lines)require(e!=null&&expected.put(e.orderLineId,e)==null,"Duplicate order line in confirmation.");
   TreeSet<Long> orderIds=new TreeSet<>(),shipmentIds=new TreeSet<>();
   for(Map<String,Object> row:rows){
    Line actual=line(row),e=expected.get(actual.orderLineId);
    require(e!=null&&e.shipmentId==actual.shipmentId&&e.orderId==actual.orderId&&e.ordered==actual.ordered&&e.toShip==actual.toShip&&e.shortQuantity==actual.shortQuantity,"Shipment quantities changed. Reload before confirming.");
    require(e.ordered>0&&e.toShip>0&&e.shortQuantity>=0&&Math.addExact(e.toShip,e.shortQuantity)==e.ordered,"Invalid shipment quantities.");
    require(e.shortQuantity==0||request.acceptShort,"Confirm the remaining quantity will close as short shipped.");
    require(n(row,"expected_quantity")==e.ordered,"Split order lines require the existing Shipment workflow.");
    require(db.queryForObject("SELECT COUNT(*) FROM shipment_line WHERE warehouse_id=? AND outbound_order_line_id=?",Long.class,w,e.orderLineId)==1,"Split shipments require the existing Shipment workflow.");
    orderIds.add(e.orderId);shipmentIds.add(e.shipmentId);
   }
   long total=0; for(Line e:request.lines)total=Math.addExact(total,e.toShip);
   require(total>0,"There are no deposited goods to ship.");
   // Preflight all orders before moving any inventory. Serialize with reservation/Pick coordination.
   for(Long oid:orderIds){
    one("SELECT outbound_order_id FROM outbound_order WHERE warehouse_id=? AND outbound_order_id=? FOR UPDATE",w,oid);
    require(db.queryForObject("SELECT COUNT(*) FROM manual_pick_queue WHERE warehouse_id=? AND order_id=? AND state IN ('QUEUED','NEEDS_REVIEW')",Long.class,w,oid)==0,"A pallet is still processing or needs review.");
    require(db.queryForObject("SELECT COALESCE(SUM(held),0) FROM quantity_allocation_line WHERE warehouse_id=? AND order_id=?",Long.class,w,oid)==0,"Release remaining reservations in Allocate before loading.");
    List<Long> siblings=db.queryForList("SELECT DISTINCT sl.shipment_id FROM shipment_line sl JOIN outbound_order_line ol ON ol.outbound_order_line_id=sl.outbound_order_line_id WHERE ol.warehouse_id=? AND ol.outbound_order_id=?",Long.class,w,oid);
    require(siblings.size()==1&&shipmentIds.containsAll(siblings),"Use the existing Shipment workflow for split orders.");
   }
   for(Long sid:shipmentIds){
    Map<String,Object> ship=one("SELECT * FROM shipment WHERE warehouse_id=? AND shipment_id=? FOR UPDATE",w,sid);
    if(!"DISPATCHED".equals(ship.get("status"))){
     long saved=db.queryForObject("SELECT COUNT(*) FROM shipment_inventory_batch WHERE warehouse_id=? AND shipment_id=?",Long.class,w,sid);
     if(saved==0)readiness.validate(w,sid,true);
     // An existing native batch is validated and resumed by ShipmentCompletionBatchService.
    }
   }
   for(Long oid:orderIds){
    Map<String,Object> order=one("SELECT status FROM outbound_order WHERE warehouse_id=? AND outbound_order_id=?",w,oid);
    if(!"COMPLETE".equals(order.get("status")))orders.completeOrder(oid,orders.findById(oid,false));
    em.flush();em.clear();
    for(Line e:request.lines)if(e.orderId==oid&&e.shortQuantity>0)shorts.finish(w,oid,e.shipmentId,e.orderLineId,e.toShip,e.shortQuantity,who);
   }
   for(Line e:request.lines){
    Map<String,Object> sl=one("SELECT * FROM shipment_line WHERE warehouse_id=? AND shipment_id=? AND outbound_order_line_id=?",w,e.shipmentId,e.orderLineId);
    require(n(sl,"shipped_quantity")==e.toShip&&n(sl,"open_quantity")==0&&n(sl,"inprocess_quantity")==0,"Shipment quantities did not reconcile.");
   }
   db.update("UPDATE stop SET status='COMPLETED',last_modified_by=?,last_modified_time=CURRENT_TIMESTAMP WHERE warehouse_id=? AND trailer_appointment_id=?",who,w,id);
   db.update("UPDATE trailer_appointment SET status='COMPLETED',completed_time=COALESCE(completed_time,CURRENT_TIMESTAMP),last_modified_by=?,last_modified_time=CURRENT_TIMESTAMP WHERE warehouse_id=? AND trailer_appointment_id=?",who,w,id);
   return Map.of("loadId",id,"status","COMPLETED","lines",request.lines,"dispatchedTrailer",false);
  });
 }
}
