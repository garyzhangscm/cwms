package com.garyzhangscm.cwms.inventory.service;
import com.garyzhangscm.cwms.inventory.model.*;
import com.garyzhangscm.cwms.inventory.repository.*;
import com.garyzhangscm.cwms.inventory.clients.*;
import com.garyzhangscm.cwms.inventory.exception.InventoryException;
import jakarta.persistence.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpMethod;
import java.util.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
@Service
public class ManualPickInventoryService {
 @org.springframework.beans.factory.annotation.Value("${manual.pick.fast-enabled:false}") private boolean fastEnabled;
 @Autowired private ManualPickFastScope fastScope;
 public boolean fastEnabled(){return fastEnabled;}
 @PersistenceContext private EntityManager em;
 @Autowired private InventoryService inventory;
 @Autowired private ManualPickInventoryOperationRepository operations;
 @Autowired private RestTemplateProxy rest;
 @Autowired private OutbuondServiceRestemplateClient outbound;
 @Autowired private WarehouseLayoutServiceRestemplateClient layout;
 @Transactional
 public Inventory apply(String requestId,Long warehouseId,Long sourceId,String sourceLpn,Long expectedQuantity,
       Long quantity,String newLpn,boolean keepOriginal,Long pickId,Location destination) throws Exception {
  UUID.fromString(requestId);
  String text=warehouseId+"|"+sourceId+"|"+sourceLpn+"|"+expectedQuantity+"|"+quantity+"|"+newLpn+"|"+keepOriginal+"|"+pickId+"|"+destination.getId();
  String fingerprint=java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8)));
  Optional<ManualPickInventoryOperation> prior=operations.findById(requestId);
  if(prior.isPresent()) return replay(prior.get(),fingerprint);
  // Recheck after the row lock: concurrent retries serialize with the original split.
  Inventory source=em.find(Inventory.class,sourceId,LockModeType.PESSIMISTIC_WRITE);
  if(source==null || !Objects.equals(source.getWarehouseId(),warehouseId)) throw bad("Source inventory not found");
  // A locking read sees a concurrently committed operation even under MySQL REPEATABLE READ.
  ManualPickInventoryOperation committed=em.find(ManualPickInventoryOperation.class,requestId,LockModeType.PESSIMISTIC_WRITE);
  if(committed!=null) return replay(committed,fingerprint);
  inventory.loadInventoryAttribute(source);
  if(!Objects.equals(source.getLpn(),sourceLpn) || !Objects.equals(source.getQuantity(),expectedQuantity)
     || quantity==null || quantity<=0 || quantity>source.getQuantity() || source.getPickId()!=null
     || (source.getAllocatedByPickId()!=null && !source.getAllocatedByPickId().equals(pickId))) throw bad("Inventory changed; refresh before picking");
  Pick pick=fastEnabled?verifiedPick(requestId,warehouseId,sourceId,sourceLpn,expectedQuantity,quantity,newLpn,keepOriginal,pickId,destination,source):outbound.getPickById(pickId);
  if(pick==null || !Objects.equals(pick.getItemId(),source.getItem().getId()) || !Objects.equals(pick.getSourceLocationId(),source.getLocationId())
      || pick.getQuantity()-pick.getPickedQuantity()!=quantity.longValue()) throw bad("Pick does not match inventory or quantity");
  Location target=layout.getLocationById(destination.getId());
  if(target==null || target.getWarehouse()==null || !Objects.equals(target.getWarehouse().getId(),warehouseId)) throw bad("Invalid destination warehouse");
  if(fastEnabled)requireCurrentLpnEligible(source);
  boolean partial=quantity<expectedQuantity;
  if(partial) {
   if(newLpn==null || !newLpn.matches("[A-Z][A-Z0-9]{7,11}") || newLpn.startsWith("DE") || newLpn.equals(sourceLpn)) throw bad("Scan a different ordinary preprinted LPN");
   Map label=rest.exchange(Map.class,"http://apigateway:5555/api/resource/preprinted-lpns/validate?warehouseId="+warehouseId+"&lpn="+newLpn,HttpMethod.GET,null);
   if(label==null || (!Boolean.TRUE.equals(label.get("issued")) || !requestId.equals(label.get("reservedFor")))) throw bad("LPN was not preprinted for this warehouse");
   inventory.validateLPN(warehouseId,newLpn,true);
   if(!inventory.validateNewLPN(warehouseId,newLpn).isEmpty()) throw bad("New LPN already has inventory");
  } else if(newLpn!=null && !newLpn.isEmpty()) throw bad("A full-pallet pick keeps its original LPN");
  Inventory picked=source;
  if(partial) {
   List<Inventory> split=inventory.splitInventory(source,newLpn,keepOriginal ? expectedQuantity-quantity : quantity);
   Inventory remaining=keepOriginal ? split.get(1) : split.get(0);
   picked=keepOriginal ? split.get(0) : split.get(1);
   remaining.setAllocatedByPickId(null);
   inventory.saveOrUpdate(remaining);
  }
  if(fastEnabled){final Inventory moving=picked;picked=fastScope.withPick(pick,()->inventory.moveInventory(moving,target,pickId,true,null));}
  else picked=inventory.moveInventory(picked,target,pickId,true,null);
  String expectedPickedLpn=partial&&!keepOriginal?newLpn:sourceLpn;
  if(!expectedPickedLpn.equals(picked.getLpn()) || !quantity.equals(picked.getQuantity()))
    throw bad("Destination consolidation changed the requested LPN or quantity; use a separate destination");
  ManualPickInventoryOperation op=new ManualPickInventoryOperation();op.requestId=requestId;op.warehouseId=warehouseId;
  op.newLpn=partial?newLpn:null;op.fingerprint=fingerprint;op.pickedInventoryId=picked.getId();op.pickedLpn=picked.getLpn();op.quantity=quantity;operations.saveAndFlush(op);
  return picked;
 }
 public static class SplitGuard {
  public String schema,requestId,sourceLpn,newLpn;public Long warehouseId,orderId,sourceInventoryId,sourceQuantity,destinationId,inventoryStatusId,clientId;public Boolean keepOriginal;public Pick pick;
 }
 private Pick verifiedPick(String requestId,Long warehouse,Long source,String sourceLpn,Long expected,Long quantity,String newLpn,boolean keep,Long pickId,Location target,Inventory stock) {
  SplitGuard guard=rest.exchange(SplitGuard.class,"http://apigateway:5555/api/outbound/picks/"+pickId+"/manual-split-check?warehouseId="+warehouse+"&requestId="+requestId,HttpMethod.GET,null);
  if(guard==null||!"manual-split-guard-v1".equals(guard.schema)||!requestId.equals(guard.requestId)||!warehouse.equals(guard.warehouseId)||!source.equals(guard.sourceInventoryId)||!sourceLpn.equals(guard.sourceLpn)||!expected.equals(guard.sourceQuantity)||!Objects.toString(newLpn,"").equals(guard.newLpn)||!Boolean.valueOf(keep).equals(guard.keepOriginal)||!target.getId().equals(guard.destinationId))throw bad("Saved split operation does not match this request");
  if(stock.getInventoryStatus()==null||!Objects.equals(guard.inventoryStatusId,stock.getInventoryStatus().getId())||!Objects.equals(guard.clientId,stock.getClientId()==null?0L:stock.getClientId()))throw bad("Inventory status or owner changed; refresh before picking");
  Pick pick=guard.pick;
  if(pick==null||!pickId.equals(pick.getId())||pick.getItemId()==null||pick.getSourceLocationId()==null||pick.getDestinationLocationId()==null||pick.getQuantity()==null||pick.getPickedQuantity()==null||pick.getPickMovements()==null||pick.getQuantity()<0||pick.getPickedQuantity()<0||pick.getQuantity()<pick.getPickedQuantity()||!quantity.equals(pick.getQuantity()-pick.getPickedQuantity()))throw bad("Saved split Pick is invalid");
  for(PickMovement hop:pick.getPickMovements())if(hop==null||hop.getLocationId()==null||hop.getSequence()==null)throw bad("Saved pick movement is invalid");
  return pick;
 }
 /** Recheck only this row while its write lock is held; no warehouse scan or capacity lookup. */
 private void requireCurrentLpnEligible(Inventory source) {
  if(source.getItem()==null||source.getInventoryStatus()==null||source.getLocationId()==null)throw bad("Source inventory details are missing");
  if(Boolean.TRUE.equals(source.getVirtual())||Boolean.TRUE.equals(source.getInboundQCRequired())||Boolean.TRUE.equals(source.getLockedForAdjust()))throw bad("This LPN is virtual, awaiting QC or locked for adjustment");
  // Preserve the existing InventoryService.isInventoryPickable lock predicate.
  if(source.getLocks()==null||source.getLocks().stream().anyMatch(link->link==null||link.getLock()==null||Boolean.TRUE.equals(link.getLock().getAllowPick())))throw bad("This LPN has an inventory lock that prevents picking");
  Location at=source.getLocation();
  if(at==null||!source.getLocationId().equals(at.getId())||at.getLocationGroup()==null||at.getLocationGroup().getLocationGroupType()==null)throw bad("Source location could not be verified");
  LocationGroupType type=at.getLocationGroup().getLocationGroupType();
  if(!Boolean.TRUE.equals(at.getEnabled())||!Boolean.TRUE.equals(at.getLocationGroup().getPickable())||!Boolean.TRUE.equals(type.getFourWallInventory()))throw bad("The current location does not permit picking");
  if(Boolean.TRUE.equals(type.getShippingStage())||Boolean.TRUE.equals(type.getDock())||Boolean.TRUE.equals(type.getTrailer())||Boolean.TRUE.equals(type.getYard()))throw bad("This LPN is in a shipping or loading location and cannot be picked again");
 }
 private Inventory replay(ManualPickInventoryOperation op,String fingerprint) {
  if(!op.fingerprint.equals(fingerprint)) throw bad("Request ID reused with different split details");
  Inventory result=new Inventory();result.setId(op.pickedInventoryId);result.setLpn(op.pickedLpn);result.setQuantity(op.quantity);return result;
 }
 private InventoryException bad(String message){return InventoryException.raiseException(message);}
}
