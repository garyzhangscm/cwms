package com.garyzhangscm.cwms.outbound.transit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import javax.servlet.http.HttpServletRequest;
import java.util.*;

@RestController
public class TransitShippingController {
    @Autowired private TransitShippingService service;
    @Autowired private TransitRuleAccess access;
    @GetMapping("/in-transit-shipping/rules")
    public Map<String,Object> get(@RequestParam("warehouseId") Long warehouseId, HttpServletRequest request) {
        access.requireAdmin(request,warehouseId);
        return service.metadata(warehouseId);
    }
    @PutMapping("/in-transit-shipping/rules")
    public TransitShippingConfiguration save(@RequestParam("warehouseId") Long warehouseId,
            @RequestBody TransitShippingConfiguration configuration,HttpServletRequest request) {
        String actor=access.requireAdmin(request,warehouseId);
        return service.save(warehouseId,configuration,actor);
    }
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String,Object>> failure(ResponseStatusException error) {
        Map<String,Object> body=new LinkedHashMap<>();body.put("result",1);body.put("message",error.getReason());
        return ResponseEntity.status(error.getStatus()).body(body);
    }
}
