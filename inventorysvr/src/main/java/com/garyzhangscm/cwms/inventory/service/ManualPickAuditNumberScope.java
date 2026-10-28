package com.garyzhangscm.cwms.inventory.service;
import com.garyzhangscm.cwms.inventory.clients.RestTemplateProxy;
import com.garyzhangscm.cwms.inventory.exception.InventoryException;
import org.springframework.beans.factory.annotation.*;
import org.springframework.stereotype.Component;
import org.springframework.http.HttpMethod;
import org.springframework.core.annotation.Order;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import java.util.*;
/** Batch only inventory audit transaction IDs for the current operation; original group IDs and audit events remain. */
@Aspect @Component @Order(-10)
public class ManualPickAuditNumberScope {
 @Value("${manual.pick.pipeline-enabled:false}") private boolean enabled;
 @Autowired private RestTemplateProxy rest;
 private static class Context {final Long warehouse;final int count;Deque<String> numbers;Context(Long w,int n){warehouse=w;count=n;}}
 private final ThreadLocal<Context> current=new ThreadLocal<>();
 public boolean enabled(){return enabled;}
 @Around("execution(* com.garyzhangscm.cwms.inventory.service.ManualPickInventoryService.apply(..))")
 public Object operation(ProceedingJoinPoint point)throws Throwable {
  if(!enabled)return point.proceed();Object[] a=point.getArgs();Context old=current.get();
  current.set(new Context((Long)a[1],a[4]!=null&&a[5]!=null&&(Long)a[5]<(Long)a[4]?3:2));
  try{return point.proceed();}finally{if(old==null)current.remove();else current.set(old);}
 }
 @Around("execution(* com.garyzhangscm.cwms.inventory.clients.CommonServiceRestemplateClient.getNextInventoryActivityTransactionId(..))")
 public Object next(ProceedingJoinPoint point)throws Throwable {
  Context c=current.get();if(c==null||!c.warehouse.equals(point.getArgs()[0]))return point.proceed();
  if(c.numbers==null){List<String> list=rest.exchangeList(String.class,"http://apigateway:5555/api/common/system-controlled-number/inventory-activity-transaction-id/batch/next?warehouseId="+c.warehouse+"&batch="+c.count,HttpMethod.GET,null);
   if(list==null||list.size()!=c.count||list.stream().anyMatch(s->s==null||s.isBlank())||new HashSet<>(list).size()!=list.size())throw InventoryException.raiseException("Audit numbers could not be verified. Resume the same saved Pick request");
   c.numbers=new ArrayDeque<>(list);
  }
  return c.numbers.isEmpty()?point.proceed():c.numbers.removeFirst();
 }
}
