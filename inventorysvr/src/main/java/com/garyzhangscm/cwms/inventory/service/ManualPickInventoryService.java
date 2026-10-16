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
  Pick pick=outbound.getPickById(pickId);
  if(pick==null || !Objects.equals(pick.getItemId(),source.getItem().getId()) || !Objects.equals(pick.getSourceLocationId(),source.getLocationId())
      || pick.getQuantity()-pick.getPickedQuantity()!=quantity.longValue()) throw bad("Pick does not match inventory or quantity");
  Location target=layout.getLocationById(destination.getId());
  if(target==null || target.getWarehouse()==null || !Objects.equals(target.getWarehouse().getId(),warehouseId)) throw bad("Invalid destination warehouse");
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
  picked=inventory.moveInventory(picked,target,pickId,true,null);
  String expectedPickedLpn=partial&&!keepOriginal?newLpn:sourceLpn;
  if(!expectedPickedLpn.equals(picked.getLpn()) || !quantity.equals(picked.getQuantity()))
    throw bad("Destination consolidation changed the requested LPN or quantity; use a separate destination");
  ManualPickInventoryOperation op=new ManualPickInventoryOperation();op.requestId=requestId;op.warehouseId=warehouseId;
  op.newLpn=partial?newLpn:null;op.fingerprint=fingerprint;op.pickedInventoryId=picked.getId();op.pickedLpn=picked.getLpn();op.quantity=quantity;operations.saveAndFlush(op);
  return picked;
 }
 private Inventory replay(ManualPickInventoryOperation op,String fingerprint) {
  if(!op.fingerprint.equals(fingerprint)) throw bad("Request ID reused with different split details");
  Inventory result=new Inventory();result.setId(op.pickedInventoryId);result.setLpn(op.pickedLpn);result.setQuantity(op.quantity);return result;
 }
 private InventoryException bad(String message){return InventoryException.raiseException(message);}
}
