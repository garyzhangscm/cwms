package com.garyzhangscm.cwms.outbound.allocation;
import com.garyzhangscm.cwms.outbound.model.ClientValidationEndpoint;
import com.garyzhangscm.cwms.outbound.model.BillableEndpoint;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.web.bind.annotation.*;
@RestController
public class AllocationTaskController {
 private final AllocationTaskService tasks;
 public AllocationTaskController(AllocationTaskService tasks){this.tasks=tasks;}
 @ClientValidationEndpoint @GetMapping("/allocation-tasks/capabilities")
 public Map<String,Object> capabilities(@RequestParam("warehouseId") Long warehouseId){Map<String,Object> m=new HashMap<>();m.put("supported",true);m.put("version",1);return m;}
 @BillableEndpoint @ClientValidationEndpoint @PostMapping("/orders/{orderId}/allocation-tasks")
 public AllocationTask submit(@PathVariable("orderId") Long orderId,@RequestParam("warehouseId") Long warehouseId,@RequestBody Map<String,String> request){return tasks.submit(warehouseId,orderId,request.get("requestId"),true);}
 @ClientValidationEndpoint @GetMapping("/allocation-tasks/{id}")
 public AllocationTask get(@PathVariable("id") String id,@RequestParam("warehouseId") Long warehouseId){return tasks.get(warehouseId,id);}
 @ClientValidationEndpoint @GetMapping("/allocation-tasks/latest")
 public List<AllocationTask> latest(@RequestParam("warehouseId") Long warehouseId,@RequestParam("orderIds") String orderIds){return tasks.latest(warehouseId,Arrays.stream(orderIds.split(",")).filter(s->!s.isBlank()).map(Long::valueOf).distinct().collect(Collectors.toList()));}
}
