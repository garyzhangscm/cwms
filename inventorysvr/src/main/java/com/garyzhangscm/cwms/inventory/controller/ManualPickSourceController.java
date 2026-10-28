package com.garyzhangscm.cwms.inventory.controller;
import com.garyzhangscm.cwms.inventory.model.*;
import com.garyzhangscm.cwms.inventory.service.*;
import com.garyzhangscm.cwms.inventory.exception.InventoryException;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
@RestController
public class ManualPickSourceController {
 @Autowired private InventoryService inventory;
 @Autowired private ManualPickAuditNumberScope numbers;
 @GetMapping("/manual-pick-split/pipeline-capabilities") public Map<String,Object> capabilities(){return Map.of("schema","manual-pick-pipeline-v1","batchAuditNumbers",numbers.enabled(),"compactSource",true);}
 @GetMapping("/manual-pick-split/source-v1") @ClientValidationEndpoint @Transactional(readOnly=true)
 public List<Map<String,Object>> source(@RequestParam Long warehouseId,@RequestParam String lpn,ClientRestriction restriction){
  if(warehouseId==null||warehouseId<=0||lpn==null||!lpn.matches("[A-Za-z0-9-]{1,12}"))throw InventoryException.raiseException("Invalid source LPN scope");
  List<Inventory> rows=inventory.findByLpn(warehouseId,lpn,false);List<Map<String,Object>> result=new ArrayList<>();
  for(Inventory row:rows){
   if(!warehouseId.equals(row.getWarehouseId())||!lpn.equals(row.getLpn()))throw InventoryException.raiseException("Source inventory scope changed");
   Long client=row.getClientId();
   if(restriction!=null&&Boolean.TRUE.equals(restriction.getThreePartyLogisticsFlag())&&!Boolean.TRUE.equals(restriction.getAllClientAccess())){
    boolean allowed=client==null?Boolean.TRUE.equals(restriction.getNonClientDataAccessible()):restriction.getClientAccesses()!=null&&Arrays.stream(restriction.getClientAccesses().split(",")).anyMatch(v->v.trim().equals(client.toString()));
    if(!allowed)throw InventoryException.raiseException("Source inventory is outside the permitted client scope");
   }
   Map<String,Object> value=new LinkedHashMap<>();value.put("id",row.getId());value.put("warehouseId",row.getWarehouseId());value.put("lpn",row.getLpn());value.put("quantity",row.getQuantity());value.put("locationId",row.getLocationId());value.put("clientId",client);value.put("pickId",row.getPickId());value.put("allocatedByPickId",row.getAllocatedByPickId());value.put("virtual",row.getVirtual());
   if(row.getItem()==null||row.getInventoryStatus()==null)throw InventoryException.raiseException("Source item or inventory status is missing");
   value.put("item",Map.of("id",row.getItem().getId(),"name",Objects.toString(row.getItem().getName(),"")));value.put("inventoryStatus",Map.of("id",row.getInventoryStatus().getId()));
   value.put("color",row.getColor());value.put("productSize",row.getProductSize());value.put("style",row.getStyle());value.put("attribute1",row.getAttribute1());value.put("attribute2",row.getAttribute2());value.put("attribute3",row.getAttribute3());value.put("attribute4",row.getAttribute4());value.put("attribute5",row.getAttribute5());result.add(value);
  }return result;
 }
}
