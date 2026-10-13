package com.garyzhangscm.cwms.resources.controller;

import com.garyzhangscm.cwms.resources.service.OtaUpdatePolicyService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.ResponseEntity;
import java.util.Map;

@RestController
public class OtaUpdatePolicyController {
    private final OtaUpdatePolicyService service;
    public OtaUpdatePolicyController(OtaUpdatePolicyService service) { this.service = service; }
    @GetMapping("/rf-app-version/ota-policy")
    public ResponseEntity<Map<String, Object>> getPolicy(@RequestParam("releaseVersion") String releaseVersion) {
        if (!releaseVersion.matches("[0-9]+\\.[0-9]+\\.[0-9]+\\+[0-9]+") || releaseVersion.length() > 50) {
            throw new IllegalArgumentException("Invalid release version");
        }
        return ResponseEntity.ok().header("Cache-Control", "no-store").body(service.policyFor(releaseVersion));
    }
}
