package com.garyzhangscm.cwms.outbound.controller;
import com.garyzhangscm.cwms.outbound.service.ManualPickQueueService;
import com.garyzhangscm.cwms.outbound.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.*;
@RestController
public class ManualPickQueueController {
 @Autowired private ManualPickQueueService queue;
 @GetMapping("/manual-pick-queue/capabilities") public Map<String,Object> capabilities(){return queue.capabilities();}
 @GetMapping("/orders/{orderId}/manual-pick-queue") @ClientValidationEndpoint
 public List<Map<String,Object>> list(@PathVariable Long orderId,@RequestParam Long warehouseId,ClientRestriction restriction){return queue.list(warehouseId,orderId,restriction);}
 @PostMapping("/orders/{orderId}/manual-pick-queue") @ClientValidationEndpoint
 public Map<String,Object> accept(@PathVariable Long orderId,@RequestParam Long warehouseId,@RequestParam String requestId,@RequestParam String sourceLpn,@RequestParam(defaultValue="") String newLpn,@RequestParam boolean keepOriginal,@RequestParam Long quantity,@RequestParam Long expectedQuantity,@RequestParam String destination,ClientRestriction restriction){return queue.accept(warehouseId,orderId,restriction,requestId,sourceLpn,newLpn,keepOriginal,quantity,expectedQuantity,destination);}
 @PostMapping("/orders/{orderId}/manual-pick-queue/{requestId}/retry") @ClientValidationEndpoint
 public Map<String,Object> retry(@PathVariable Long orderId,@PathVariable String requestId,@RequestParam Long warehouseId,ClientRestriction restriction){return queue.retry(warehouseId,orderId,restriction,requestId);}
}
