package com.garyzhangscm.cwms.common.service;
import com.garyzhangscm.cwms.common.model.UnitOfMeasure;
import com.garyzhangscm.cwms.common.exception.RequestValidationFailException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import javax.persistence.*;
import java.util.*;
@Service
public class UnitLevelService {
 @PersistenceContext private EntityManager em;
 public static class Update {public Long id; public String level;}
 public static class Batch {public List<Update> updates;}
 @Transactional
 public List<UnitOfMeasure> save(Long companyId, Long warehouseId, Batch batch) {
  if(companyId==null || companyId<=0 || warehouseId==null || warehouseId<=0 || batch==null || batch.updates==null || batch.updates.isEmpty() || batch.updates.size()>500) fail("Invalid level update request");
  Map<Long,Update> updates=new TreeMap<>();
  for(Update u:batch.updates) {
   if(u==null || u.id==null || u.id<=0 || updates.put(u.id,u)!=null || (u.level!=null && !Arrays.asList("ITEM","CASE","PALLET").contains(u.level))) fail("Invalid or duplicate unit level");
  }
  List<UnitOfMeasure> saved=new ArrayList<>();
  for(Update u:updates.values()) {
   UnitOfMeasure unit=em.find(UnitOfMeasure.class,u.id,LockModeType.PESSIMISTIC_WRITE);
   if(unit==null || !Objects.equals(companyId,unit.getCompanyId()) || (unit.getWarehouseId()!=null && !Objects.equals(warehouseId,unit.getWarehouseId()))) fail("Unit is outside the selected company or warehouse");
   unit.setLevel(u.level);saved.add(unit);
  }
  em.flush();
  // Check only packages affected by this batch; never rewrite existing footprints.
  String ids=updates.keySet().toString().replace('[','(').replace(']',')');
  List<?> invalid=em.createNativeQuery("SELECT x.item_package_type_id FROM item_unit_of_measure x JOIN unit_of_measure u ON u.unit_of_measure_id=x.unit_of_measure_id WHERE x.item_package_type_id IN (SELECT y.item_package_type_id FROM item_unit_of_measure y WHERE y.unit_of_measure_id IN "+ids+") AND u.packaging_level IS NOT NULL GROUP BY x.item_package_type_id,u.packaging_level HAVING COUNT(*)>1 LIMIT 1").getResultList();
  if(!invalid.isEmpty()) fail("This level would duplicate a level in an existing item package. Adjust that footprint first.");
  if(!em.createNativeQuery("SELECT x.item_unit_of_measure_id FROM item_unit_of_measure x JOIN unit_of_measure u ON u.unit_of_measure_id=x.unit_of_measure_id WHERE x.unit_of_measure_id IN "+ids+" AND u.packaging_level='ITEM' AND (x.weight IS NULL OR x.weight<=0) LIMIT 1").getResultList().isEmpty()) fail("An existing Item level footprint has an empty or zero weight. Update its weight before assigning Item level.");
  return saved;
 }
 private static void fail(String message) {throw RequestValidationFailException.raiseException(message);}
}
