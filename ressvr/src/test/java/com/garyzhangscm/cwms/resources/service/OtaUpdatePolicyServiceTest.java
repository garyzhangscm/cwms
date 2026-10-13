package com.garyzhangscm.cwms.resources.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

class OtaUpdatePolicyServiceTest {
    @TempDir Path directory;
    @Test void readsOnlyRequestedReleaseAndReflectsRollback() throws Exception {
        Path file = directory.resolve("policy.json");
        OtaUpdatePolicyService service = new OtaUpdatePolicyService(file.toString());
        assertEquals(0, service.policyFor("1.62.4+5").get("minimumPatch"));
        Files.writeString(file, "{\"android\":{\"1.62.4+5\":{\"minimumPatch\":8,\"message\":\"Report fix\"}}}");
        assertEquals(8, service.policyFor("1.62.4+5").get("minimumPatch"));
        assertEquals(0, service.policyFor("1.62.3+4").get("minimumPatch"));
        Files.writeString(file, "{\"android\":{\"1.62.4+5\":{\"minimumPatch\":0}}}");
        assertEquals(0, service.policyFor("1.62.4+5").get("minimumPatch"));
    }
    @Test void endpointRejectsInvalidVersionsAndDisablesCaching() {
        OtaUpdatePolicyService service = new OtaUpdatePolicyService(directory.resolve("policy.json").toString());
        com.garyzhangscm.cwms.resources.controller.OtaUpdatePolicyController controller = new com.garyzhangscm.cwms.resources.controller.OtaUpdatePolicyController(service);
        assertEquals("no-store", controller.getPolicy("1.62.4+5").getHeaders().getCacheControl());
        assertThrows(IllegalArgumentException.class, () -> controller.getPolicy("../policy"));
    }
    @Test void invalidConfigurationDoesNotForceAnUpdate() throws Exception {
        Path file = directory.resolve("policy.json");
        OtaUpdatePolicyService service = new OtaUpdatePolicyService(file.toString());
        Files.writeString(file, "broken");
        assertEquals(0, service.policyFor("1.62.4+5").get("minimumPatch"));
        Files.writeString(file, "{\"android\":{\"1.62.4+5\":{\"minimumPatch\":-1}}}");
        assertEquals(0, service.policyFor("1.62.4+5").get("minimumPatch"));
    }
}
