package com.garyzhangscm.cwms.outbound.service;
import com.garyzhangscm.cwms.outbound.model.*;
import com.garyzhangscm.cwms.outbound.repository.ManualPickOperationRepository;
import com.garyzhangscm.cwms.outbound.clients.*;
import com.garyzhangscm.cwms.outbound.exception.OrderOperationException;
import javax.persistence.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.web.util.UriComponentsBuilder;
import java.util.*;
@Service
public class PreprintedManualPickService {
 @org.springframework.beans.factory.annotation.Value("${manual.pick.fast-enabled:false}") private boolean fastEnabled;
 public boolean fastEnabled(){return fastEnabled;}
 @PersistenceContext private EntityManager em;
 @Autowired private ManualPickPreparationScope preparation;
 @Autowired private OrderService orders;
 @Autowired private PickService picks;
 @Autowired private InventoryServiceRestemplateClient inventory;
 @Autowired private WarehouseLayoutServiceRestemplateClient layout;
 @Autowired private RestTemplateProxy rest;
 @Autowired private ManualPickOperationRepository operations;
 @Autowired private PlatformTransactionManager transactions;
 private static class SourcePreview {final Inventory source;final long pickable;SourcePreview(Inventory source,long pickable){this.source=source;this.pickable=pickable;}}
 private SourcePreview inspect(Long warehouseId,Long orderId,String sourceLpn) {
  Order order=orders.findById(orderId);
  if(!Objects.equals(order.getWarehouseId(),warehouseId))throw bad("Order not found in warehouse");
  List<Inventory> rows=inventory.getInventoryByLpn(warehouseId,sourceLpn);
  if(rows.size()!=1)throw bad("This split flow requires a single inventory record on the LPN; ask a supervisor to consolidate mixed stock");
  long quantity=orders.getPickableQuantityForManualPick(order,sourceLpn,false);
  return new SourcePreview(rows.get(0),quantity);
 }
 public Map<String,Object> preview(Long warehouseId,Long orderId,String sourceLpn) {
  SourcePreview value=preparation.within(warehouseId,sourceLpn,()->inspect(warehouseId,orderId,sourceLpn));
  return Map.of("sourceQuantity",value.source.getQuantity(),"pickableQuantity",value.pickable,"sourceInventoryId",value.source.getId());
 }
 public ManualPickOperation process(String requestId,Long warehouseId,Long orderId,String sourceLpn,String newLpn,
                                   boolean keepOriginal,Long quantity,Long expectedQuantity,String destination) {
  ManualPickOperation op=prepare(requestId,warehouseId,orderId,sourceLpn,newLpn,keepOriginal,quantity,expectedQuantity,destination);
  return finish(op.requestId);
 }
 public ManualPickOperation prepare(String requestId,Long warehouseId,Long orderId,String sourceLpn,String newLpn,
                                   boolean keepOriginal,Long quantity,Long expectedQuantity,String destination) {
  UUID.fromString(requestId);
  if(sourceLpn==null || !sourceLpn.matches("[A-Za-z0-9-]{1,12}") || newLpn==null || destination==null || destination.length()>100) throw bad("Invalid picking details");
  TransactionTemplate tx=new TransactionTemplate(transactions);
  // Preparation is committed separately: order demand is reserved and retries keep the SAME pick.
  ManualPickOperation op=tx.execute(status->preparation.within(warehouseId,sourceLpn,()->{
   Order order=em.find(Order.class,orderId,LockModeType.PESSIMISTIC_WRITE);
   if(order==null || !Objects.equals(order.getWarehouseId(),warehouseId)) throw bad("Order not found in warehouse");
   Optional<ManualPickOperation> old=operations.findById(requestId);
   boolean toStage="@SHIP_STAGE".equals(destination);
   Location target=toStage?null:layout.getLocationByName(warehouseId,destination);
   if(!toStage && target==null) throw bad("Destination not found in warehouse");
   if(old.isPresent()) {
    ManualPickOperation prior=old.get();
    if(prior.cancelled) throw bad("This request was cancelled. Start a new pick.");
    if(!Objects.equals(prior.warehouseId,warehouseId)||!Objects.equals(prior.orderId,orderId)||!prior.sourceLpn.equals(sourceLpn)
       ||!Objects.equals(prior.newLpn,newLpn)||prior.keepOriginal!=keepOriginal||!Objects.equals(prior.quantity,quantity)
       ||!Objects.equals(prior.sourceQuantity,expectedQuantity)||(!toStage && !Objects.equals(prior.destinationId,target.getId()))) throw bad("Retry details differ; reuse the original operation");
    return prior;
   }
   SourcePreview data=inspect(warehouseId,orderId,sourceLpn);
   long source=data.source.getQuantity(),available=data.pickable;
   if(quantity==null||quantity<=0||quantity>available||expectedQuantity==null||source!=expectedQuantity.longValue()) throw bad("Order or inventory quantity changed; refresh before picking");
   if(quantity<source) {
    if(!newLpn.matches("[A-Z][A-Z0-9]{7,11}")||newLpn.startsWith("DE")||newLpn.equals(sourceLpn)) throw bad("Scan a different ordinary preprinted LPN");
    if(!fastEnabled){Map issued=rest.exchange(Map.class,"http://apigateway:5555/api/resource/preprinted-lpns/validate?warehouseId="+warehouseId+"&lpn="+newLpn,HttpMethod.GET,null);
    if(issued==null||!Boolean.TRUE.equals(issued.get("issued"))) throw bad("New LPN was not printed for this warehouse");}
    if(!inventory.getInventoryByLpn(warehouseId,newLpn).isEmpty()) throw bad("New LPN already has inventory");
    String claimUrl=UriComponentsBuilder.fromHttpUrl("http://apigateway:5555/api/resource/preprinted-lpns/claim")
      .queryParam("warehouseId",warehouseId).queryParam("lpn",newLpn).queryParam("requestId",requestId).toUriString();
    Map claim=rest.exchange(Map.class,claimUrl,HttpMethod.POST,null);
    if(fastEnabled&&(claim==null||!requestId.equals(claim.get("reservedFor"))))throw bad("New LPN claim could not be confirmed");
   } else if(!newLpn.isEmpty()) throw bad("A full pallet keeps its original LPN");
   Inventory sourceInventory=fastEnabled?data.source:inventory.getInventoryByLpn(warehouseId,sourceLpn).get(0);
   Location sourceLocation=layout.getLocationById(sourceInventory.getLocationId());
   List<Pick> created=orders.generateManualPick(order,sourceLocation,sourceLpn,sourceInventory.getItem().getId(),quantity);
   if(created.size()!=1 || !Objects.equals(created.get(0).getQuantity(),quantity)) throw bad("Unable to reserve the exact requested quantity");
   if(toStage) target=layout.getLocationById(created.get(0).getDestinationLocationId());
   if(target==null) throw bad("Shipping stage not found");
   ManualPickOperation value=new ManualPickOperation();value.requestId=requestId;value.warehouseId=warehouseId;value.orderId=orderId;
   value.sourceInventoryId=sourceInventory.getId();value.sourceLpn=sourceLpn;value.newLpn=newLpn;value.keepOriginal=keepOriginal;
   value.quantity=quantity;value.sourceQuantity=expectedQuantity;value.destinationId=target.getId();value.pickId=created.get(0).getId();value.completed=false;
   return operations.saveAndFlush(value);
  }));
  return op;
 }
 public ManualPickOperation finish(String requestId) {
  ManualPickOperation op=operations.findById(requestId).orElseThrow(()->bad("Saved Pick not found"));
  if(op.cancelled)throw bad("This request was cancelled");
  if(op.completed) return op;
  Long warehouseId=op.warehouseId;
  TransactionTemplate tx=new TransactionTemplate(transactions);
  // Inventory service commits both split sides + movement once. A timed-out response is safe to retry.
  Location target;
  if(preparation.enabled()){target=new Location();target.setId(op.destinationId);}else target=layout.getLocationById(op.destinationId);
  String url=UriComponentsBuilder.fromHttpUrl("http://apigateway:5555/api/inventory/manual-pick-split"+(fastEnabled?(preparation.enabled()&&op.quantity.equals(op.sourceQuantity)?"/whole-v1":"/compact-v1"):""))
   .queryParam("requestId",op.requestId).queryParam("warehouseId",warehouseId).queryParam("sourceId",op.sourceInventoryId)
   .queryParam("sourceLpn",op.sourceLpn).queryParam("expectedQuantity",op.sourceQuantity).queryParam("quantity",op.quantity)
   .queryParam("newLpn",op.newLpn).queryParam("keepOriginal",op.keepOriginal).queryParam("pickId",op.pickId).toUriString();
  Inventory moved=rest.exchange(Inventory.class,url,HttpMethod.POST,target);
  String expectedLpn=op.quantity<op.sourceQuantity&&!op.keepOriginal?op.newLpn:op.sourceLpn;
  if(fastEnabled&&(moved==null||moved.getId()==null||!op.quantity.equals(moved.getQuantity())||!expectedLpn.equals(moved.getLpn())))throw bad("Inventory movement response cannot be verified; resume the same saved request");
  return tx.execute(status->{
   ManualPickOperation locked=em.find(ManualPickOperation.class,requestId,LockModeType.PESSIMISTIC_WRITE);
   if(locked.completed) return locked;
   Pick pick=em.find(Pick.class,locked.pickId,LockModeType.PESSIMISTIC_WRITE);
   if(!Objects.equals(pick.getPickedQuantity(),0L)) throw bad("Pick was changed outside this operation; supervisor reconciliation required");
   picks.recordPreprintedManualPick(pick,locked.quantity,moved.getLpn(),requestId);
   locked.completed=true;return operations.saveAndFlush(locked);
  });
 }
 public Map<String,Object> cancelUnstarted(String requestId,Long warehouseId,Long orderId) {
  UUID.fromString(requestId);
  return new TransactionTemplate(transactions).execute(status->{
   Order order=em.find(Order.class,orderId,LockModeType.PESSIMISTIC_WRITE);
   if(order==null || !Objects.equals(order.getWarehouseId(),warehouseId)) throw bad("Order not found in warehouse");
   Optional<ManualPickOperation> old=operations.findById(requestId);
   if(old.isPresent()) {
    ManualPickOperation op=old.get();
    if(!Objects.equals(op.warehouseId,warehouseId)||!Objects.equals(op.orderId,orderId)) throw bad("Operation belongs to a different order");
    return Map.of("cancelled",op.cancelled,"completed",op.completed);
   }
   // Tombstone under the same order lock: a delayed POST cannot start after cancellation.
   ManualPickOperation op=new ManualPickOperation();op.requestId=requestId;op.warehouseId=warehouseId;op.orderId=orderId;
   op.sourceInventoryId=0L;op.sourceLpn="";op.quantity=0L;op.sourceQuantity=0L;op.destinationId=0L;
   op.cancelled=true;op.completed=false;op.keepOriginal=false;operations.saveAndFlush(op);
   return Map.of("cancelled",true,"completed",false);
  });
 }
 private OrderOperationException bad(String message){return OrderOperationException.raiseException(message);}
}
