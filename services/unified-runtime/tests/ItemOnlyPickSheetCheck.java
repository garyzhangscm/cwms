import java.nio.file.*;
import java.util.*;
import java.lang.reflect.*;
import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;

/** Render the real Jasper template with no staging location or candidate inventory. */
public class ItemOnlyPickSheetCheck {
 static void require(boolean b,String m){if(!b)throw new AssertionError(m);}
 static Map<String,Object> row(String name,long quantity,String unit){
  Map<String,Object> item=new HashMap<>();item.put("name",name);item.put("description","Product "+name);
  item.put("defaultItemPackageType",unit==null?null:Map.of("stockItemUnitOfMeasure",Map.of("unitOfMeasure",Map.of("name",unit))));
  return Map.of("number","1","item",item,"openQuantity",quantity);
 }
 static String contents(JRPrintElement e){
  String text=e instanceof JRPrintText?((JRPrintText)e).getFullText():"";
  if(e instanceof JRPrintFrame)for(JRPrintElement child:((JRPrintFrame)e).getElements())text+="\n"+contents(child);
  return text;
 }
 public static void main(String[] args)throws Exception{
  Path template=Path.of(args[0]),out=Path.of(args[1]);Files.createDirectories(out);
  String xml=Files.readString(template);
  require(!xml.contains("manualPickableInventory")&&!xml.contains("LPN"),"Source recommendation remained");
  require(xml.contains("<jr:codeExpression><![CDATA[$P{destination_location}"),"Drop-off barcode missing");
  require(xml.contains("$P{destination_location} != null && !$P{destination_location}.trim().isEmpty()"),"Drop-off barcode must reject blank values before encoding");
  JasperReport report=JasperCompileManager.compileReport(template.toString());
  Map<String,Object> p=new HashMap<>();p.put("order_number","FY000004078");p.put("destination_location","DROP-OFF-TEST");p.put(JRParameter.REPORT_LOCALE,Locale.US);
  JasperPrint print=JasperFillManager.fillReport(report,p,new JRBeanCollectionDataSource(List.of(row("WP-00020-50",72,"PCS"),row("WEIGHT-ITEM",25,"LB"),row("NO-UNIT",3,null))));
  String text="";for(JRPrintPage page:print.getPages())for(JRPrintElement e:page.getElements())text+="\n"+contents(e);
  for(String value:List.of("FY000004078","WP-00020-50","72","PCS","WEIGHT-ITEM","LB","NO-UNIT","Drop-off Location","DROP-OFF-TEST"))require(text.contains(value),"Missing "+value);
  require(!text.contains("Shipping Stage")&&!text.contains("Unknown location"),"Location hint remained");
  ImageIO.write((BufferedImage)JasperPrintManager.printPageToImage(print,0,1.5f),"png",out.resolve("manual-pick-sheet-preview.png").toFile());
  // Empty staging and missing/blank order barcode must never prevent a report.
  for(String number:new String[]{null,"","   "}){
   p.put("order_number",number);
   p.put("destination_location",number);
   JasperFillManager.fillReport(report,p,new JRBeanCollectionDataSource(List.of(row("TEST",1,null))));
  }
  p.put("order_number","EMPTY-ORDER");
  JasperFillManager.fillReport(report,p,new JRBeanCollectionDataSource(List.of()));
  // Report preparation on the exact patched production class must not call inventory/layout services.
  String base="com.garyzhangscm.cwms.outbound.";
  Class<?> orderClass=Class.forName(base+"model.Order"),reportClass=Class.forName(base+"model.Report");
  Object order=orderClass.getConstructor().newInstance();orderClass.getMethod("setNumber",String.class).invoke(order,"FY000004078");
  List<Object> lines=new ArrayList<>();orderClass.getMethod("setOrderLines",List.class).invoke(order,lines);
  Object service=Class.forName(base+"service.OrderService").getConstructor().newInstance();
  Field pickField=service.getClass().getDeclaredField("pickService");pickField.setAccessible(true);
  pickField.set(service,new com.garyzhangscm.cwms.outbound.service.PickService(){public java.util.List<com.garyzhangscm.cwms.outbound.model.Pick> findByOrder(com.garyzhangscm.cwms.outbound.model.Order ignored){return java.util.List.of();}});
  for(String flag:List.of("true","false")){
   System.setProperty("MES_OUTBOUND_MANUAL_PICK_REPORT_V2",flag);Object data=reportClass.getConstructor().newInstance();
   for(String method:List.of("setupOrderManualPickReportParameters","setupOrderManualPickReportData")){
    Method m=service.getClass().getDeclaredMethod(method,reportClass,orderClass);m.setAccessible(true);m.invoke(service,data,order);
   }
   require(((java.util.Collection<?>)reportClass.getMethod("getData").invoke(data)).isEmpty(),"Report no longer uses order lines");
   Map<?,?> params=(Map<?,?>)reportClass.getMethod("getParameters").invoke(data);
   require("FY000004078".equals(params.get("order_number"))&&"".equals(params.get("destination_location")),"Unexpected report parameters");
   Class<?> locationClass=Class.forName(base+"model.Location");Object location=locationClass.getConstructor().newInstance();
   locationClass.getMethod("setName",String.class).invoke(location,"DROP-OFF-TEST");
   orderClass.getMethod("setStageLocation",locationClass).invoke(order,location);
   Method parameterMethod=service.getClass().getDeclaredMethod("setupOrderManualPickReportParameters",reportClass,orderClass);parameterMethod.setAccessible(true);parameterMethod.invoke(service,data,order);
   require("DROP-OFF-TEST".equals(params.get("destination_location")),"Drop-off location lost");
   orderClass.getMethod("setStageLocation",locationClass).invoke(order,new Object[]{null});
  }
  System.out.println("PASS: actual Jasper compilation/render; PCS/LB/missing units; empty staging; blank/null barcode; empty order; both site flags without inventory/layout clients");
 }
}
