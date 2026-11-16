package com.garyzhangscm.cwms.outbound.loadlink;

import java.util.Map;
import org.springframework.web.bind.annotation.*;
import com.garyzhangscm.cwms.outbound.model.*;

@RestController
@RequestMapping("/loads")
public class LoadPalletController {
    private final LoadShippingService service;

    public LoadPalletController(LoadShippingService service) {
        this.service = service;
    }

    @GetMapping("/pallet-counts")
    @ClientValidationEndpoint
    public Map<Long, Long> palletCounts(@RequestParam("warehouseId") long warehouseId, ClientRestriction restriction) {
        return service.palletCounts(warehouseId, restriction);
    }
}
