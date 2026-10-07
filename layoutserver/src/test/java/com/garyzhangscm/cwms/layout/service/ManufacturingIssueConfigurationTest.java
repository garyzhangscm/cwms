package com.garyzhangscm.cwms.layout.service;

import com.garyzhangscm.cwms.layout.model.*;
import com.garyzhangscm.cwms.layout.repository.WarehouseConfigurationRepository;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class ManufacturingIssueConfigurationTest {
    @Test public void oldClientsPreserveDisabledRules() {
        WarehouseConfigurationRepository repository = mock(WarehouseConfigurationRepository.class);
        WarehouseConfigurationService service = new WarehouseConfigurationService();
        ReflectionTestUtils.setField(service, "warehouseConfigurationRepository", repository);
        Warehouse warehouse = new Warehouse(); warehouse.setId(1L);
        WarehouseConfiguration existing = new WarehouseConfiguration(); existing.setId(5L);
        existing.setManufacturingIssueRequireSourceLocation(false);
        existing.setManufacturingIssueRequireAllocatedLpn(false);
        when(repository.findByWarehouse(1L)).thenReturn(existing);
        WarehouseConfiguration incoming = new WarehouseConfiguration(); incoming.setWarehouse(warehouse);
        service.saveOrUpdate(incoming);
        assertEquals(Long.valueOf(5), incoming.getId());
        assertEquals(Boolean.FALSE, incoming.getManufacturingIssueRequireSourceLocation());
        assertEquals(Boolean.FALSE, incoming.getManufacturingIssueRequireAllocatedLpn());
    }
    @Test public void newConfigurationDefaultsStrictAndExplicitChoicesSurvive() {
        WarehouseConfigurationRepository repository = mock(WarehouseConfigurationRepository.class);
        WarehouseConfigurationService service = new WarehouseConfigurationService();
        ReflectionTestUtils.setField(service, "warehouseConfigurationRepository", repository);
        Warehouse warehouse = new Warehouse(); warehouse.setId(1L);
        WarehouseConfiguration incoming = new WarehouseConfiguration(); incoming.setWarehouse(warehouse);
        incoming.setManufacturingIssueRequireAllocatedLpn(false);
        service.saveOrUpdate(incoming);
        assertEquals(Boolean.TRUE, incoming.getManufacturingIssueRequireSourceLocation());
        assertEquals(Boolean.FALSE, incoming.getManufacturingIssueRequireAllocatedLpn());
    }
}
