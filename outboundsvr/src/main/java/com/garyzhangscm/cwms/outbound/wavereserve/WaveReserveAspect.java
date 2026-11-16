package com.garyzhangscm.cwms.outbound.wavereserve;
import static com.garyzhangscm.cwms.outbound.wavereserve.WaveReserveStore.*;
import com.garyzhangscm.cwms.outbound.model.*;
import com.garyzhangscm.cwms.outbound.quantity.*;
import org.springframework.context.annotation.Lazy;
import org.aspectj.lang.*;import org.aspectj.lang.annotation.*;
import org.springframework.stereotype.Component;import org.springframework.beans.factory.annotation.*;
@Aspect @Component @org.springframework.core.annotation.Order(-20)
public class WaveReserveAspect {
 @Autowired @Lazy WaveReserveService service;
 @Autowired @Lazy QuantityAllocationService quantities;
 static Object call(ProceedingJoinPoint p){try{return p.proceed();}catch(RuntimeException e){throw e;}catch(Throwable e){throw new IllegalStateException(e);}}
 @Around("execution(* com.garyzhangscm.cwms.outbound.quantity.QuantityAllocationService.update(..))")
 public Object update(ProceedingJoinPoint p){long w=(Long)p.getArgs()[0];var req=(QuantityAllocationService.Request)p.getArgs()[3];return quantities.locked(w,()->{if(req!=null&&req.lines!=null)for(var line:req.lines)if(line!=null&&line.orderLineId!=null&&line.held!=null)service.store().validateHeld(w,line.orderLineId,line.held);return call(p);});}
 @Around("execution(* com.garyzhangscm.cwms.outbound.quantity.QuantityAllocationService.allocate(..))")
 public Object allocate(ProceedingJoinPoint p){var req=(AllocationRequest)p.getArgs()[0];if(req.getShipmentLines()==null||req.getShipmentLines().size()!=1)return call(p);var sl=req.getShipmentLines().get(0);long w=sl.getWarehouseId(),line=sl.getOrderLine().getId();if(!quantities.enrolled(sl.getOrderLine().getOrder().getId()))return call(p);return quantities.locked(w,()->{
  service.store().validateConversion(w,line,sl.getId(),req.getQuantity(),service.currentWave());
  var result=(AllocationResult)call(p);
  if(service.currentWave()!=null){long actual=result.getPicks().stream().mapToLong(Pick::getQuantity).sum();for(var pick:result.getPicks())require(pick.getShipmentLine()!=null&&pick.getShipmentLine().getId().equals(sl.getId()),"Generated Pick does not match the Wave Shipment line");service.store().converted(w,sl.getId(),actual);}return result;
 });}
 @Around("execution(* com.garyzhangscm.cwms.outbound.quantity.QuantityAllocationService.restoreCancelled(..))")
 public Object restore(ProceedingJoinPoint p){var pick=(Pick)p.getArgs()[0];Object result=call(p);if(pick.getShipmentLine()!=null)service.store().restored(pick.getWarehouseId(),pick.getShipmentLine().getId(),(Long)p.getArgs()[1]);return result;}
 @Around("execution(* com.garyzhangscm.cwms.outbound.service.OrderService.generateManualPick(com.garyzhangscm.cwms.outbound.model.Order,com.garyzhangscm.cwms.outbound.model.Location,String,Long,Long))")
 public Object scopedPick(ProceedingJoinPoint p){if(service.currentWave()==null)return call(p);Object[]a=p.getArgs();return service.generateScoped((Order)a[0],(Location)a[1],(String)a[2],(Long)a[3],(Long)a[4]);}
 @Around("execution(* com.garyzhangscm.cwms.outbound.service.WaveService.allocateWave(..)) || execution(* com.garyzhangscm.cwms.outbound.service.WaveService.completeWave(..)) || execution(* com.garyzhangscm.cwms.outbound.service.WaveService.cancelWave(..)) || execution(* com.garyzhangscm.cwms.outbound.service.WaveService.delete(..)) || execution(* com.garyzhangscm.cwms.outbound.service.WaveService.removeWave(..)) || execution(* com.garyzhangscm.cwms.outbound.service.WaveService.changeWave(..))")
 public Object legacy(ProceedingJoinPoint p){Object arg=p.getArgs()[0];if(arg instanceof String){for(String value:((String)arg).split(","))if(!value.isBlank())require(service.store().db.queryForObject("SELECT COUNT(*) FROM wave_reserve_run WHERE wave_id=?",Long.class,Long.parseLong(value))==0,"Manage Reserve Waves from the 4200 Wave screen");}Long id=arg instanceof Wave?((Wave)arg).getId():arg instanceof Long?(Long)arg:null;if(id!=null)require(service.store().db.queryForObject("SELECT COUNT(*) FROM wave_reserve_run WHERE wave_id=?",Long.class,id)==0,"This Wave uses Reserve. Manage its reservation and scanning in the 4200 Wave screen; shipping remains in Dispatch Trailer.");return call(p);}
 @Around("execution(* com.garyzhangscm.cwms.outbound.service.WaveService.deassignShipmentLine(..))")
 public Object deassign(ProceedingJoinPoint p){Object arg=p.getArgs()[0];Long id=arg instanceof Wave?((Wave)arg).getId():(Long)arg;require(service.store().db.queryForObject("SELECT COUNT(*) FROM wave_reserve_run WHERE wave_id=?",Long.class,id)==0,"Release and review this Reserve Wave before changing its Shipment lines");return call(p);}
 @Around("execution(* com.garyzhangscm.cwms.outbound.service.WaveService.planWaveByOrderLines(..)) || execution(* com.garyzhangscm.cwms.outbound.service.WaveService.planWaveByShipmentLines(..))")
 public Object plan(ProceedingJoinPoint p){if(service.isPlanning())return call(p);long w=(Long)p.getArgs()[0];String number=(String)p.getArgs()[1];if(number!=null)require(service.store().db.queryForObject("SELECT COUNT(*) FROM wave_reserve_run r JOIN wave w ON w.wave_id=r.wave_id WHERE r.warehouse_id=? AND w.number=?",Long.class,w,number)==0,"Use the 4200 Wave screen to edit this Reserve Wave");return call(p);}

 @Around("execution(* com.garyzhangscm.cwms.outbound.service.ShipmentService.cancelShipment(..)) || execution(* com.garyzhangscm.cwms.outbound.service.ShipmentService.removeShipment(..)) || execution(* com.garyzhangscm.cwms.outbound.service.ShipmentService.delete(..))")
 public Object shipmentTerminal(ProceedingJoinPoint p){Object a=p.getArgs()[0];java.util.List<Long> ids=new java.util.ArrayList<>();if(a instanceof Shipment)ids.add(((Shipment)a).getId());else if(a instanceof Long)ids.add((Long)a);else if(a instanceof String)for(String value:((String)a).split(","))if(!value.isBlank())ids.add(Long.parseLong(value));
  for(Long id:ids)require(service.store().db.queryForObject("SELECT COUNT(*) FROM shipment_line sl JOIN wave_reserve_run r ON r.wave_id=sl.wave_id AND r.warehouse_id=sl.warehouse_id WHERE sl.shipment_id=?",Long.class,id)==0,"Cancel this Shipment's Reserve Wave grouping first. Its order Reserve will remain available.");return call(p);
 }
 @Around("execution(* com.garyzhangscm.cwms.outbound.service.ShipmentLineService.cancelShipmentLine(..)) || execution(* com.garyzhangscm.cwms.outbound.service.ShipmentLineService.delete(..))")
 public Object lineTerminal(ProceedingJoinPoint p){Object a=p.getArgs()[0];Long id=a instanceof ShipmentLine?((ShipmentLine)a).getId():(Long)a;require(service.store().db.queryForObject("SELECT COUNT(*) FROM shipment_line sl JOIN wave_reserve_run r ON r.wave_id=sl.wave_id AND r.warehouse_id=sl.warehouse_id WHERE sl.shipment_line_id=?",Long.class,id)==0,"Cancel the Reserve Wave grouping before removing its Shipment lines");return call(p);}
 @Around("execution(* com.garyzhangscm.cwms.outbound.service.ShipmentLineService.save(..)) || execution(* com.garyzhangscm.cwms.outbound.service.ShipmentLineService.planShipmentLineIntoWave(..)) || execution(* com.garyzhangscm.cwms.outbound.service.ShipmentLineService.deassignShipmentLineFromWave(..))")
 public Object lineIdentity(ProceedingJoinPoint p){for(Object arg:p.getArgs())if(arg instanceof ShipmentLine){ShipmentLine sl=(ShipmentLine)arg;if(sl.getId()==null)continue;var rows=service.store().db.queryForList("SELECT sl.* FROM shipment_line sl JOIN wave_reserve_run r ON r.wave_id=sl.wave_id AND r.warehouse_id=sl.warehouse_id WHERE sl.shipment_line_id=?",sl.getId());if(!rows.isEmpty()){var row=rows.get(0);require(sl.getWave()!=null&&sl.getWave().getId()==n(row,"wave_id")&&sl.getOrderLine()!=null&&sl.getOrderLine().getId()==n(row,"outbound_order_line_id")&&sl.getWarehouseId()==n(row,"warehouse_id")&&sl.getQuantity()==n(row,"quantity")&&sl.getStatus()!=ShipmentLineStatus.CANCELLED,"Reserve Wave line identity and demand cannot be changed while attached");}}return call(p);}

 @Around("execution(* com.garyzhangscm.cwms.outbound.service.OrderService.getPickableQuantityForManualPick(com.garyzhangscm.cwms.outbound.model.Order,String,Boolean))")
 public Object scopedAvailability(ProceedingJoinPoint p){if(service.currentWave()==null)return call(p);Object[] args=p.getArgs().clone();args[0]=service.scopedReadOrder((Order)args[0]);return service.readingAvailability(()->{try{return p.proceed(args);}catch(RuntimeException e){throw e;}catch(Throwable e){throw new IllegalStateException(e);}});}
 @Around("execution(* com.garyzhangscm.cwms.outbound.service.ShipmentService.findByOrder(com.garyzhangscm.cwms.outbound.model.Order,boolean))")
 public Object scopedShipments(ProceedingJoinPoint p){if(!service.isAvailability())return call(p);return service.scopedReadShipments((Order)p.getArgs()[0]);}
}
