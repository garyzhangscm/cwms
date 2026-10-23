import java.util.*;import java.nio.file.*;import java.awt.image.BufferedImage;import javax.imageio.ImageIO;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.garyzhangscm.cwms.outbound.model.*;import com.garyzhangscm.cwms.outbound.reportfix.*;
import net.sf.jasperreports.engine.*;import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
public class PalletPickSheetCheck {
 static int checks;static void check(boolean b,String m){checks++;if(!b)throw new AssertionError(m);}
 static ItemUnitOfMeasure uom(String name,long qty){UnitOfMeasure u=new UnitOfMeasure();u.setName(name);ItemUnitOfMeasure n=new ItemUnitOfMeasure();n.setQuantity(qty);n.setUnitOfMeasure(u);return n;}
 static Item item(String name,ItemUnitOfMeasure...units){Item item=new Item();item.setName(name);item.setDescription("Standard packaging example");ItemPackageType p=new ItemPackageType();p.setDefaultFlag(true);p.setItemUnitOfMeasures(new ArrayList<>(List.of(units)));item.setDefaultItemPackageType(p);item.setItemPackageTypes(List.of(p));return item;}
 static String text(JRPrintElement e){String s=e instanceof JRPrintText?((JRPrintText)e).getFullText():"";if(e instanceof JRPrintFrame)for(JRPrintElement c:((JRPrintFrame)e).getElements())s+="\n"+text(c);return s;}
 public static void main(String[] a)throws Exception{
  Item item=item("STANDARD-36",uom("PCS",1),uom("PL",36));
  for(long[] c:new long[][]{{72,2,0},{90,2,18},{18,0,18},{36,1,0},{0,0,0},{Long.MAX_VALUE,Long.MAX_VALUE/36,Long.MAX_VALUE%36}}){Map<String,Object> r=PalletBreakdown.calculate(item,c[0]);check(r.get("fullPallets").equals(c[1])&&r.get("looseQuantity").equals(c[2]),"Integer division/remainder");}
  check("MISSING".equals(PalletBreakdown.calculate(null,90).get("palletStatus")),"Missing item");
  check("MISSING".equals(PalletBreakdown.calculate(item("CASE-ONLY",uom("PCS",1),uom("CASE",12)),90).get("palletStatus")),"Case is not pallet");
  check("AMBIGUOUS".equals(PalletBreakdown.calculate(item("CONFLICT",uom("PCS",1),uom("PL",36),uom("PALLET",48)),90).get("palletStatus")),"Conflicting pallet sizes");
  check("INVALID".equals(PalletBreakdown.calculate(item("INVALID",uom("PCS",1),uom("PL",0)),90).get("palletStatus")),"Invalid pallet count");
  check("INVALID".equals(PalletBreakdown.calculate(item("STOCK",uom("PCS",2),uom("PL",36)),90).get("palletStatus")),"Do not guess stock conversion");
  check("LB".equals(PalletBreakdown.calculate(item("WEIGHT",uom("LB",1),uom("PALLET",100)),118).get("stockUnit")),"Weight must not become pieces");
  Item ambiguous=item("NO-DEFAULT",uom("PCS",1),uom("PL",36));ItemPackageType extra=new ItemPackageType();ambiguous.getDefaultItemPackageType().setDefaultFlag(false);ambiguous.setItemPackageTypes(List.of(ambiguous.getDefaultItemPackageType(),extra));
  check("AMBIGUOUS".equals(PalletBreakdown.calculate(ambiguous,90).get("palletStatus")),"Multiple packs without an explicit default");
  Order o=new Order();List<OrderLine> lines=new ArrayList<>();int i=0;
  for(long quantity:new long[]{72,90,18,36}){OrderLine l=new OrderLine();l.setId((long)++i);l.setNumber(""+i);l.setOpenQuantity(quantity);l.setItem(item);lines.add(l);}
  OrderLine missing=new OrderLine();missing.setId(5L);missing.setNumber("5");missing.setOpenQuantity(15L);missing.setItem(item("MISSING-PACK",uom("PCS",1)));lines.add(missing);o.setOrderLines(lines);
  // JSON round-trip reproduces the actual Outbound -> Resource wire format.
  ObjectMapper mapper=new ObjectMapper();List<?> data=mapper.readValue(mapper.writeValueAsBytes(ManualPickDemandRows.build(o,List.of())),List.class);
  JasperReport report=JasperCompileManager.compileReport(a[0]);Map<String,Object> params=new HashMap<>();params.put("order_number","PALLET-PREVIEW");params.put("destination_location","DROP-OFF-TEST");params.put(JRParameter.REPORT_LOCALE,Locale.US);
  JasperPrint print=JasperFillManager.fillReport(report,params,new JRBeanCollectionDataSource(data));String text="";
  for(JRPrintPage page:print.getPages())for(JRPrintElement e:page.getElements())text+="\n"+text(e);
  for(String value:List.of("2 Pallets","1 Pallet","2 Pallets + 18 PCS","18 PCS (partial pallet)","36 PCS/Pallet","Pallet size not configured"))check(text.contains(value),"Rendered value missing: "+value);
  Path out=Path.of(a[1]);Files.createDirectories(out);ImageIO.write((BufferedImage)JasperPrintManager.printPageToImage(print,0,1.5f),"png",out.resolve("pallet-pick-sheet-preview.png").toFile());
  params.put("destination_location",null);JasperFillManager.fillReport(report,params,new JRBeanCollectionDataSource(data));checks++;
  System.out.println("PASS "+checks+" pallet calculation, actual JSON/Jasper render and empty drop-off checks");
 }
}
