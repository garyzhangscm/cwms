package com.garyzhangscm.cwms.outbound.quantity;
import com.garyzhangscm.cwms.outbound.service.ManualPickPreparationScope;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.Map;
@RestController
public class ManualPickPipelineCapabilitiesController {
 @Autowired private ManualPickPreparationScope preparation;
 @GetMapping("/manual-pick-pipeline/capabilities") public Map<String,Object> capabilities(){return Map.of("schema","manual-pick-pipeline-v1","enabled",preparation.enabled(),"requestScopedSource",true,"compactSource",true,"wholePalletEndpoint",true);}
}
