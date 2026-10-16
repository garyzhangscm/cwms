package com.garyzhangscm.cwms.outbound.allocation;
import com.garyzhangscm.cwms.outbound.model.*;
import com.garyzhangscm.cwms.outbound.service.*;
import javax.persistence.*;
import java.util.*;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
@Component
public class ProductionAllocationGateway implements AllocationOrderGateway {
 @PersistenceContext private EntityManager em;
 @Autowired @Lazy private OrderService orders;
 @Autowired private AllocationStrategyFactory strategies;
 private Order order(long warehouseId,long orderId) {
  Order o=em.find(Order.class,orderId);
  if(o==null || !Objects.equals(o.getWarehouseId(),warehouseId))throw new AllocationValidationFailure("ORDER_NOT_FOUND","Order was not found in this warehouse.");return o;
 }
 public String lockAndGetState(long warehouseId,long orderId) {
  Order o=em.find(Order.class,orderId,LockModeType.PESSIMISTIC_WRITE);
  if(o==null || !Objects.equals(o.getWarehouseId(),warehouseId))throw new AllocationValidationFailure("ORDER_NOT_FOUND","Order was not found in this warehouse.");return o.getStatus().name();
 }
 public void validate(long warehouseId,long orderId) {
  Order o=order(warehouseId,orderId);
  if(Boolean.TRUE.equals(o.getCancelRequested()) || Boolean.TRUE.equals(o.getAllowForManualPick()) || o.getCategory()==OrderCategory.OUTSOURCING_ORDER)throw new AllocationValidationFailure("ORDER_NOT_READY","This order is not eligible for automatic allocation.");
  List<String> invalid=new ArrayList<>();int count=0;
  for(OrderLine line:o.getOrderLines())if(!Boolean.TRUE.equals(line.getNonAllocatable())) {
   ++count;
   if(line.getAllocationStrategyType()==null || !strategies.getAllocationStrategyByType(line.getAllocationStrategyType()).isPresent())invalid.add(line.getNumber());
  }
  if(count==0)throw new AllocationValidationFailure("NO_ALLOCATABLE_LINES","The order has no allocatable lines.");
  if(!invalid.isEmpty())throw new AllocationValidationFailure("INVALID_STRATEGY","Set a supported allocation strategy for line(s): "+String.join(", ",invalid.subList(0,Math.min(50,invalid.size())))+".");
 }
 public void markAllocating(long warehouseId,long orderId) {order(warehouseId,orderId).setStatus(OrderStatus.ALLOCATING);}
 public void execute(long orderId) {orders.executeAllocationTask(orderId);}
 public boolean hasPlannedWork(long orderId) {return em.createQuery("select count(sl) from ShipmentLine sl where sl.orderLine.order.id=:id",Long.class).setParameter("id",orderId).getSingleResult()>0;}
 public void finish(long warehouseId,long orderId,String original,boolean success) {
  Order o=order(warehouseId,orderId);
  if(o.getStatus()==OrderStatus.ALLOCATING)o.setStatus(success || hasPlannedWork(orderId)?OrderStatus.INPROCESS:OrderStatus.valueOf(original));
 }
}
