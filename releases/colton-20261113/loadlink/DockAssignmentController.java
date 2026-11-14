package com.garyzhangscm.cwms.outbound.loadlink;
import java.util.*;
import org.springframework.web.bind.annotation.*;
import com.garyzhangscm.cwms.outbound.model.*;
@RestController
@RequestMapping("/loads")
public class DockAssignmentController {
 private final LoadShippingService service;
 public DockAssignmentController(LoadShippingService service){this.service=service;}
 @GetMapping("/dock-assignments") @ClientValidationEndpoint
 public List<Map<String,Object>> list(@RequestParam("warehouseId") long warehouseId,ClientRestriction cr){return service.dockAssignments(warehouseId,cr);}
 @PostMapping("/{id}/dock-assignment") @ClientValidationEndpoint @BillableEndpoint
 public Map<String,Object> assign(@PathVariable("id") long id,@RequestParam("warehouseId") long warehouseId,@RequestBody LoadShippingService.DockRequest request,ClientRestriction cr){return service.assignDock(warehouseId,id,request,cr);}
}
