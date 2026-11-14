package com.garyzhangscm.cwms.outbound.wavereserve;
import static com.garyzhangscm.cwms.outbound.wavereserve.WaveReserveStore.*;
import com.garyzhangscm.cwms.outbound.model.*;
import com.garyzhangscm.cwms.outbound.service.*;
import com.garyzhangscm.cwms.outbound.quantity.*;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
import javax.sql.DataSource;
import javax.servlet.http.HttpServletRequest;
import java.util.*;
import org.springframework.context.annotation.Lazy;
import java.util.function.Supplier;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class WaveReserveService {
 @Autowired @Lazy QuantityAllocationService quantities;
 @Autowired @Lazy WaveService waves;
 @Autowired @Lazy PreprintedManualPickService manual;
 @Autowired @Lazy ShipmentLineService shipmentLines;
 @Autowired @Lazy PickService picks;
 @Autowired @Lazy OrderLineService orderLines;
 @Value("${wave.reserve.preview-token:}") String token;
 public final WaveReserveStore store;
 final ThreadLocal<Long> scope=new ThreadLocal<>();
 final ThreadLocal<Long> shipmentScope=new ThreadLocal<>();
 final ThreadLocal<Boolean> availability=ThreadLocal.withInitial(()->false);
 final ThreadLocal<Boolean> planning=ThreadLocal.withInitial(()->false);
 public WaveReserveService(DataSource ds){store=new WaveReserveStore(new JdbcTemplate(ds));}
 public WaveReserveStore store(){return store;}
 public Long currentWave(){return scope.get();}
 public boolean isAvailability(){return availability.get();}
 public boolean isPlanning(){return planning.get();}
 public void gate(HttpServletRequest r){require(token.length()>=32&&MessageDigest.isEqual(token.getBytes(StandardCharsets.UTF_8),String.valueOf(r.getHeader("X-MES-Wave-Preview")).getBytes(StandardCharsets.UTF_8)),"Wave Reserve is available only in the 4200 preview");}
 public <T>T within(long wave,long sl,Supplier<T> call){Long old=scope.get(),line=shipmentScope.get();scope.set(wave);shipmentScope.set(sl);try{return call.get();}finally{if(old==null)scope.remove();else scope.set(old);if(line==null)shipmentScope.remove();else shipmentScope.set(line);}}
 public void access(long w,long wave,ClientRestriction cr){
  require(store.db.queryForObject("SELECT COUNT(*) FROM wave WHERE warehouse_id=? AND wave_id=?",Long.class,w,wave)==1,"Wave not found in this warehouse");
  for(var r:store.lines(w,wave))quantities.requireQueueOrderAccess(w,n(r,"order_id"),cr);
 }
 public Map<String,Object> view(long w,long wave,ClientRestriction cr){access(w,wave,cr);var runs=store.db.queryForList("SELECT state,last_error,updated_at FROM wave_reserve_run WHERE warehouse_id=? AND wave_id=?",w,wave);var result=new LinkedHashMap<String,Object>();result.put("managed",!runs.isEmpty());result.put("waveId",wave);if(!runs.isEmpty()){result.putAll(runs.get(0));result.put("lines",store.lines(w,wave));result.put("pendingScans",store.db.queryForList("SELECT m.request_id,m.source_lpn,m.new_lpn,m.keep_original,m.quantity,m.source_quantity,s.shipment_line_id FROM wave_reserve_scan s JOIN manual_pick_operation m ON m.request_id=s.request_id WHERE s.warehouse_id=? AND s.wave_id=? AND m.completed=FALSE AND m.cancelled=FALSE",w,wave));}return result;}
 public Map<String,Object> list(long w,String ids,ClientRestriction cr){var out=new LinkedHashMap<String,Object>();require(ids!=null&&ids.matches("[0-9,]{1,2000}"),"Invalid Wave list");var distinct=new LinkedHashSet<String>(Arrays.asList(ids.split(",")));require(distinct.size()<=100,"At most 100 Waves");for(String id:distinct){long wave=Long.parseLong(id);out.put(id,view(w,wave,cr));}return out;}
 public Wave plan(long w,String number,String comment,List<Long> ids,boolean shipments,ClientRestriction cr){
  quantities.requireAllocationPermission(w);require(ids!=null&&!ids.isEmpty()&&ids.size()<=500&&!ids.contains(null),"Select 1 to 500 lines");
  return quantities.locked(w,()->{
   if(number!=null&&!number.isBlank())for(var r:store.db.queryForList("SELECT wave_id,status FROM wave WHERE warehouse_id=? AND number=?",w,number)){
    long existing=n(r,"wave_id");require(store.managed(w,existing),"Existing legacy Wave requires a reviewed migration; create a new Reserve Wave");
    require(!List.of("COMPLETED","CANCELLED","ALLOCATING").contains(r.get("status")),"Cannot change a closed Wave");
    require(store.db.queryForObject("SELECT COUNT(*) FROM wave_reserve_claim WHERE warehouse_id=? AND wave_id=?",Long.class,w,existing)==0,"Release this Wave before adding lines");
    require(store.db.queryForObject("SELECT COUNT(*) FROM pick p JOIN shipment_line sl ON sl.shipment_line_id=p.shipment_line_id WHERE sl.warehouse_id=? AND sl.wave_id=? AND p.quantity>0",Long.class,w,existing)==0,"Cannot add lines after picking has started");
   }
   var orderIds=new TreeSet<Long>();
   for(Long id:new LinkedHashSet<>(ids)){
    String sql=shipments?"SELECT ol.outbound_order_id order_id FROM shipment_line sl JOIN outbound_order_line ol ON ol.outbound_order_line_id=sl.outbound_order_line_id AND ol.warehouse_id=sl.warehouse_id WHERE sl.warehouse_id=? AND sl.shipment_line_id=?":"SELECT outbound_order_id order_id FROM outbound_order_line WHERE warehouse_id=? AND outbound_order_line_id=?";
    if(!shipments)require(store.db.queryForObject("SELECT COUNT(*) FROM shipment_line WHERE warehouse_id=? AND outbound_order_line_id=? AND status<>'CANCELLED'",Long.class,w,id)==0,"This order line already has a Shipment. Select its Shipment line instead of planning it again.");
    var rows=store.db.queryForList(sql,w,id);require(rows.size()==1,"Selected line not found in warehouse");orderIds.add(n(rows.get(0),"order_id"));
   }
   for(long order:orderIds){quantities.requireQueueOrderAccess(w,order,cr);var mode=quantities.entry(w,order,cr);require("ITEM_QUANTITY".equals(mode.get("mode")),"Selected order uses Classic allocation; it cannot be mixed into a Reserve Wave");}
   planning.set(true);try{
    Wave result=shipments?waves.planWaveByShipmentLines(w,number,ids,comment):waves.planWaveByOrderLines(w,number,ids,comment);
    store.db.update("INSERT INTO wave_reserve_run(warehouse_id,wave_id,state) VALUES(?,?,'PLANNED') ON DUPLICATE KEY UPDATE state='PLANNED',last_error=NULL",w,result.getId());return result;
   }finally{planning.remove();}
  });
 }
 public Map<String,Object> allocate(long w,long wave,String requestId,ClientRestriction cr){
  UUID.fromString(requestId);quantities.requireAllocationPermission(w);access(w,wave,cr);require(store.managed(w,wave),"Legacy Wave cannot be allocated through Reserve without a reviewed migration");
  try{return quantities.locked(w,()->{
   access(w,wave,cr);var previous=store.db.queryForList("SELECT warehouse_id,wave_id FROM wave_reserve_request WHERE request_id=?",requestId);if(!previous.isEmpty()){require(n(previous.get(0),"warehouse_id")==w&&n(previous.get(0),"wave_id")==wave,"Request ID belongs to another Wave");return view(w,wave,cr);}String status=store.db.queryForObject("SELECT status FROM wave WHERE warehouse_id=? AND wave_id=? FOR UPDATE",String.class,w,wave);
   require(!List.of("COMPLETED","CANCELLED","ALLOCATING").contains(status),"Wave is closed or has unresolved legacy allocation");
   Map<Long,Long> targets=store.targets(w,wave);var requests=new TreeMap<Long,QuantityAllocationService.Request>();
   for(var e:targets.entrySet()){
    var q=store.db.queryForMap("SELECT * FROM quantity_allocation_line WHERE warehouse_id=? AND order_line_id=?",w,e.getKey());
    if(e.getValue()==n(q,"held"))continue;
    long order=n(q,"order_id");var request=requests.computeIfAbsent(order,id->{var value=new QuantityAllocationService.Request();value.requestId=UUID.randomUUID().toString();value.lines=new ArrayList<>();return value;});
    var update=new QuantityAllocationService.Update();update.orderLineId=e.getKey();update.held=e.getValue();update.revision=n(q,"revision");request.lines.add(update);
   }
   for(var e:requests.entrySet())quantities.update(w,e.getKey(),cr,e.getValue());
   store.assign(w,wave);store.db.update("INSERT INTO wave_reserve_request(request_id,warehouse_id,wave_id) VALUES(?,?,?)",requestId,w,wave);store.db.update("UPDATE wave_reserve_run SET state='READY',last_error=NULL WHERE warehouse_id=? AND wave_id=?",w,wave);
   // Native ALLOCATED now represents prepared quantity, never an automatic physical Pick.
   store.db.update("UPDATE wave SET status='ALLOCATED' WHERE warehouse_id=? AND wave_id=?",w,wave);return view(w,wave,cr);
  });}catch(RuntimeException e){String message=String.valueOf(e.getMessage());store.db.update("UPDATE wave_reserve_run SET state=CASE WHEN state IN ('PLANNED','FAILED') THEN 'FAILED' ELSE state END,last_error=? WHERE warehouse_id=? AND wave_id=?",message.substring(0,Math.min(message.length(),900)),w,wave);throw e;}
 }
 public Map<String,Object> release(long w,long wave,ClientRestriction cr){quantities.requireAllocationPermission(w);return quantities.locked(w,()->{access(w,wave,cr);require(store.managed(w,wave),"Not a Reserve Wave");require(!List.of("COMPLETED","CANCELLED").contains(store.db.queryForObject("SELECT status FROM wave WHERE warehouse_id=? AND wave_id=?",String.class,w,wave)),"Wave is closed");require(store.db.queryForObject("SELECT COUNT(*) FROM manual_pick_operation m JOIN pick p ON p.pick_id=m.pick_id JOIN shipment_line sl ON sl.shipment_line_id=p.shipment_line_id WHERE sl.warehouse_id=? AND sl.wave_id=? AND m.completed=FALSE AND m.cancelled=FALSE",Long.class,w,wave)==0,"Resume or cancel pending Pick operations before releasing this Wave");store.release(w,wave);store.db.update("UPDATE wave_reserve_run SET state='RELEASED',last_error=NULL WHERE warehouse_id=? AND wave_id=?",w,wave);return view(w,wave,cr);});}
 public Map<String,Object> preview(long w,long wave,long sl,String lpn,ClientRestriction cr){return quantities.locked(w,()->{access(w,wave,cr);var row=line(w,wave,sl);var result=new LinkedHashMap<String,Object>(within(wave,sl,()->manual.preview(w,n(row,"order_id"),lpn)));result.put("pickableQuantity",Math.min(n(result,"pickableQuantity"),n(row,"wave_reserved")));return result;});}
 Map<String,Object> line(long w,long wave,long sl){require(store.managed(w,wave),"Not a Reserve Wave");return store.lines(w,wave).stream().filter(r->n(r,"shipment_line_id")==sl).findFirst().orElseThrow(()->new IllegalArgumentException("Shipment line does not belong to this Wave"));}
 public static class Scan {public String requestId,sourceLpn,newLpn="",destination="@SHIP_STAGE";public boolean keepOriginal=true;public long shipmentLineId,quantity,expectedQuantity;}
 static String fingerprint(Scan s){try{byte[] b=new ObjectMapper().writeValueAsBytes(s);return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(b));}catch(Exception e){throw new IllegalArgumentException("Invalid scan",e);}}
 public ManualPickOperation scan(long w,long wave,Scan request,ClientRestriction cr){
  quantities.requireAllocationPermission(w);require("@SHIP_STAGE".equals(request.destination),"Wave picking uses the Shipment shipping stage");UUID.fromString(request.requestId);String hash=fingerprint(request);
  ManualPickOperation op=quantities.locked(w,()->{
   access(w,wave,cr);var row=line(w,wave,request.shipmentLineId);var saved=store.db.queryForList("SELECT * FROM wave_reserve_scan WHERE request_id=?",request.requestId);
   if(!saved.isEmpty()){var old=saved.get(0);require(n(old,"warehouse_id")==w&&n(old,"wave_id")==wave&&n(old,"shipment_line_id")==request.shipmentLineId&&hash.equals(old.get("fingerprint")),"Retry details changed; resume the original scan");}
   else {require(store.db.queryForObject("SELECT COUNT(*) FROM manual_pick_operation WHERE request_id=?",Long.class,request.requestId)==0,"Request ID belongs to another operation");store.validateConversion(w,n(row,"order_line_id"),request.shipmentLineId,request.quantity,wave);store.db.update("INSERT INTO wave_reserve_scan(request_id,warehouse_id,wave_id,shipment_line_id,fingerprint) VALUES(?,?,?,?,?)",request.requestId,w,wave,request.shipmentLineId,hash);}
   return within(wave,request.shipmentLineId,()->manual.prepare(request.requestId,w,n(row,"order_id"),request.sourceLpn,request.newLpn,request.keepOriginal,request.quantity,request.expectedQuantity,request.destination));
  });
  return manual.finish(op.requestId);
 }

 public Map<String,Object> cancelScan(long w,long wave,long sl,String requestId,ClientRestriction cr){quantities.requireAllocationPermission(w);UUID.fromString(requestId);return quantities.locked(w,()->{access(w,wave,cr);var r=line(w,wave,sl);var existing=store.db.queryForList("SELECT warehouse_id,wave_id,shipment_line_id FROM wave_reserve_scan WHERE request_id=?",requestId);if(!existing.isEmpty()){var saved=existing.get(0);require(n(saved,"warehouse_id")==w&&n(saved,"wave_id")==wave&&n(saved,"shipment_line_id")==sl,"Request belongs to another Wave line");}return manual.cancelUnstarted(requestId,w,n(r,"order_id"));});}
 /** Cancel grouping and release only its share of native held Reserve, in one warehouse transaction. */
 public Map<String,Object> cancelAndRelease(long w,long wave,ClientRestriction cr){
  quantities.requireAllocationPermission(w);
  return quantities.locked(w,()->{
   access(w,wave,cr);require(store.managed(w,wave),"Not a Reserve Wave");
   String status=store.db.queryForObject("SELECT status FROM wave WHERE warehouse_id=? AND wave_id=? FOR UPDATE",String.class,w,wave);
   if("CANCELLED".equals(status))return view(w,wave,cr);
   require(!"COMPLETED".equals(status),"Completed Wave cannot be cancelled");
   require(store.db.queryForObject("SELECT COUNT(*) FROM shipment_line sl WHERE sl.warehouse_id=? AND sl.wave_id=? AND (sl.inprocess_quantity>0 OR sl.loaded_quantity>0 OR sl.shipped_quantity>0)",Long.class,w,wave)==0,"Reverse picking/loading before cancelling this Wave");
   require(store.db.queryForObject("SELECT COUNT(*) FROM pick p JOIN shipment_line sl ON sl.shipment_line_id=p.shipment_line_id WHERE sl.warehouse_id=? AND sl.wave_id=? AND p.quantity>0",Long.class,w,wave)==0,"Cancel or reconcile Picks before cancelling this Wave");
   var rows=store.lines(w,wave);var amounts=new TreeMap<Long,Long>();var orders=new TreeSet<Long>();
   for(var row:rows){require(n(row,"open_quantity")>=0,"Invalid open quantity");amounts.merge(n(row,"order_line_id"),n(row,"open_quantity"),Math::addExact);orders.add(n(row,"order_id"));}
   for(long order:orders)require(store.db.queryForObject("SELECT COUNT(*) FROM manual_pick_operation WHERE warehouse_id=? AND order_id=? AND completed=FALSE AND cancelled=FALSE",Long.class,w,order)==0,"Resume or cancel pending Pick requests for this order first");
   store.release(w,wave);
   var requests=new TreeMap<Long,QuantityAllocationService.Request>();long released=0;
   for(var entry:amounts.entrySet()){
    var q=store.db.queryForMap("SELECT * FROM quantity_allocation_line WHERE warehouse_id=? AND order_line_id=?",w,entry.getKey());
    long held=n(q,"held"),other=store.claimed(w,entry.getKey());require(held>=other,"Order Reserve is below other Wave claims; reconcile first");
    long target=Math.max(other,Math.max(0,held-entry.getValue()));
    if(target==held)continue;
    released=Math.addExact(released,held-target);
    var request=requests.computeIfAbsent(n(q,"order_id"),id->{var r=new QuantityAllocationService.Request();r.requestId=UUID.randomUUID().toString();r.lines=new ArrayList<>();return r;});
    var update=new QuantityAllocationService.Update();update.orderLineId=entry.getKey();update.held=target;update.revision=n(q,"revision");request.lines.add(update);
   }
   for(var entry:requests.entrySet())quantities.update(w,entry.getKey(),cr,entry.getValue());
   var result=new LinkedHashMap<String,Object>(close(w,wave,true,cr));result.put("releasedQuantity",released);return result;
  });
 }
 public Map<String,Object> close(long w,long wave,boolean cancel,ClientRestriction cr){quantities.requireAllocationPermission(w);return quantities.locked(w,()->{access(w,wave,cr);require(store.managed(w,wave),"Not a Reserve Wave");
  if(cancel){require(store.db.queryForObject("SELECT COUNT(*) FROM pick p JOIN shipment_line sl ON sl.shipment_line_id=p.shipment_line_id WHERE sl.warehouse_id=? AND sl.wave_id=? AND p.quantity>0",Long.class,w,wave)==0,"Cancel or reconcile Picks before cancelling the Wave");release(w,wave,cr);store.db.update("UPDATE shipment_line SET wave_id=NULL WHERE warehouse_id=? AND wave_id=?",w,wave);}
  else {var rows=store.lines(w,wave);require(!rows.isEmpty()&&rows.stream().allMatch(r->n(r,"quantity")>0&&n(r,"shipped_quantity")==n(r,"quantity")),"Dispatch all Shipments before closing the Wave. Completing a Wave does not ship inventory.");store.release(w,wave);}
  String state=cancel?"CANCELLED":"COMPLETED";store.db.update("UPDATE wave SET status=? WHERE warehouse_id=? AND wave_id=?",state,w,wave);store.db.update("UPDATE wave_reserve_run SET state=?,last_error=NULL WHERE warehouse_id=? AND wave_id=?",state,w,wave);return view(w,wave,cr);
 });}
 public Order scopedReadOrder(Order original){
  var row=line(original.getWarehouseId(),scope.get(),shipmentScope.get());require(n(row,"order_id")==original.getId(),"Order does not match selected Wave line");
  Order copy=new Order();org.springframework.beans.BeanUtils.copyProperties(original,copy,"orderLines");copy.setOrderLines(List.of(shipmentLines.findById(shipmentScope.get()).getOrderLine()));return copy;
 }
 public List<Shipment> scopedReadShipments(Order order){
  var row=line(order.getWarehouseId(),scope.get(),shipmentScope.get());require(n(row,"order_id")==order.getId(),"Order does not match selected Wave line");
  ShipmentLine sl=shipmentLines.findById(shipmentScope.get());Shipment copy=new Shipment();copy.setId(sl.getShipment().getId());copy.setWarehouseId(order.getWarehouseId());copy.setStatus(sl.getShipment().getStatus());copy.setShipmentLines(List.of(sl));return List.of(copy);
 }
 public <T>T readingAvailability(Supplier<T> call){boolean old=availability.get();availability.set(true);try{return call.get();}finally{if(old)availability.set(true);else availability.remove();}}
 public List<Pick> generateScoped(Order order,Location source,String lpn,Long item,Long quantity){
  long slId=shipmentScope.get();var r=line(order.getWarehouseId(),scope.get(),slId);require(n(r,"order_id")==order.getId()&&n(r,"item_id")==item&&quantity<=n(r,"open_quantity"),"Scanned LPN or quantity does not match this Shipment line");
  ShipmentLine sl=shipmentLines.findById(slId);orderLines.loadOrderLineAttribute(sl.getOrderLine());return picks.generateManualPickForOutboundShipment(sl,source,lpn,quantity).getPicks();
 }
}
