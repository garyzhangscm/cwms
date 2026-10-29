package com.garyzhangscm.cwms.inventory.controller;
import com.garyzhangscm.cwms.inventory.service.ShipmentInventoryBatchService;
import com.garyzhangscm.cwms.inventory.model.*;
import org.springframework.web.bind.annotation.*;
import java.util.*;
@RestController
public class ShipmentInventoryBatchController {
 private final ShipmentInventoryBatchService service;
 public ShipmentInventoryBatchController(ShipmentInventoryBatchService service){this.service=service;}
 @GetMapping("/shipment-inventory-batch/capabilities") public Map<String,Object> capabilities(){return Map.of("schema","shipment-inventory-batch-v1","enabled",true);}
 @GetMapping("/shipment-inventory-batch/{shipmentId}") @ClientValidationEndpoint
 public List<Map<String,Object>> preview(@RequestParam Long warehouseId,@PathVariable Long shipmentId,ClientRestriction restriction)throws Exception{return service.preview(warehouseId,shipmentId,restriction);}
 @PostMapping("/shipment-inventory-batch/{shipmentId}") @ClientValidationEndpoint
 public Map<String,Object> ship(@RequestParam Long warehouseId,@PathVariable Long shipmentId,@RequestBody Map<String,Object> request,ClientRestriction restriction)throws Exception{return service.ship(warehouseId,shipmentId,request,restriction);}
}
