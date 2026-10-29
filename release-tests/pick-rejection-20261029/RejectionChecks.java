import com.garyzhangscm.cwms.outbound.service.ManualPickRejection;import com.garyzhangscm.cwms.outbound.model.*;
public class RejectionChecks {
 static void check(String s,String code){if(!s.contains("\"code\":\""+code+"\""))throw new AssertionError(s);}
 public static void main(String[] args){Inventory i=new Inventory();i.setQuantity(12L);Location l=new Location();l.setName("TRANS_SHIPPING");check(ManualPickRejection.message("L1",i,l),"TRANSFER_SHIPPING");l.setName("C1");check(ManualPickRejection.message("L1",i,l),"NOT_PICKABLE");i.setPickId(3L);check(ManualPickRejection.message("L1",i,l),"ALREADY_ASSIGNED");i.setPickId(null);l.setEnabled(false);check(ManualPickRejection.message("L1",i,l),"LOCATION_DISABLED");check(ManualPickRejection.message("L1",i,null),"NOT_PICKABLE");System.out.println("PASS verified transfer, assigned, disabled, unknown and missing location explanations");}
}
