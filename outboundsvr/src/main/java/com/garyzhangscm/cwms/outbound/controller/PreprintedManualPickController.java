package com.garyzhangscm.cwms.outbound.controller;
import com.garyzhangscm.cwms.outbound.service.PreprintedManualPickService;
import com.garyzhangscm.cwms.outbound.model.ManualPickOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
@RestController
@RequestMapping("/orders/{orderId}/preprinted-manual-pick")
public class PreprintedManualPickController {
 @Autowired private PreprintedManualPickService service;
 @GetMapping public Map<String,Object> preview(@PathVariable Long orderId,@RequestParam Long warehouseId,@RequestParam String sourceLpn) {
  return service.preview(warehouseId,orderId,sourceLpn);
 }
 @PostMapping("/cancel-unstarted") public Map<String,Object> cancel(@PathVariable Long orderId,@RequestParam Long warehouseId,@RequestParam String requestId) {
  return service.cancelUnstarted(requestId,warehouseId,orderId);
 }
 @PostMapping public ManualPickOperation process(@PathVariable Long orderId,@RequestParam Long warehouseId,@RequestParam String requestId,
    @RequestParam String sourceLpn,@RequestParam(defaultValue="") String newLpn,@RequestParam boolean keepOriginal,
    @RequestParam Long quantity,@RequestParam Long expectedQuantity,@RequestParam String destination) {
  return service.process(requestId,warehouseId,orderId,sourceLpn,newLpn,keepOriginal,quantity,expectedQuantity,destination);
 }
}
