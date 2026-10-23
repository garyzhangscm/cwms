package com.garyzhangscm.cwms.outbound.reportfix;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import com.garyzhangscm.cwms.outbound.model.*;
import com.garyzhangscm.cwms.outbound.service.*;
import com.garyzhangscm.cwms.outbound.exception.OrderOperationException;
@Service
public class OrderPrintPolicy {
 @Autowired private ShipmentService shipmentService;
 @Autowired private PickService pickService;
 @Autowired private ShortAllocationService shortAllocationService;
 public Map<String,Object> evaluate(Order order){
  List<Shipment> shipments=shipmentService.findByOrder(order,false);
  List<Map<String,Object>> options=new ArrayList<>();boolean unfinished=false;
  for(Shipment s:shipments){
   if(s.getStatus()==ShipmentStatus.CANCELLED)continue;
   List<Pick> picks=pickService.findByShipment(s);
   boolean dispatched=s.getStatus()==ShipmentStatus.DISPATCHED;
   boolean ready=dispatched||(!picks.isEmpty()&&picks.stream().allMatch(p->p.getQuantity()!=null&&p.getPickedQuantity()!=null&&p.getQuantity()>0&&p.getPickedQuantity()>=p.getQuantity())&&shortAllocationService.findByShipment(s).isEmpty()&&s.getShipmentLines().stream().noneMatch(l->l.getOpenQuantity()!=null&&l.getOpenQuantity()>0));
   if(!ready)unfinished=true;
   Map<String,Object> option=new LinkedHashMap<>();option.put("id",s.getId());option.put("number",s.getNumber());option.put("ready",ready);option.put("status",s.getStatus().name());options.add(option);
  }
  boolean cancelled=order.getStatus()==OrderStatus.CANCELLED;
  boolean pending=unfinished||order.getOrderLines().stream().anyMatch(l->l.getOpenQuantity()!=null&&l.getOpenQuantity()>0);
  boolean manual=!cancelled&&Boolean.TRUE.equals(order.getAllowForManualPick())&&(order.getStatus()==OrderStatus.OPEN||order.getStatus()==OrderStatus.INPROCESS)&&pending;
  boolean shipping=!cancelled&&options.stream().anyMatch(m->Boolean.TRUE.equals(m.get("ready")));
  Map<String,Object> result=new LinkedHashMap<>();result.put("manualPick",manual);result.put("shipping",shipping);result.put("shipments",options);result.put("manualReason",manual?"":"Picking is complete, the order is cancelled, or no pick demand remains.");result.put("shippingReason",shipping?"":"Complete picking for a shipment before printing Packing Slip or BOL.");return result;
 }
 @SuppressWarnings("unchecked") public Long require(Order order,boolean manual,Long requested){
  Map<String,Object> state=evaluate(order);
  if(manual){if(!Boolean.TRUE.equals(state.get("manualPick")))throw OrderOperationException.raiseException((String)state.get("manualReason"));return null;}
  if(!Boolean.TRUE.equals(state.get("shipping")))throw OrderOperationException.raiseException((String)state.get("shippingReason"));
  List<Map<String,Object>> options=(List<Map<String,Object>>)state.get("shipments");
  List<Map<String,Object>> ready=new ArrayList<>();for(Map<String,Object> s:options)if(Boolean.TRUE.equals(s.get("ready"))&&(requested==null||requested.equals(s.get("id"))))ready.add(s);
  if(ready.size()!=1)throw OrderOperationException.raiseException(requested==null?"Select one completed-picking shipment to print.":"This shipment does not belong to the order or picking is incomplete.");
  return (Long)ready.get(0).get("id");
 }
}
