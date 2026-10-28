package com.garyzhangscm.cwms.outbound.quantity;

import com.garyzhangscm.cwms.outbound.model.ClientRestriction;
import com.garyzhangscm.cwms.outbound.model.ClientValidationEndpoint;
import com.garyzhangscm.cwms.outbound.model.BillableEndpoint;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/orders/{id}/quantity-allocation")
public class QuantityAllocationController {
    @Autowired private QuantityAllocationService service;
    @GetMapping @ClientValidationEndpoint
    public Map<String,Object> view(@PathVariable Long id,@RequestParam Long warehouseId,ClientRestriction restriction) {
        return service.view(warehouseId,id,restriction);
    }
    @PostMapping("/entry") @ClientValidationEndpoint @BillableEndpoint
    public Map<String,Object> entry(@PathVariable Long id,@RequestParam Long warehouseId,ClientRestriction restriction) {
        service.requireAllocationPermission(warehouseId);
        return service.entry(warehouseId,id,restriction);
    }
    @PostMapping("/enable") @ClientValidationEndpoint @BillableEndpoint
    public Map<String,Object> enable(@PathVariable Long id,@RequestParam Long warehouseId,ClientRestriction restriction) {
        service.requireAllocationPermission(warehouseId);
        return service.enable(warehouseId,id,restriction);
    }
    @PostMapping @ClientValidationEndpoint @BillableEndpoint
    public Map<String,Object> update(@PathVariable Long id,@RequestParam Long warehouseId,@RequestBody QuantityAllocationService.Request request,ClientRestriction restriction) {
        service.requireAllocationPermission(warehouseId);
        return service.update(warehouseId,id,restriction,request);
    }
}
