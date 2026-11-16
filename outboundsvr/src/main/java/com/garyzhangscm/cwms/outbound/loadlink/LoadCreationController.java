package com.garyzhangscm.cwms.outbound.loadlink;

import java.util.Map;
import org.springframework.web.bind.annotation.*;
import com.garyzhangscm.cwms.outbound.model.*;

@RestController
@RequestMapping("/loads")
public class LoadCreationController {
    private final LoadShippingService service;

    public LoadCreationController(LoadShippingService service) {
        this.service = service;
    }

    @PostMapping("/unassigned")
    @ClientValidationEndpoint
    @BillableEndpoint
    public Map<String, Object> create(@RequestParam("warehouseId") long warehouseId,
                                     @RequestBody Map<String, String> request,
                                     ClientRestriction restriction) {
        return service.createUnassignedLoad(warehouseId, request.get("number"));
    }
}
