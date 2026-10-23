package com.garyzhangscm.cwms.outbound.reportfix;
import org.aspectj.lang.*;import org.aspectj.lang.annotation.*;import org.springframework.stereotype.Component;import org.springframework.beans.factory.annotation.Autowired;
import com.garyzhangscm.cwms.outbound.model.Order;import com.garyzhangscm.cwms.outbound.service.OrderService;
@Aspect @Component @org.springframework.core.annotation.Order(-100)
public class OrderPrintGuard {
 @Autowired private OrderPrintPolicy policy;@Autowired @org.springframework.context.annotation.Lazy private OrderService orderService;
 @Around("execution(public * com.garyzhangscm.cwms.outbound.service.OrderService.generateManualPickReportByOrder(..)) || execution(public * com.garyzhangscm.cwms.outbound.service.OrderService.generatePackingListByOrder(..)) || execution(public * com.garyzhangscm.cwms.outbound.service.OrderService.generateBillOfLadingByOrder(..))")
 public Object generate(ProceedingJoinPoint call)throws Throwable {
  Object arg=call.getArgs()[0];Order order=arg instanceof Order?(Order)arg:orderService.findById((Long)arg);
  boolean manual=call.getSignature().getName().contains("ManualPick");Long id=policy.require(order,manual,ShippingReportScope.current());
  try(ShippingReportScope scope=new ShippingReportScope(id)){return call.proceed();}
 }
}
