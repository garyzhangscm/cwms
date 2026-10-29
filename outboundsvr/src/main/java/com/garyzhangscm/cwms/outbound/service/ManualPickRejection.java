package com.garyzhangscm.cwms.outbound.service;
import com.garyzhangscm.cwms.outbound.model.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;
/** Read-only explanation after a verified picking rejection; never changes eligibility. */
public final class ManualPickRejection {
 public static String message(String lpn,Inventory stock,Location location) {
  String code="NOT_PICKABLE",reason="This pallet is not available for picking. Ask your supervisor to check its inventory status and location.";
  String next="Scan another pallet. Ask your supervisor to verify this pallet before using it.";
  String name=location==null?"Unavailable":Objects.toString(location.getName(),"Unavailable");
  LocationGroup group=location==null?null:location.getLocationGroup();LocationGroupType type=group==null?null:group.getLocationGroupType();
  if(stock.getPickId()!=null||stock.getAllocatedByPickId()!=null){code="ALREADY_ASSIGNED";reason="This pallet is already assigned to a picking operation and cannot be used for this pick.";}
  else if("TRANS_SHIPPING".equalsIgnoreCase(name)||group!=null&&"TRANS_SHIPPING".equalsIgnoreCase(group.getName())||type!=null&&"TRANS_SHIPPING".equalsIgnoreCase(type.getName())){code="TRANSFER_SHIPPING";reason="This pallet is already in the transfer shipping area and cannot be picked from this location.";}
  else if(type!=null&&Boolean.TRUE.equals(type.getShippingStage())){code="SHIPPING_AREA";reason="This pallet is already in a shipping area and cannot be picked from this location.";}
  else if(location!=null&&Boolean.FALSE.equals(location.getEnabled())){code="LOCATION_DISABLED";reason="This pallet is in a disabled location.";}
  else if(group!=null&&Boolean.FALSE.equals(group.getPickable())){code="LOCATION_NOT_PICKABLE";reason="Picking is not allowed from this pallet's current location.";}
  else if(stock.getQuantity()!=null&&stock.getQuantity()<=0){code="NO_AVAILABLE_QUANTITY";reason="This pallet has no available quantity to pick.";}
  try{return "PICK_REJECTION_V1:"+new ObjectMapper().writeValueAsString(Map.of("code",code,"title","Pallet unavailable for picking","lpn",lpn,"location",name,"reason",reason,"nextAction",next));}
  catch(Exception e){return "Pallet unavailable for picking. Scan another pallet or ask your supervisor to verify its status.";}
 }
 private ManualPickRejection(){}
}
