package com.garyzhangscm.cwms.outbound.quantity;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.Map;
@RestController
public class QuantityAllocationCapabilitiesController {
    @Autowired private QuantityAllocationService service;
    @GetMapping("/quantity-allocation/capabilities")
    public Map<String,Object> capabilities(@RequestParam(required=false) Long warehouseId) {
        return Map.of("enabled",service.enabled(),"defaultForWarehouse",warehouseId!=null&&service.defaultFor(warehouseId),"mode","ITEM_QUANTITY","bindLocationAtAllocation",false,"bindLpnAtAllocation",false,"requiresSavedManualPickRequest",true,"migratesExistingPicks",false,"reserveChecksTotal",true,"pickChecksTotal",!service.pickLpnOnly(),"pickPolicy",service.pickLpnOnly()?"SCANNED_LPN_AND_HELD_QUANTITY":"POOL_REVALIDATION");
    }
}
