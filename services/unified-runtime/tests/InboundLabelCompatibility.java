import com.garyzhangscm.cwms.inbound.model.*;
import com.garyzhangscm.cwms.inbound.service.ReceiptService;
import java.lang.reflect.*;
import java.time.ZonedDateTime;
import java.util.*;
public class InboundLabelCompatibility {
 static Map<String,Object> label(String description) throws Exception { return label(description,true,true); }
 static Map<String,Object> label(String description,boolean withPackage,boolean withStock) throws Exception {
  Item item=new Item();item.setId(1L);item.setName("OFFLINE-TEST");item.setDescription(description);
  ItemPackageType pack=new ItemPackageType();pack.setName("TEST-PACK");
  UnitOfMeasure uom=new UnitOfMeasure();uom.setName("PCS");
  ItemUnitOfMeasure stock=new ItemUnitOfMeasure();stock.setQuantity(1);stock.setUnitOfMeasure(uom);
  if(withStock){pack.setStockItemUnitOfMeasure(stock);pack.setItemUnitOfMeasures(Arrays.asList(stock));} else {pack.setItemUnitOfMeasures(new ArrayList<>());}
  item.setItemPackageTypes(withPackage?Arrays.asList(pack):new ArrayList<>());
  Receipt receipt=new Receipt();receipt.setId(1L);receipt.setNumber("OFFLINE-RECEIPT");receipt.setCheckInTime(ZonedDateTime.parse("2026-10-22T00:00:00Z"));
  ReceiptLine line=new ReceiptLine();line.setId(1L);line.setReceipt(receipt);line.setItem(item);line.setItemId(1L);
  ReceivingLPNLabelFontSize font=new ReceivingLPNLabelFontSize();font.setCharactersPerLine(20);font.setBaseSize(40);font.setBaseSizeLineCount(3);font.setStep(2);
  Method method=ReceiptService.class.getDeclaredMethod("getLPNDocumentContent",ReceiptLine.class,String.class,Long.class,Boolean.class,ReceivingLPNLabelFontSize.class,ReceivingLPNLabelFontSize.class,ReceivingLPNLabelFontSize.class);method.setAccessible(true);
  return (Map<String,Object>)method.invoke(new ReceiptService(),line,"OFFLINE-LPN",27L,false,font,font,font);
 }
 public static void main(String[] args) throws Exception {
  String[] descriptions={"Short description","","   ","This is a very long description for label wrapping compatibility",null};
  for(int i=0;i<descriptions.length;i++){
   try {Map<String,Object> data=label(descriptions[i]);
    if(!Long.valueOf(27).equals(data.get("quantity")) || !"OFFLINE-LPN".equals(data.get("lpn")))throw new AssertionError("Core label fields changed");
    System.out.println("RESULT case="+i+" OK split="+data.keySet().stream().filter(k->k.startsWith("item_description_")).count()+" stockUOM="+data.get("stockUOM")+" barcode="+data.get("barcode"));
   } catch(InvocationTargetException e){System.out.println("RESULT case="+i+" ERROR="+e.getCause().getClass().getSimpleName());}
  }
  for(int i=5;i<7;i++) {
   try {Map<String,Object> data=label("Valid description",i!=5,i!=6);System.out.println("RESULT case="+i+" OK quantity="+data.get("quantity"));}
   catch(InvocationTargetException e){System.out.println("RESULT case="+i+" ERROR="+e.getCause().getClass().getSimpleName());}
  }
 }
}
