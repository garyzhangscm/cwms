import com.garyzhangscm.cwms.outbound.model.*;
import java.time.ZonedDateTime;
import java.util.*;
public class LegacyManualPickNullDates {
 static int checks;
 static void check(boolean value,String message){checks++;if(!value)throw new AssertionError(message);}
 static Inventory inventory(String lpn,String name,String date){Inventory i=new Inventory();i.setLpn(lpn);i.setQuantity(72L);if(name!=null){Location l=new Location();l.setName(name);i.setLocation(l);}i.setLocationId(59L);if(date!=null)i.setInWarehouseDatetime(ZonedDateTime.parse(date));return i;}
 public static void main(String[] args){
  Inventory known=inventory("KNOWN","A1","2026-10-01T00:00:00Z"),missing=inventory("NO-DATE","A1",null),other=inventory("OTHER","B1",null);
  List<Inventory> input=new ArrayList<>(Arrays.asList(missing,other,known));OrderLine line=new OrderLine();line.setupManualPickableInventoryForDisplay(input);String text=String.join("\n",line.getManualPickableInventoryForDisplay());
  check(text.contains("A1")&&text.contains("B1"),"Locations retained");check(text.contains("KNOWN")&&text.contains("NO-DATE")&&text.contains("OTHER"),"All candidate LPNs retained");check(text.contains("72"),"Quantities retained");check(text.contains("Unknown"),"Missing dates explicitly unknown");check(text.indexOf("KNOWN")<text.indexOf("NO-DATE"),"Known dates sort before unknown in same location");
  check(missing.getInWarehouseDatetime()==null&&missing.getQuantity()==72,"Source inventory never changed");check(input.get(0)==missing&&input.get(1)==other,"Candidate list never reordered in place");
  line.setupManualPickableInventoryForDisplay(Arrays.asList(missing,other));check(line.getManualPickableInventoryForDisplay().size()==4,"All missing dates supported");
  line.setupManualPickableInventoryForDisplay(Collections.emptyList());check(line.getManualPickableInventoryForDisplay().isEmpty(),"Empty candidates supported");
  line.setupManualPickableInventoryForDisplay(Arrays.asList(inventory("MISSING-LOCATION",null,null)));check(String.join("",line.getManualPickableInventoryForDisplay()).contains("Unknown location"),"Missing location display preserved");
  System.out.println("RESULT "+checks+" manual-pick null-date cases passed; no database, network or printing");
 }
}
