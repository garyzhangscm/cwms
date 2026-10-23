package com.garyzhangscm.cwms.inventory.controller;
import com.garyzhangscm.cwms.inventory.model.ClientValidationEndpoint;
import com.garyzhangscm.cwms.inventory.model.ClientRestriction;
import com.garyzhangscm.cwms.inventory.service.RfidItemLookupService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(name="mes.rfid.lookup.enabled", havingValue="true")
@RestController
public class RfidItemLookupController {
    @Autowired private RfidItemLookupService lookup;
    @ClientValidationEndpoint
    @GetMapping("/items-query/rfid-upcs")
    public List<Map<String, Object>> findRfidItems(@RequestParam Long companyId,
            @RequestParam Long warehouseId, @RequestParam String upcs,
            ClientRestriction clientRestriction) {
        return lookup.lookup(companyId, warehouseId, upcs, clientRestriction);
    }
}
