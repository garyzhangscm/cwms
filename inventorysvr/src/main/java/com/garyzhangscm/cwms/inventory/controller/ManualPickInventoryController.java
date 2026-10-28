package com.garyzhangscm.cwms.inventory.controller;
import com.garyzhangscm.cwms.inventory.model.*;
import com.garyzhangscm.cwms.inventory.service.ManualPickInventoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/manual-pick-split")
public class ManualPickInventoryController {
 @Autowired private ManualPickInventoryService service;
 @PostMapping public Inventory apply(@RequestParam String requestId,@RequestParam Long warehouseId,@RequestParam Long sourceId,
  @RequestParam String sourceLpn,@RequestParam Long expectedQuantity,@RequestParam Long quantity,
  @RequestParam(defaultValue="") String newLpn,@RequestParam boolean keepOriginal,@RequestParam Long pickId,@RequestBody Location destination) throws Exception {
  return service.apply(requestId,warehouseId,sourceId,sourceLpn,expectedQuantity,quantity,newLpn,keepOriginal,pickId,destination);
 }
 @GetMapping("/fast-capabilities") public java.util.Map<String,Object> capabilities(){return java.util.Map.of("schema","manual-pick-fast-v1","enabled",service.fastEnabled());}
 @PostMapping("/compact-v1") public java.util.Map<String,Object> compact(@RequestParam String requestId,@RequestParam Long warehouseId,@RequestParam Long sourceId,
  @RequestParam String sourceLpn,@RequestParam Long expectedQuantity,@RequestParam Long quantity,
  @RequestParam(defaultValue="") String newLpn,@RequestParam boolean keepOriginal,@RequestParam Long pickId,@RequestBody Location destination)throws Exception {
  Inventory moved=service.apply(requestId,warehouseId,sourceId,sourceLpn,expectedQuantity,quantity,newLpn,keepOriginal,pickId,destination);
  return java.util.Map.of("id",moved.getId(),"lpn",moved.getLpn(),"quantity",moved.getQuantity());
 }
 @PostMapping("/whole-v1") public java.util.Map<String,Object> whole(@RequestParam String requestId,@RequestParam Long warehouseId,@RequestParam Long sourceId,
  @RequestParam String sourceLpn,@RequestParam Long expectedQuantity,@RequestParam Long quantity,
  @RequestParam(defaultValue="") String newLpn,@RequestParam boolean keepOriginal,@RequestParam Long pickId,@RequestBody Location destination)throws Exception {
  if(quantity==null||quantity<=0||!quantity.equals(expectedQuantity)||!newLpn.isEmpty())throw com.garyzhangscm.cwms.inventory.exception.InventoryException.raiseException("Whole-pallet request must move the complete source quantity and retain its LPN");
  Inventory moved=service.apply(requestId,warehouseId,sourceId,sourceLpn,expectedQuantity,quantity,newLpn,keepOriginal,pickId,destination);
  return java.util.Map.of("id",moved.getId(),"lpn",moved.getLpn(),"quantity",moved.getQuantity());
 }

}
