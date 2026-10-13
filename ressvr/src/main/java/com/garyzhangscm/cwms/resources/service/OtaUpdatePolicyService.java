package com.garyzhangscm.cwms.resources.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/** Read-only release policy. No accounts, URLs or business records are exposed. */
@Service
public class OtaUpdatePolicyService {
    private final Path policyFile;
    private final ObjectMapper mapper = new ObjectMapper();
    public OtaUpdatePolicyService(@Value("${rf.ota.policy-file:/usr/local/reports/ota-policy.json}") String path) {
        policyFile = Path.of(path);
    }
    public Map<String, Object> policyFor(String release) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("releaseVersion", release);
        result.put("minimumPatch", 0);
        result.put("message", "");
        // Configuration is read on every request, so lowering/clearing a minimum
        // after a rollback does not require a service restart.
        try {
            if (!Files.isRegularFile(policyFile) || Files.size(policyFile) > 16384) return result;
            JsonNode policy = mapper.readTree(Files.readAllBytes(policyFile)).path("android").path(release);
            JsonNode minimum = policy.path("minimumPatch");
            if (!minimum.isIntegralNumber() || !minimum.canConvertToInt() || minimum.intValue() < 0) return result;
            result.put("minimumPatch", minimum.intValue());
            if (policy.path("message").isTextual()) {
                String message = policy.path("message").textValue();
                if (message.length() <= 500) result.put("message", message);
            }
        } catch (Exception ignored) {
            // Missing/bad policy must never make an installed app unusable.
        }
        return result;
    }
}
