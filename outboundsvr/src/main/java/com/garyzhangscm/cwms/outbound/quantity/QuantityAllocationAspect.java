package com.garyzhangscm.cwms.outbound.quantity;

import com.garyzhangscm.cwms.outbound.model.*;
import com.garyzhangscm.cwms.outbound.service.OrderService;
import com.garyzhangscm.cwms.outbound.service.PickService;
import com.garyzhangscm.cwms.outbound.exception.OrderOperationException;
import org.aspectj.lang.*;
import org.aspectj.lang.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/** All strategy beans participate; selecting DEFAULT or using an old client is not a bypass. */
@Aspect @Component @Order(0)
public class QuantityAllocationAspect {
    @Autowired @org.springframework.context.annotation.Lazy private QuantityAllocationService quantities;
    @Autowired @org.springframework.context.annotation.Lazy private OrderService orders;
    @Autowired @org.springframework.context.annotation.Lazy private PickService picks;
    private final ThreadLocal<Boolean> cancelling=ThreadLocal.withInitial(()->false);
    private final ThreadLocal<Long> converting=new ThreadLocal<>();

    @Around("execution(* com.garyzhangscm.cwms.outbound.model.AllocationStrategy+.allocate(..))")
    public Object allocate(ProceedingJoinPoint point) throws Throwable {
        if(!quantities.enabled()) return point.proceed();
        AllocationRequest request=(AllocationRequest)point.getArgs()[0];
        return quantities.allocate(request,()->{
            Long orderId=request.getShipmentLines().size()==1?request.getShipmentLines().get(0).getOrderLine().getOrder().getId():null;
            if(orderId!=null&&quantities.enrolled(orderId))converting.set(orderId);
            try{return (AllocationResult)proceed(point);}finally{converting.remove();}
        });
    }
    @Around("execution(* com.garyzhangscm.cwms.outbound.service.PickService.generatePick(..)) || execution(* com.garyzhangscm.cwms.outbound.service.PickService.generateMnaualPick(..))")
    public Object directPick(ProceedingJoinPoint point) throws Throwable {
        if(!quantities.enabled())return point.proceed();
        Long warehouse=null,item=null,status=null,client=null,orderId=null;long quantity=-1;boolean mixedOwner=false;
        for(Object arg:point.getArgs()) {
            if(arg instanceof ShipmentLine){ShipmentLine line=(ShipmentLine)arg;warehouse=line.getWarehouseId();orderId=line.getOrderLine().getOrder().getId();}
            if(arg instanceof WorkOrder)warehouse=((WorkOrder)arg).getWarehouseId();
            if(arg instanceof ShortAllocation)warehouse=((ShortAllocation)arg).getWarehouseId();
            if(arg instanceof Inventory){Inventory stock=(Inventory)arg;item=stock.getItem().getId();status=stock.getInventoryStatus().getId();if(client==null)client=stock.getClientId();}
            if(arg instanceof InventorySummary){InventorySummary stock=(InventorySummary)arg;item=stock.getItem().getId();status=stock.getInventoryStatus().getId();java.util.Set<Long> owners=new java.util.HashSet<>();for(java.util.List<Inventory> group:stock.getInventories().values())for(Inventory row:group)owners.add(row.getClientId()==null?0L:row.getClientId());mixedOwner=owners.size()!=1;if(!mixedOwner)client=owners.iterator().next();}
            if(arg instanceof Long && quantity<0)quantity=(Long)arg;
        }
        if(orderId!=null && warehouse!=null && quantities.defaultFor(warehouse))quantities.requireManualPickAllowed(orders.findById(orderId));
        if(converting.get()!=null && converting.get().equals(orderId))return point.proceed();
        if(warehouse==null||item==null||status==null||quantity<0)throw OrderOperationException.raiseException("Cannot validate quantity reservation for this pick path");
        final long w=warehouse,i=item,s=status,c=client==null?0:client,q=quantity;final Long o=orderId;final boolean mixed=mixedOwner;
        return quantities.locked(w,()->{if(quantities.hasItemHolds(w,i)){if(mixed)throw OrderOperationException.raiseException("Mixed-owner allocation needs review while item quantity is reserved");quantities.checkDirectPick(w,i,s,c,q,o);}return proceed(point);});
    }
    @Around("execution(* com.garyzhangscm.cwms.outbound.service.OrderService.allocate(Long, Boolean))")
    public Object automatic(ProceedingJoinPoint point) throws Throwable {
        quantities.requireClassicAllocationAllowed((Long)point.getArgs()[0]);
        return point.proceed();
    }
    @Around("execution(* com.garyzhangscm.cwms.outbound.service.OrderService.getPickableQuantityForManualPick(..))")
    public Object availability(ProceedingJoinPoint point) throws Throwable {
        long actual=((Number)point.proceed()).longValue();if(!quantities.enabled())return actual;
        Object[] args=point.getArgs();com.garyzhangscm.cwms.outbound.model.Order order=args[0] instanceof com.garyzhangscm.cwms.outbound.model.Order?(com.garyzhangscm.cwms.outbound.model.Order)args[0]:orders.findById((Long)args[0]);
        if(!quantities.enrolled(order.getId()))return actual;
        // The underlying availability already checked LPN eligibility. Obtain only the scanned item's identity.
        // Request-specific stock is intentionally not cached across scans.
        java.util.List<com.garyzhangscm.cwms.outbound.model.Inventory> rows=inventory.getInventoryByLpn(order.getWarehouseId(),(String)args[1]);
        if(rows.size()!=1)return 0L;
        return quantities.clamp(order,rows.get(0).getItem().getId(),actual);
    }
    @Autowired @org.springframework.context.annotation.Lazy private com.garyzhangscm.cwms.outbound.clients.InventoryServiceRestemplateClient inventory;
    // Runs before shipment planning; the enclosing saved-operation preparation holds the order lock.
    @Around("execution(* com.garyzhangscm.cwms.outbound.service.OrderService.generateManualPick(..))")
    public Object manualEntry(ProceedingJoinPoint point) throws Throwable {
        if(quantities.enabled()) {
            Object first=point.getArgs()[0];
            com.garyzhangscm.cwms.outbound.model.Order order=first instanceof com.garyzhangscm.cwms.outbound.model.Order?(com.garyzhangscm.cwms.outbound.model.Order)first:orders.findById((Long)first);
            quantities.requireManualPickAllowed(order);
        }
        return point.proceed();
    }
    @Around("execution(* com.garyzhangscm.cwms.outbound.service.PreprintedManualPickService.process(..)) || execution(* com.garyzhangscm.cwms.outbound.service.PreprintedManualPickService.prepare(..))")
    public Object safeScan(ProceedingJoinPoint point) throws Throwable {
        if(!quantities.enabled())return point.proceed();
        return quantities.scanning(()->proceed(point));
    }
    @Around("execution(* com.garyzhangscm.cwms.outbound.controller.ManualPickAvailabilityController.availability(..))")
    public Object orderAvailability(ProceedingJoinPoint point) throws Throwable {
        Object result=point.proceed();if(!quantities.enabled())return result;
        return quantities.clampAvailability((Long)point.getArgs()[0],(java.util.Map<String,Object>)result);
    }

    @Around("execution(* com.garyzhangscm.cwms.outbound.service.PickService.cancelPick(..))")
    public Object cancellation(ProceedingJoinPoint point) throws Throwable {
        if(!quantities.enabled()||cancelling.get())return point.proceed();
        Object[] args=point.getArgs();Pick pick=args[0] instanceof Pick?(Pick)args[0]:picks.findById((Long)args[0]);
        if(pick.getShipmentLine()==null || !quantities.enrolled(pick.getShipmentLine().getOrderLine().getOrder().getId()))return point.proceed();
        return quantities.locked(pick.getWarehouseId(),()->{
            cancelling.set(true);
            try {
                Pick current=quantities.lockPick(pick.getId());quantities.requirePickCancelable(current);
                long open=current.getQuantity()-current.getPickedQuantity();long cancelled=args.length>1&&args[1] instanceof Long?(Long)args[1]:open;
                if(cancelled<0 || cancelled>open)throw OrderOperationException.raiseException("Pick quantity changed. Refresh before cancellation");
                Object[] fresh=args.clone();if(fresh[0] instanceof Pick)fresh[0]=current;
                Object result=proceed(point,fresh);quantities.restoreCancelled(current,cancelled);return result;
            }
            finally {cancelling.remove();}
        });
    }
    /** Unpick calls cancelPick on the same service instance, bypassing its Spring advice. */
    @Around("execution(* com.garyzhangscm.cwms.outbound.service.PickService.unpick(..))")
    public Object unpick(ProceedingJoinPoint point) throws Throwable {
        if(!quantities.enabled()||cancelling.get())return point.proceed();
        Object[] args=point.getArgs();Pick pick=args[0] instanceof Pick?(Pick)args[0]:picks.findById((Long)args[0]);
        if(pick.getShipmentLine()==null || !quantities.enrolled(pick.getShipmentLine().getOrderLine().getOrder().getId()))return point.proceed();
        return quantities.locked(pick.getWarehouseId(),()->{
            cancelling.set(true);
            try {
                Pick current=quantities.lockPick(pick.getId());quantities.requirePickCancelable(current);
                long quantity=(Long)args[1],total=current.getQuantity(),picked=current.getPickedQuantity();
                if(quantity<=0 || quantity>picked || picked>total)throw OrderOperationException.raiseException("Picked quantity changed. Refresh before Unpick");
                Object[] fresh=args.clone();if(fresh[0] instanceof Pick)fresh[0]=current;
                Object result=proceed(point,fresh);
                // Legacy cancellation reduces total quantity only. Keep a partial Pick's confirmed quantity consistent too.
                if(quantity<total){current.setPickedQuantity(picked-quantity);picks.saveOrUpdate(current);}
                quantities.restoreCancelled(current,quantity);return result;
            }
            finally {cancelling.remove();}
        });
    }
    @Around("execution(* com.garyzhangscm.cwms.outbound.service.OrderService.save(..)) || execution(* com.garyzhangscm.cwms.outbound.service.OrderService.saveOrUpdate(..))")
    public Object orderSave(ProceedingJoinPoint point) throws Throwable {
        if(!quantities.enabled())return point.proceed();
        for(Object value:point.getArgs()) if(value instanceof com.garyzhangscm.cwms.outbound.model.Order) {
            com.garyzhangscm.cwms.outbound.model.Order order=(com.garyzhangscm.cwms.outbound.model.Order)value;
            if(order.getId()!=null && quantities.enrolled(order.getId()))return quantities.locked(order.getWarehouseId(),()->{quantities.validateOrderSave(order);return proceed(point);});
        }
        return point.proceed();
    }
    @Around("execution(* com.garyzhangscm.cwms.outbound.service.OrderService.completeOrder(Long, ..)) || execution(* com.garyzhangscm.cwms.outbound.service.OrderService.removeOrder(Long)) || execution(* com.garyzhangscm.cwms.outbound.service.OrderService.cancelOrder(Long, Long, ..))")
    public Object terminalById(ProceedingJoinPoint point) throws Throwable {
        if(!quantities.enabled())return point.proceed();
        Object[] args=point.getArgs();int position=point.getSignature().getName().equals("cancelOrder")?1:0;
        Long id=(Long)args[position];if(!quantities.enrolled(id))return point.proceed();
        com.garyzhangscm.cwms.outbound.model.Order order=orders.findById(id);
        return quantities.locked(order.getWarehouseId(),()->{quantities.requireNoHolds(id);return proceed(point);});
    }
    @Around("execution(* com.garyzhangscm.cwms.outbound.service.OrderLineService.save(..)) || execution(* com.garyzhangscm.cwms.outbound.service.OrderLineService.saveOrUpdate(..))")
    public Object lineSave(ProceedingJoinPoint point) throws Throwable {
        if(!quantities.enabled())return point.proceed();
        for(Object arg:point.getArgs())if(arg instanceof OrderLine){OrderLine line=(OrderLine)arg;if(line.getWarehouseId()!=null)return quantities.locked(line.getWarehouseId(),()->{quantities.validateLineSave(line);return proceed(point);});}
        return point.proceed();
    }
    @Around("execution(* com.garyzhangscm.cwms.outbound.controller.PickController.addPick(..)) || execution(* com.garyzhangscm.cwms.outbound.controller.PickController.changePick(..))")
    public Object rawPick(ProceedingJoinPoint point) throws Throwable {
        if(!quantities.enabled())return point.proceed();
        for(Object arg:point.getArgs())if(arg instanceof Pick){Pick pick=(Pick)arg;if(pick.getWarehouseId()==null||pick.getItemId()==null)throw OrderOperationException.raiseException("Pick warehouse and item are required");return quantities.locked(pick.getWarehouseId(),()->{if(quantities.hasItemHolds(pick.getWarehouseId(),pick.getItemId())||(pick.getShipmentLine()!=null&&quantities.enrolled(pick.getShipmentLine().getOrderLine().getOrder().getId())))throw OrderOperationException.raiseException("Use the allocation and scan confirmation flows instead of directly editing reserved Picks");return proceed(point);});}
        throw OrderOperationException.raiseException("Pick is required");
    }
    private static Object proceed(ProceedingJoinPoint point) {
        try{return point.proceed();}catch(RuntimeException e){throw e;}catch(Error e){throw e;}catch(Throwable e){throw new IllegalStateException("Allocation failed",e);}
    }
    private static Object proceed(ProceedingJoinPoint point,Object[] args) {
        try{return point.proceed(args);}catch(RuntimeException e){throw e;}catch(Error e){throw e;}catch(Throwable e){throw new IllegalStateException("Allocation failed",e);}
    }
}
