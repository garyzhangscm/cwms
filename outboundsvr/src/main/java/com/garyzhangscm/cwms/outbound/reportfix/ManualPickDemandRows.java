package com.garyzhangscm.cwms.outbound.reportfix;
import java.util.*;import com.garyzhangscm.cwms.outbound.model.*;
public final class ManualPickDemandRows {
 public static List<Map<String,Object>> build(Order order,List<Pick> picks){
  Map<Long,Long> remaining=new HashMap<>();
  for(Pick p:picks)if(p.getShipmentLine()!=null&&p.getShipmentLine().getOrderLineId()!=null)remaining.merge(p.getShipmentLine().getOrderLineId(),Math.max(0L,p.getQuantity()-p.getPickedQuantity()),Long::sum);
  List<Map<String,Object>> rows=new ArrayList<>();
  for(OrderLine line:order.getOrderLines()){
   long quantity=Math.max(0L,line.getOpenQuantity()==null?0L:line.getOpenQuantity())+remaining.getOrDefault(line.getId(),0L);
   if(quantity<=0)continue;
   Map<String,Object> row=new HashMap<>();row.put("number",line.getNumber());row.put("item",line.getItem());row.put("openQuantity",quantity);rows.add(row);
  }return rows;
 }
}
