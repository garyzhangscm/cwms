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
}
