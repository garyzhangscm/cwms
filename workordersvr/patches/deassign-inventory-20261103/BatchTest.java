package com.garyzhangscm.cwms.workorder.clients;
import com.garyzhangscm.cwms.workorder.model.Inventory;
import java.util.*;
import org.springframework.http.HttpMethod;
public class BatchTest {
 static class Proxy extends RestTemplateProxy {
  int calls=0, fail=0;
  public <T> List<T> exchangeList(Class<T> type,String uri,HttpMethod method,Object body) {
    if (!uri.startsWith("http://apigateway:5555/api/inventory/inventories?warehouseId=1&locationId=218&pickIds=")||uri.length()>2048) throw new AssertionError(uri);
    if(++calls==fail)throw new IllegalStateException("batch failed");
    List<T> rows=new ArrayList<>(); for(String s:uri.split("pickIds=")[1].split(",")){Inventory i=new Inventory(); i.setId(Long.valueOf(s));rows.add(type.cast(i));}return rows;
  }
 }
 public static void main(String[] args) throws Exception {
   Proxy p=new Proxy(); InventoryServiceRestemplateClient c=new InventoryServiceRestemplateClient();
   var f=c.getClass().getDeclaredField("restTemplateProxy");f.setAccessible(true);f.set(c,p);
   if(!c.findDeliveredInventory(1L,218L,"").isEmpty()||p.calls!=0)throw new AssertionError();
   var ids=new StringJoiner(",");for(int i=0;i<745;i++)ids.add(""+(Long.MAX_VALUE-i));ids.add(""+Long.MAX_VALUE);
   var rows=c.findDeliveredInventory(1L,218L,ids.toString());
   if(rows.size()!=745||p.calls!=12)throw new AssertionError();
   for(int i=0;i<745;i++) if(rows.get(i).getId()!=Long.MAX_VALUE-i)throw new AssertionError();
   p.fail=p.calls+2;try {c.findDeliveredInventory(1L,218L,ids.toString());throw new AssertionError();}catch(IllegalStateException expected){if(!"batch failed".equals(expected.getMessage()))throw expected;}
   System.out.println("PASS: no unfiltered call; 745 distinct IDs in 12 scoped batches; failure aborts lookup");
 }
}
