package com.garyzhangscm.cwms.outbound.loadlink;
import java.util.Map;
import org.springframework.web.bind.annotation.*;
import org.springframework.cache.annotation.*;
import com.garyzhangscm.cwms.outbound.model.*;
@RestController
@RequestMapping("/loads/{id}/shipping")
public class LoadShippingController {
 private final LoadShippingService service;
 public LoadShippingController(LoadShippingService service){this.service=service;}
 @GetMapping @ClientValidationEndpoint
 public Map<String,Object> preview(@PathVariable("id") long id,@RequestParam("warehouseId") long warehouseId,ClientRestriction restriction){return service.preview(warehouseId,id,restriction);}
 public static class OrderPlanRequest { public String orderNumber; }
 @GetMapping("/order-plans") @ClientValidationEndpoint
 public Map<String,Object> orderPlans(@PathVariable("id") long id,@RequestParam("warehouseId") long warehouseId,ClientRestriction restriction) {
  return service.getOrderPlans(warehouseId,id,restriction);
 }
 @PostMapping("/order-plans") @ClientValidationEndpoint @BillableEndpoint
 public Map<String,Object> addOrderPlan(@PathVariable("id") long id,@RequestParam("warehouseId") long warehouseId,@RequestBody OrderPlanRequest request,ClientRestriction restriction) {
  return service.addOrderPlan(warehouseId,id,request.orderNumber,restriction);
 }
 @DeleteMapping("/order-plans/{orderId}") @ClientValidationEndpoint @BillableEndpoint
 public Map<String,Object> removeOrderPlan(@PathVariable("id") long id,@PathVariable("orderId") long orderId,@RequestParam("warehouseId") long warehouseId,ClientRestriction restriction) {
  return service.removeOrderPlan(warehouseId,id,orderId,restriction);
 }
 @GetMapping("/shipment-options") @ClientValidationEndpoint
 public Map<String,Object> shipmentOptions(@PathVariable("id") long id,@RequestParam("warehouseId") long warehouseId,ClientRestriction restriction){
  return service.shipmentChoices(warehouseId,id,restriction);
 }
 @PostMapping("/shipments") @ClientValidationEndpoint @BillableEndpoint
 public Map<String,Object> replace(@PathVariable("id") long id,@RequestParam("warehouseId") long warehouseId,@RequestBody LoadShippingService.Links request,ClientRestriction restriction){return service.replace(warehouseId,id,request,restriction);}
 @PostMapping("/confirm-loaded") @ClientValidationEndpoint @BillableEndpoint
 @Caching(evict={@CacheEvict(value="AdminService_Order",allEntries=true),@CacheEvict(value="IntegrationService_Order",allEntries=true),@CacheEvict(value="WorkOrderService_OrderLine",allEntries=true)})
 public Map<String,Object> confirm(@PathVariable("id") long id,@RequestParam("warehouseId") long warehouseId,@RequestBody LoadShippingService.Confirmation request,ClientRestriction restriction){return service.confirm(warehouseId,id,request,restriction);}
}
