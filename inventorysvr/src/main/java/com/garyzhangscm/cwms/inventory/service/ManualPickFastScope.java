package com.garyzhangscm.cwms.inventory.service;
import com.garyzhangscm.cwms.inventory.model.Pick;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.springframework.stereotype.Component;
import java.util.Objects;
import java.util.function.Supplier;
/** Reuse only the Pick already validated for this inventory movement; never cache across requests. */
@Aspect @Component
public class ManualPickFastScope {
 private final ThreadLocal<Pick> current=new ThreadLocal<>();
 public <T>T withPick(Pick pick,Supplier<T> action){Pick prior=current.get();current.set(pick);try{return action.get();}finally{if(prior==null)current.remove();else current.set(prior);}}
 @Around("execution(* com.garyzhangscm.cwms.inventory.clients.OutbuondServiceRestemplateClient.getPickById(..))")
 public Object reuse(ProceedingJoinPoint point)throws Throwable{Pick pick=current.get();return pick!=null&&Objects.equals(pick.getId(),point.getArgs()[0])?pick:point.proceed();}
}
