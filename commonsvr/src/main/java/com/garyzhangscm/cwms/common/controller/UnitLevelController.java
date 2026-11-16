package com.garyzhangscm.cwms.common.controller;
import com.garyzhangscm.cwms.common.model.*;
import com.garyzhangscm.cwms.common.service.UnitLevelService;
import com.garyzhangscm.cwms.common.clients.WarehouseLayoutServiceRestemplateClient;
import com.garyzhangscm.cwms.common.exception.RequestValidationFailException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.*;
import org.springframework.web.bind.annotation.*;
import java.util.*;
@RestController
public class UnitLevelController {
 @Autowired private UnitLevelService levels;
 @Autowired private WarehouseLayoutServiceRestemplateClient layout;
 @BillableEndpoint
 @PutMapping("/unit-of-measures/levels")
 @Caching(evict={@CacheEvict(cacheNames="AdminService_UnitOfMeasure",allEntries=true),@CacheEvict(cacheNames="InventoryService_UnitOfMeasure",allEntries=true),@CacheEvict(cacheNames="IntegrationService_UnitOfMeasure",allEntries=true),@CacheEvict(cacheNames="InboundService_UnitOfMeasure",allEntries=true),@CacheEvict(cacheNames="OutboundService_UnitOfMeasure",allEntries=true),@CacheEvict(cacheNames="WorkOrderService_UnitOfMeasure",allEntries=true)})
 public List<UnitOfMeasure> save(@RequestParam Long companyId,@RequestParam Long warehouseId,@RequestBody UnitLevelService.Batch batch) {
  if(!Objects.equals(layout.getWarehouseById(warehouseId).getCompanyId(),companyId)) throw RequestValidationFailException.raiseException("Warehouse does not belong to company");
  return levels.save(companyId,warehouseId,batch);
 }
}
