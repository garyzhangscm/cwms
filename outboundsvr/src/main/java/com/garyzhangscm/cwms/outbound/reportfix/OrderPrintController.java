package com.garyzhangscm.cwms.outbound.reportfix;
import java.util.*;import org.springframework.web.bind.annotation.*;import org.springframework.beans.factory.annotation.Autowired;
import com.garyzhangscm.cwms.outbound.model.*;import com.garyzhangscm.cwms.outbound.service.OrderService;
@RestController
public class OrderPrintController {
 @Autowired private OrderService orderService;@Autowired private OrderPrintPolicy policy;
 @GetMapping("/orders/{id}/print-eligibility") public Map<String,Object> eligibility(@PathVariable Long id){return policy.evaluate(orderService.findById(id));}
 @PostMapping("/orders/{id}/shipping-report") public ReportHistory report(@PathVariable Long id,@RequestParam Long shipmentId,@RequestParam String type,@RequestParam(defaultValue="en_US") String locale)throws Exception {
  Order order=orderService.findById(id);Long selected=policy.require(order,false,shipmentId);
  try(ShippingReportScope scope=new ShippingReportScope(selected)){
   if("BILL_OF_LADING".equals(type))return orderService.generateBillOfLadingByOrder(order,locale);
   if("PACKING_SLIP".equals(type))return orderService.generatePackingListByOrder(order,locale);
   throw new IllegalArgumentException("Unsupported shipping report type");
  }
 }
}
