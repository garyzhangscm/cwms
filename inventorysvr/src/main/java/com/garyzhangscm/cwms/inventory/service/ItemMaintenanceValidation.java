package com.garyzhangscm.cwms.inventory.service;
import com.garyzhangscm.cwms.inventory.model.*;
import com.garyzhangscm.cwms.inventory.clients.CommonServiceRestemplateClient;
import com.garyzhangscm.cwms.inventory.exception.RequestValidationFailException;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.util.*;
/** Validate interactive maintenance without changing import, conversion or receiving workflows. */
@Aspect @Component
public class ItemMaintenanceValidation {
 @Autowired private CommonServiceRestemplateClient common;
 @Before("execution(* com.garyzhangscm.cwms.inventory.controller.ItemController.addItem(..)) || execution(* com.garyzhangscm.cwms.inventory.controller.ItemController.changeItem(..))")
 public void before(JoinPoint point) {
  Object[] args=point.getArgs();Item item=(Item)args[args.length-1];
  validate(item,point.getSignature().getName().equals("addItem"));
 }
 public void validate(Item item,boolean creating) {
  if(item==null)fail("Item is required");
  if(creating) {
   if(blank(item.getName()) || blank(item.getDescription()))fail("Item name and description are required");
   if(item.getItemFamily()==null || item.getItemFamily().getId()==null || item.getItemFamily().getId()<=0)fail("Item family is required");
   if(item.getUnitCost()==null || !Double.isFinite(item.getUnitCost()) || item.getUnitCost()<0)fail("Unit cost must be entered and cannot be negative");
   if(item.getShelfLifeDays()==null || item.getShelfLifeDays()<0)fail("Shelf life days must be entered as a nonnegative whole number");
  }
  Map<Long,UnitOfMeasure> units=new HashMap<>();
  if(item.getItemPackageTypes()==null)return;
  for(ItemPackageType pack:item.getItemPackageTypes()) {
   Set<String> seen=new HashSet<>();
   if(pack.getItemUnitOfMeasures()==null)continue;
   for(ItemUnitOfMeasure row:pack.getItemUnitOfMeasures()) {
    Long id=row.getUnitOfMeasureId();if(id==null && row.getUnitOfMeasure()!=null)id=row.getUnitOfMeasure().getId();
    if(id==null || id<=0)fail("Select an existing Unit of Measure for the footprint");
    UnitOfMeasure unit=units.get(id);if(unit==null){unit=common.getUnitOfMeasureById(id);units.put(id,unit);}
    if(unit==null || !Objects.equals(unit.getCompanyId(),item.getCompanyId()) || (unit.getWarehouseId()!=null && !Objects.equals(unit.getWarehouseId(),item.getWarehouseId())))fail("Unit of Measure is outside this item scope");
    String level=unit.getLevel();
    if(level!=null && !seen.add(level))fail("Only one "+level+" level is allowed in each item package");
    if("ITEM".equals(level) && (row.getWeight()==null || !Double.isFinite(row.getWeight()) || row.getWeight()<=0))fail("Item level weight is required and must be greater than zero");
   }
  }
 }
 private static boolean blank(String s){return s==null || s.trim().isEmpty();}
 private static void fail(String message){throw RequestValidationFailException.raiseException(message);}
}
