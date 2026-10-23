import com.garyzhangscm.cwms.inbound.model.*;
import com.garyzhangscm.cwms.inbound.service.ReceiptService;
import java.lang.reflect.*;
import java.time.ZonedDateTime;
import java.util.*;
public class InboundLabelCompatibility {
 static Map<String,Object> label(String description) throws Exception { return label(description,true,true); }
 static Map<String,Object> label(String description,boolean withPackage,boolean withStock) throws Exception {
  return label(description,withPackage,withStock,false,false,27L,false);
 }
 static Map<String,Object> label(String description,boolean withPackage,boolean withStock,boolean receiptPackage,boolean caseUnit,Long quantity,boolean ignore) throws Exception {
  Item item=new Item();item.setId(1L);item.setName("OFFLINE-TEST");item.setDescription(description);
  ItemPackageType pack=new ItemPackageType();pack.setName("TEST-PACK");
  UnitOfMeasure uom=new UnitOfMeasure();uom.setName("PCS");
  ItemUnitOfMeasure stock=new ItemUnitOfMeasure();stock.setQuantity(1);stock.setUnitOfMeasure(uom);
  stock.setCaseFlag(false);
  if(withStock){pack.setStockItemUnitOfMeasure(stock);pack.setItemUnitOfMeasures(Arrays.asList(stock));} else {pack.setItemUnitOfMeasures(new ArrayList<>());}
  if(caseUnit){ItemUnitOfMeasure cases=new ItemUnitOfMeasure();cases.setCaseFlag(true);cases.setQuantity(240);cases.setUnitOfMeasure(uom);pack.setItemUnitOfMeasures(Arrays.asList(stock,cases));}
  item.setItemPackageTypes(withPackage?Arrays.asList(pack):new ArrayList<>());
  Receipt receipt=new Receipt();receipt.setId(1L);receipt.setNumber("OFFLINE-RECEIPT");receipt.setCheckInTime(ZonedDateTime.parse("2026-10-22T00:00:00Z"));
  ReceiptLine line=new ReceiptLine();line.setId(1L);line.setReceipt(receipt);line.setItem(item);line.setItemId(1L);
  if(receiptPackage){pack.setId(2L);line.setItemPackageType(pack);line.setItemPackageTypeId(2L);}
  ReceivingLPNLabelFontSize font=new ReceivingLPNLabelFontSize();font.setCharactersPerLine(20);font.setBaseSize(40);font.setBaseSizeLineCount(3);font.setStep(2);
  Method method=ReceiptService.class.getDeclaredMethod("getLPNDocumentContent",ReceiptLine.class,String.class,Long.class,Boolean.class,ReceivingLPNLabelFontSize.class,ReceivingLPNLabelFontSize.class,ReceivingLPNLabelFontSize.class);method.setAccessible(true);
  return (Map<String,Object>)method.invoke(new ReceiptService(),line,"OFFLINE-LPN",quantity,ignore,font,font,font);
 }
 public static void main(String[] args) throws Exception {
  String[] descriptions={"Short description","","   ","This is a very long description for label wrapping compatibility",null};
  for(int i=0;i<descriptions.length;i++){
   try {Map<String,Object> data=label(descriptions[i]);
    if(!Long.valueOf(27).equals(data.get("quantity")) || !"OFFLINE-LPN".equals(data.get("lpn")))throw new AssertionError("Core label fields changed");
    System.out.println("RESULT case="+i+" OK split="+data.keySet().stream().filter(k->k.startsWith("item_description_")).count()+" stockUOM="+data.get("stockUOM")+" barcode="+data.get("barcode"));
   } catch(InvocationTargetException e){throw new AssertionError("Label failed for description case "+i,e.getCause());}
  }
  for(int i=5;i<7;i++) {
   try {Map<String,Object> data=label("Valid description",i!=5,i!=6);System.out.println("RESULT case="+i+" OK quantity="+data.get("quantity"));}
   catch(InvocationTargetException e){throw new AssertionError("Label failed for package case "+i,e.getCause());}
  }
  for(boolean caseUnit:new boolean[]{false,true})for(boolean ignore:new boolean[]{false,true}) {
   Map<String,Object> data=label("Receipt package",true,true,true,caseUnit,240L,ignore);
   if(!Objects.equals(data.get("quantity"),ignore?"":240L))throw new AssertionError("Explicit/ignored quantity changed");
   if(caseUnit&&!Integer.valueOf(240).equals(data.get("caseQuantity")))throw new AssertionError("Case quantity missing");
   if(!caseUnit&&data.containsKey("caseQuantity"))throw new AssertionError("Unexpected case quantity");
   System.out.println("RESULT receiptPackage=true caseUnit="+caseUnit+" ignoreQuantity="+ignore+" OK");
  }
  Map<String,Object> inferred=label("Receipt package",true,true,true,true,null,false);
  if(!Long.valueOf(240).equals(inferred.get("quantity")))throw new AssertionError("Largest package quantity not used");
  System.out.println("RESULT inferredPackageQuantity=240 OK");
 }
}
