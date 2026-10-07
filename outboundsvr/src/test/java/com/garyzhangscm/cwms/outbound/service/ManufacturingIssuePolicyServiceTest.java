package com.garyzhangscm.cwms.outbound.service;

import com.garyzhangscm.cwms.outbound.clients.WarehouseLayoutServiceRestemplateClient;
import com.garyzhangscm.cwms.outbound.model.*;
import com.garyzhangscm.cwms.outbound.repository.PickRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.Arrays;
import java.util.Collections;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ManufacturingIssuePolicyServiceTest {
    WarehouseLayoutServiceRestemplateClient client;
    PickRepository repository;
    ManufacturingIssuePolicyService service;
    WarehouseConfiguration configuration;
    @BeforeEach void setup() {
        client = mock(WarehouseLayoutServiceRestemplateClient.class);
        repository = mock(PickRepository.class);
        service = new ManufacturingIssuePolicyService(client, repository);
        configuration = new WarehouseConfiguration();
        when(client.getManufacturingIssueConfiguration(1L)).thenReturn(configuration);
        when(repository.findOpenWorkOrderPicksForIssue(1L, 2L, 3L, 4L))
            .thenReturn(Collections.singletonList(pick(10L, "L1")));
    }
    Pick pick(Long source, String lpn) {
        Pick pick = new Pick(); pick.setSourceLocationId(source); pick.setLpn(lpn); return pick;
    }
    void validate(Long source, String lpn) { service.validate(1L, 2L, 3L, 4L, source, lpn); }
    @Test void legacyDefaultsAreStrict() {
        assertDoesNotThrow(() -> validate(10L, "L1"));
        assertThrows(RuntimeException.class, () -> validate(11L, "L1"));
        assertThrows(RuntimeException.class, () -> validate(10L, "L2"));
    }
    @Test void missingConfigurationIsStrict() {
        when(client.getManufacturingIssueConfiguration(1L)).thenReturn(null);
        assertThrows(RuntimeException.class, () -> validate(11L, "L2"));
    }
    @Test void bothDisabledSkipOriginalAllocationQuery() {
        configuration.setManufacturingIssueRequireSourceLocation(false);
        configuration.setManufacturingIssueRequireAllocatedLpn(false);
        assertDoesNotThrow(() -> validate(11L, "L2")); verifyNoInteractions(repository);
    }
    @Test void sourceOnly() {
        configuration.setManufacturingIssueRequireAllocatedLpn(false);
        assertDoesNotThrow(() -> validate(10L, "L2"));
        assertThrows(RuntimeException.class, () -> validate(11L, "L1"));
    }
    @Test void lpnOnly() {
        configuration.setManufacturingIssueRequireSourceLocation(false);
        assertDoesNotThrow(() -> validate(11L, "L1"));
        assertThrows(RuntimeException.class, () -> validate(10L, "L2"));
    }
    @Test void bothChecksMustMatchSamePick() {
        when(repository.findOpenWorkOrderPicksForIssue(1L, 2L, 3L, 4L))
            .thenReturn(Arrays.asList(pick(10L, "L1"), pick(11L, "L2")));
        assertThrows(RuntimeException.class, () -> validate(10L, "L2"));
    }
    @Test void locationOnlyAllocationDoesNotInventAnLpnRequirement() {
        when(repository.findOpenWorkOrderPicksForIssue(1L, 2L, 3L, 4L))
            .thenReturn(Collections.singletonList(pick(10L, null)));
        assertDoesNotThrow(() -> validate(10L, "L2"));
        assertThrows(RuntimeException.class, () -> validate(11L, "L2"));
    }
    @Test void noOpenAllocationUsesExistingManualRules() {
        when(repository.findOpenWorkOrderPicksForIssue(1L, 2L, 3L, 4L)).thenReturn(Collections.emptyList());
        assertDoesNotThrow(() -> validate(11L, "L2"));
        verify(repository).findOpenWorkOrderPicksForIssue(1L, 2L, 3L, 4L);
    }
    @Test void lookupFailureCannotBypassPolicy() {
        when(client.getManufacturingIssueConfiguration(1L)).thenThrow(new IllegalStateException("unavailable"));
        assertThrows(IllegalStateException.class, () -> validate(10L, "L1")); verifyNoInteractions(repository);
    }
    @Test void missingContextFailsBeforeLookup() {
        assertThrows(RuntimeException.class, () -> validate(null, "L1"));
        assertThrows(RuntimeException.class, () -> validate(10L, " "));
        verifyNoInteractions(client, repository);
    }
}
