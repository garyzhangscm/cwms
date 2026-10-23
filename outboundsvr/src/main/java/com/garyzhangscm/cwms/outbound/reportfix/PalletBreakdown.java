package com.garyzhangscm.cwms.outbound.reportfix;
import java.util.*;
import com.garyzhangscm.cwms.outbound.model.*;
/** Advisory standard-pack conversion, independent of actual LPNs and inventory allocation. */
public final class PalletBreakdown {
 private static boolean isPallet(String name){return name!=null&&Set.of("PL","PLT","PAL","PALLET","PALLETS").contains(name.trim().toUpperCase(Locale.ROOT));}
 public static Map<String,Object> calculate(Item item,long quantity){
  Map<String,Object> result=new HashMap<>();result.put("unitsPerPallet",null);result.put("fullPallets",null);result.put("looseQuantity",quantity);result.put("stockUnit","");result.put("palletStatus","MISSING");
  if(item==null)return result;
  ItemPackageType pack=item.getDefaultItemPackageType();if(pack==null)return result;
  List<ItemPackageType> packs=item.getItemPackageTypes();
  if(packs!=null&&packs.size()>1&&(!Boolean.TRUE.equals(pack.getDefaultFlag())||packs.stream().filter(p->Boolean.TRUE.equals(p.getDefaultFlag())).count()!=1)){
   result.put("palletStatus","AMBIGUOUS");return result;
  }
  ItemUnitOfMeasure stock=pack.getStockItemUnitOfMeasure();
  if(stock==null||stock.getUnitOfMeasure()==null||stock.getQuantity()==null||stock.getQuantity()!=1L){result.put("palletStatus","INVALID");return result;}
  result.put("stockUnit",stock.getUnitOfMeasure().getName());
  Set<Long> sizes=new HashSet<>();boolean invalid=false;
  for(ItemUnitOfMeasure u:pack.getItemUnitOfMeasures())if(u!=null&&u.getUnitOfMeasure()!=null&&isPallet(u.getUnitOfMeasure().getName())){
   if(u.getQuantity()==null||u.getQuantity()<=0)invalid=true;else sizes.add(u.getQuantity());
  }
  if(invalid||sizes.size()>1){result.put("palletStatus",invalid?"INVALID":"AMBIGUOUS");return result;}
  if(sizes.isEmpty())return result;
  long size=sizes.iterator().next();result.put("unitsPerPallet",size);result.put("fullPallets",quantity/size);result.put("looseQuantity",quantity%size);result.put("palletStatus","OK");return result;
 }
}
