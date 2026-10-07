package com.garyzhangscm.cwms.outbound.service;

import com.garyzhangscm.cwms.outbound.model.*;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.Collections;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ManufacturingManualPickPolicyTest {
    @Test void rejectedPolicyPreventsAllocationCreation() {
        PickService service = new PickService();
        ManufacturingIssuePolicyService policy = mock(ManufacturingIssuePolicyService.class);
        AllocationService allocator = mock(AllocationService.class);
        ReflectionTestUtils.setField(service, "manufacturingIssuePolicyService", policy);
        ReflectionTestUtils.setField(service, "allocationService", allocator);
        WorkOrder order = new WorkOrder(); order.setWarehouseId(1L);
        WorkOrderLine line = new WorkOrderLine(); line.setId(2L);
        Item item = new Item(); item.setId(3L);
        ProductionLine productionLine = new ProductionLine(); productionLine.setId(5L);
        Location destination = new Location(); destination.setId(4L);
        productionLine.setInboundStageLocation(destination);
        ProductionLineAssignment assignment = new ProductionLineAssignment(); assignment.setProductionLine(productionLine);
        order.setProductionLineAssignments(Collections.singletonList(assignment));
        Location source = new Location(); source.setId(10L);
        doThrow(new IllegalStateException("policy rejected")).when(policy).validate(1L, 2L, 3L, 4L, 10L, "L1");
        assertThrows(IllegalStateException.class, () -> service.generateManualPickForWorkOrder(order, line, item, 5L, source, 10L, "L1"));
        verify(policy).validate(1L, 2L, 3L, 4L, 10L, "L1");
        verifyNoInteractions(allocator);
    }
    @Test void wrongWarehouseRejectedBeforeInventoryLookup() {
        PickService service = new PickService();
        WorkOrder order = new WorkOrder(); order.setWarehouseId(2L);
        assertThrows(RuntimeException.class, () -> service.generateManualPickForWorkOrder(1L, order, 5L, "L1", 10L));
    }
}
