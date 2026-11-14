package com.garyzhangscm.cwms.outbound.wavereserve;
import com.garyzhangscm.cwms.outbound.model.*;
import org.springframework.web.bind.annotation.*;
import javax.servlet.http.HttpServletRequest;
import java.util.*;
@RestController @RequestMapping("/wave-reserve")
public class WaveReserveController {
 final WaveReserveService service;public WaveReserveController(WaveReserveService service){this.service=service;}
 @GetMapping("/capabilities") public Map<String,Object> capabilities(HttpServletRequest req){service.gate(req);return Map.of("supported",true);}
 @GetMapping @ClientValidationEndpoint public Map<String,Object> list(@RequestParam long warehouseId,@RequestParam String waveIds,ClientRestriction cr,HttpServletRequest req){service.gate(req);return service.list(warehouseId,waveIds,cr);}
 @GetMapping("/{wave}") @ClientValidationEndpoint public Map<String,Object> view(@PathVariable long wave,@RequestParam long warehouseId,ClientRestriction cr,HttpServletRequest req){service.gate(req);return service.view(warehouseId,wave,cr);}
 @PostMapping("/plan/{kind}") @ClientValidationEndpoint public Wave plan(@PathVariable String kind,@RequestParam long warehouseId,@RequestParam(required=false) String waveNumber,@RequestParam(required=false) String comment,@RequestBody List<Long> ids,ClientRestriction cr,HttpServletRequest req){service.gate(req);WaveReserveStore.require(List.of("orders","shipments").contains(kind),"Invalid planning kind");return service.plan(warehouseId,waveNumber,comment,ids,kind.equals("shipments"),cr);}
 @PostMapping("/{wave}/allocate") @ClientValidationEndpoint public Map<String,Object> allocate(@PathVariable long wave,@RequestParam long warehouseId,@RequestParam String requestId,ClientRestriction cr,HttpServletRequest req){service.gate(req);return service.allocate(warehouseId,wave,requestId,cr);}
 @PostMapping("/{wave}/release") @ClientValidationEndpoint public Map<String,Object> release(@PathVariable long wave,@RequestParam long warehouseId,ClientRestriction cr,HttpServletRequest req){service.gate(req);return service.release(warehouseId,wave,cr);}
 @GetMapping("/{wave}/scan-preview") @ClientValidationEndpoint public Map<String,Object> preview(@PathVariable long wave,@RequestParam long warehouseId,@RequestParam long shipmentLineId,@RequestParam String sourceLpn,ClientRestriction cr,HttpServletRequest req){service.gate(req);return service.preview(warehouseId,wave,shipmentLineId,sourceLpn,cr);}
 @PostMapping("/{wave}/scan") @ClientValidationEndpoint public ManualPickOperation scan(@PathVariable long wave,@RequestParam long warehouseId,@RequestBody WaveReserveService.Scan body,ClientRestriction cr,HttpServletRequest req){service.gate(req);return service.scan(warehouseId,wave,body,cr);}
 @PostMapping("/{wave}/scan-cancel") @ClientValidationEndpoint public Map<String,Object> cancelScan(@PathVariable long wave,@RequestParam long warehouseId,@RequestParam long shipmentLineId,@RequestParam String requestId,ClientRestriction cr,HttpServletRequest req){service.gate(req);return service.cancelScan(warehouseId,wave,shipmentLineId,requestId,cr);}
 @PostMapping("/{wave}/cancel-release") @ClientValidationEndpoint public Map<String,Object> cancelRelease(@PathVariable long wave,@RequestParam long warehouseId,ClientRestriction cr,HttpServletRequest req){service.gate(req);return service.cancelAndRelease(warehouseId,wave,cr);}
 @PostMapping("/{wave}/close") @ClientValidationEndpoint public Map<String,Object> close(@PathVariable long wave,@RequestParam long warehouseId,@RequestParam(defaultValue="false") boolean cancel,ClientRestriction cr,HttpServletRequest req){service.gate(req);return service.close(warehouseId,wave,cancel,cr);}
}
