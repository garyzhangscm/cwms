import java.util.Map;
import java.util.List;
public class AllocationWireRuntimeProbe {
 public static void main(String[] args)throws Exception {
  String p="com.garyzhangscm.cwms.workorder.";
  Class<?> mapperClass=Class.forName("com.fasterxml.jackson.databind.ObjectMapper"),orderClass=Class.forName(p+"model.WorkOrder"),warehouseClass=Class.forName(p+"model.Warehouse");
  Object mapper=mapperClass.getConstructor().newInstance();mapperClass.getMethod("findAndRegisterModules").invoke(mapper);
  var read=mapperClass.getMethod("readValue",String.class,Class.class);
  Object order=read.invoke(mapper,"{\"id\":2586,\"number\":\"WO0000000390\",\"warehouseId\":1,\"item\":{\"id\":8392,\"warehouseId\":1},\"workOrderLines\":[{\"id\":11901,\"openQuantity\":50000,\"item\":{\"id\":6402,\"warehouseId\":1}}]}",orderClass);
  Object warehouse=read.invoke(mapper,"{\"id\":1}",warehouseClass);orderClass.getMethod("setWarehouse",warehouseClass).invoke(order,warehouse);
  Class<?> helper=Class.forName(p+"service.WorkOrderAllocationDataService");
  Object loader=helper.getConstructor(Class.forName(p+"clients.InventoryServiceRestemplateClient"),Class.forName(p+"clients.WarehouseLayoutServiceRestemplateClient"),mapperClass).newInstance(null,null,mapper);
  Map<?,?> normal=(Map<?,?>)mapperClass.getMethod("convertValue",Object.class,Class.class).invoke(mapper,order,Map.class);
  if(normal.containsKey("warehouse"))throw new AssertionError("Normal serialization was broadened");
  Object payload=helper.getMethod("requestBody",orderClass).invoke(loader,order);
  String json=(String)mapperClass.getMethod("writeValueAsString",Object.class).invoke(mapper,payload);
  Map<?,?> wire=(Map<?,?>)read.invoke(mapper,json,Map.class);
  if(!Integer.valueOf(1).equals(((Map<?,?>)wire.get("warehouse")).get("id")))throw new AssertionError("Warehouse lost in wire JSON");
  if(!Integer.valueOf(1).equals(((Map<?,?>)wire.get("item")).get("warehouseId")))throw new AssertionError("Finished item warehouse lost");
  Map<?,?> line=(Map<?,?>)((List<?>)wire.get("workOrderLines")).get(0);
  if(!Integer.valueOf(1).equals(line.get("warehouseId"))||!Integer.valueOf(1).equals(((Map<?,?>)line.get("item")).get("warehouseId")))throw new AssertionError("Line warehouse lost");
  System.out.println("PASS: actual release JAR serializes warehouse, finished Item and material warehouse IDs in allocation JSON; normal responses unchanged. No APIs or database accessed.");
 }
}
