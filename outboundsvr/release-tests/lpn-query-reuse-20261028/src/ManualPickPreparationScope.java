package com.garyzhangscm.cwms.outbound.service;
import com.garyzhangscm.cwms.outbound.model.Inventory;
import com.garyzhangscm.cwms.outbound.clients.RestTemplateProxy;
import org.springframework.beans.factory.annotation.*;
import org.springframework.stereotype.Component;
import org.springframework.http.HttpMethod;
import org.springframework.web.util.UriComponentsBuilder;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import java.util.*;
import java.util.function.Supplier;
/** One snapshot during a single preparation transaction. Never survives into inventory movement. */
@Aspect @Component @org.springframework.core.annotation.Order(-100)
public class ManualPickPreparationScope {
 @Value("${manual.pick.pipeline-enabled:false}") private boolean enabled;
 @Autowired @org.springframework.context.annotation.Lazy private RestTemplateProxy rest;
 private static class Context {final Long warehouse;final String lpn;List<Inventory> rows;String candidateKey;List<Inventory> candidates;Context(Long w,String l){warehouse=w;lpn=l;}}
 private final ThreadLocal<Context> current=new ThreadLocal<>();
 public boolean enabled(){return enabled;}
 public <T>T within(Long warehouse,String lpn,Supplier<T> action){
  if(!enabled)return action.get();Context old=current.get();
  if(old!=null&&Objects.equals(old.warehouse,warehouse)&&Objects.equals(old.lpn,lpn))return action.get();
  current.set(new Context(warehouse,lpn));try{return action.get();}finally{if(old==null)current.remove();else current.set(old);}
 }
 @Around("execution(* com.garyzhangscm.cwms.outbound.clients.RestTemplateProxy.exchangeList(..))")
 public Object source(ProceedingJoinPoint point)throws Throwable {
  Context c=current.get();Object[] args=point.getArgs();if(c==null||args[0]!=Inventory.class||args[2]!=HttpMethod.GET)return point.proceed();
  var uri=UriComponentsBuilder.fromUriString((String)args[1]).build();var q=uri.getQueryParams();
  if("/api/inventory/inventories/pickable".equals(uri.getPath()))return candidates(point,c,uri);
  if(!"apigateway".equals(uri.getHost())||uri.getPort()!=5555||!"/api/inventory/inventories".equals(uri.getPath())||!new HashSet<>(List.of("warehouseId","lpn","includeDetails")).containsAll(q.keySet())||!Objects.equals(String.valueOf(c.warehouse),q.getFirst("warehouseId"))||!Objects.equals(c.lpn,q.getFirst("lpn")))return point.proceed();
  if(c.rows==null){String url=UriComponentsBuilder.fromHttpUrl("http://apigateway:5555/api/inventory/manual-pick-split/source-v1").queryParam("warehouseId",c.warehouse).queryParam("lpn",c.lpn).toUriString();
   List<Inventory> rows=rest.exchangeList(Inventory.class,url,HttpMethod.GET,null);
   if(rows==null)throw new IllegalStateException("Source snapshot response is missing");
   for(Inventory row:rows)if(row==null||row.getId()==null||!c.warehouse.equals(row.getWarehouseId())||!c.lpn.equals(row.getLpn())||row.getQuantity()==null||row.getItem()==null||row.getItem().getId()==null||row.getInventoryStatus()==null||row.getInventoryStatus().getId()==null)throw new IllegalStateException("Source snapshot does not match requested LPN");
   c.rows=List.copyOf(rows);
  }
  return c.rows;
 }
 // Reuse only the single-LPN availability read immediately preceding candidate allocation.
 // The queue's warehouse mutex, held-quantity checks and saved-operation exclusion stay active.
 // Inventory movement executes outside this scope and still takes its own row lock and revalidates.
 private Object candidates(ProceedingJoinPoint point,Context c,org.springframework.web.util.UriComponents uri)throws Throwable {
  var q=uri.getQueryParams();
  Set<String> allowed=Set.of("includeDetails","itemId","inventoryStatusId","locationId","lpn","lpnLimit");
  boolean eligible=point.getArgs()[3]==null && "http".equals(uri.getScheme()) && "apigateway".equals(uri.getHost()) && uri.getPort()==5555
   && org.springframework.transaction.support.TransactionSynchronizationManager.isActualTransactionActive()
   && allowed.containsAll(q.keySet()) && q.values().stream().allMatch(v->v.size()==1)
   && "false".equals(q.getFirst("includeDetails")) && c.lpn.equals(q.getFirst("lpn"))
   && c.rows!=null && c.rows.size()==1;
  Inventory source=eligible?c.rows.get(0):null;
  eligible=eligible && Objects.equals(String.valueOf(source.getItem().getId()),q.getFirst("itemId"))
   && Objects.equals(String.valueOf(source.getInventoryStatus().getId()),q.getFirst("inventoryStatusId"))
   && source.getLocationId()!=null && Objects.equals(String.valueOf(source.getLocationId()),q.getFirst("locationId"));
  String limit=q.getFirst("lpnLimit");
  if(!eligible || (limit!=null&&!"500".equals(limit))){c.candidateKey=null;c.candidates=null;return point.proceed();}
  String key=q.getFirst("itemId")+"|"+q.getFirst("inventoryStatusId")+"|"+q.getFirst("locationId")+"|"+c.lpn;
  if("500".equals(limit) && key.equals(c.candidateKey) && c.candidates!=null){
   List<Inventory> answer=c.candidates;c.candidates=null;c.candidateKey=null;
   org.slf4j.LoggerFactory.getLogger(getClass()).info("Manual Pick reused checked LPN candidates: warehouse={}, lpn={}, rows={}",c.warehouse,c.lpn,answer.size());
   return answer;
  }
  c.candidateKey=null;c.candidates=null;
  Object result=point.proceed();
  if(limit==null && result instanceof List<?> && ((List<?>)result).size()==1 && ((List<?>)result).get(0) instanceof Inventory){
   Inventory row=(Inventory)((List<?>)result).get(0);
   if(Objects.equals(row.getId(),source.getId()) && Objects.equals(row.getWarehouseId(),c.warehouse)
    && Objects.equals(row.getLpn(),c.lpn) && Objects.equals(row.getLocationId(),source.getLocationId())
    && row.getItem()!=null && Objects.equals(row.getItem().getId(),source.getItem().getId())
    && row.getInventoryStatus()!=null && Objects.equals(row.getInventoryStatus().getId(),source.getInventoryStatus().getId())
    && row.getQuantity()!=null && row.getQuantity()>0 && row.getQuantity()<=source.getQuantity()){
    c.candidateKey=key;c.candidates=new ArrayList<>(List.of(row));
   }
  }
  return result;
 }

}
