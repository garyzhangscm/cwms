import com.garyzhangscm.cwms.inventory.service.RfidItemLookupService;
import org.springframework.web.server.ResponseStatusException;
public class LookupCheck {
 public static void main(String[] args) {
  var v = RfidItemLookupService.variants("00618842526307,618842526307");
  if (!v.contains("618842526307") || !v.contains("0618842526307") || v.size()!=3) throw new AssertionError(v);
  if (RfidItemLookupService.variants("10618842526304").size()!=1) throw new AssertionError("case-level GTIN collapsed");
  for (String bad : new String[]{"", "618842526308", "x", "618842526307,", String.join(",", java.util.Collections.nCopies(51,"618842526307"))}) {
   try { RfidItemLookupService.variants(bad); throw new AssertionError("accepted invalid input"); }
   catch (ResponseStatusException expected) {}
  }
  System.out.println("UPC normalization, GTIN indicator and input limits passed");
 }
}
