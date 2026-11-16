package com.garyzhangscm.cwms.outbound.loadlink;
import java.util.*;
import org.springframework.web.bind.annotation.*;
import com.garyzhangscm.cwms.outbound.model.*;
@RestController @RequestMapping("/loads/yard-visits")
public class YardVisitController {
 private final LoadShippingService service;
 public YardVisitController(LoadShippingService service){this.service=service;}
 @GetMapping @ClientValidationEndpoint
 public List<Map<String,Object>> list(@RequestParam("warehouseId") long w,@RequestParam(value="history",defaultValue="false") boolean history,ClientRestriction cr){return service.yardVisits(w,history,cr);}
 @PostMapping @ClientValidationEndpoint @BillableEndpoint
 public Map<String,Object> checkIn(@RequestParam("warehouseId") long w,@RequestBody YardVisitStore.Request r,ClientRestriction cr){return service.yardCheckIn(w,r,cr);}
 @PostMapping("/{id}/{action}") @ClientValidationEndpoint @BillableEndpoint
 public Map<String,Object> change(@PathVariable("id") long id,@PathVariable("action") String action,@RequestParam("warehouseId") long w,@RequestBody YardVisitStore.Request r,ClientRestriction cr){return service.yardChange(w,id,action,r,cr);}
}
