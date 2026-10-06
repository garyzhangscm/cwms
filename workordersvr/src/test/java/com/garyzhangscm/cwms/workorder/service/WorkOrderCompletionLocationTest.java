package com.garyzhangscm.cwms.workorder.service;

import com.garyzhangscm.cwms.workorder.clients.WarehouseLayoutServiceRestemplateClient;
import com.garyzhangscm.cwms.workorder.exception.WorkOrderException;
import com.garyzhangscm.cwms.workorder.model.*;
import org.junit.jupiter.api.*;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class WorkOrderCompletionLocationTest {
    WorkOrderCompleteTransactionService service;
    ProductionLineAssignmentService assignments;
    WarehouseLayoutServiceRestemplateClient layout;
    WorkOrderCompleteTransaction transaction;
    @BeforeEach void setup() {
        service=spy(new WorkOrderCompleteTransactionService());
        assignments=mock(ProductionLineAssignmentService.class);
        layout=mock(WarehouseLayoutServiceRestemplateClient.class);
        ReflectionTestUtils.setField(service,"productionLineAssignmentService",assignments);
        ReflectionTestUtils.setField(service,"warehouseLayoutServiceRestemplateClient",layout);
        transaction=new WorkOrderCompleteTransaction();
        WorkOrder order=new WorkOrder();order.setId(259L);transaction.setWorkOrder(order);
        doReturn(transaction).when(service).startNewTransaction(eq(transaction),nullable(Location.class));
    }
    ReturnMaterialRequest addReturn(Long locationId) {
        var line=new WorkOrderLineCompleteTransaction();
        var request=new ReturnMaterialRequest();request.setQuantity(10L);request.setLocationId(locationId);
        line.getReturnMaterialRequests().add(request);transaction.getWorkOrderLineCompleteTransactions().add(line);
        return request;
    }
    @Test void noInventoryOperationsNeedNoLocationOrAssignment() {
        assertSame(transaction,service.startNewTransaction(1L,transaction,null));
        verifyNoInteractions(assignments,layout);
        verify(service).startNewTransaction(transaction,(Location)null);
    }
    @Test void explicitRequestDestinationDoesNotNeedProductionLine() {
        addReturn(42L);service.startNewTransaction(1L,transaction,null);
        verifyNoInteractions(assignments,layout);
    }
    @Test void missingReturnDestinationStillBlocksCompletion() {
        addReturn(null);
        when(assignments.findAll(1L,null,null,259L,null,false)).thenReturn(List.of());
        assertThrows(WorkOrderException.class,()->service.startNewTransaction(1L,transaction,null));
        verify(service,never()).startNewTransaction(eq(transaction),nullable(Location.class));
    }
    @Test void explicitDefaultDestinationIsLookedUpOnce() {
        addReturn(null);Location destination=new Location();destination.setId(42L);
        when(layout.getLocationById(42L)).thenReturn(destination);
        service.startNewTransaction(1L,transaction,42L);
        verifyNoInteractions(assignments);verify(layout,times(1)).getLocationById(42L);
        verify(service).startNewTransaction(transaction,destination);
    }
    @Test void choosesAssignmentWithConfiguredStagingLocation() {
        addReturn(null);var first=new ProductionLineAssignment();first.setProductionLine(new ProductionLine());
        var second=new ProductionLineAssignment();var line=new ProductionLine();line.setOutboundStageLocationId(42L);second.setProductionLine(line);
        when(assignments.findAll(1L,null,null,259L,null,false)).thenReturn(List.of(first,second));
        var destination=new Location();when(layout.getLocationById(42L)).thenReturn(destination);
        service.startNewTransaction(1L,transaction,null);
        verify(service).startNewTransaction(transaction,destination);
    }
    @Test void byProductInventoryAlsoRequiresDestination() {
        var request=new WorkOrderByProductProduceTransaction();request.setLpn("TEST");request.setQuantity(10L);
        request.setInventoryStatus(new InventoryStatus());request.setItemPackageType(new ItemPackageType());
        transaction.getWorkOrderByProductProduceTransactions().add(request);
        when(assignments.findAll(1L,null,null,259L,null,false)).thenReturn(List.of());
        assertThrows(WorkOrderException.class,()->service.startNewTransaction(1L,transaction,null));
        request.setLocationId(42L);service.startNewTransaction(1L,transaction,null);
        verifyNoInteractions(layout);
    }
    @Test void unusedByProductDefinitionDoesNotRequireLocation() {
        transaction.getWorkOrderByProductProduceTransactions().add(new WorkOrderByProductProduceTransaction());
        service.startNewTransaction(1L,transaction,null);verifyNoInteractions(assignments,layout);
    }
    @Test void quantityBalanceValidationStillRunsWithoutLocation() {
        var real=new WorkOrderCompleteTransactionService();
        var line=new WorkOrderLine();line.setNumber("1");line.setDeliveredQuantity(10L);line.setConsumedQuantity(0L);
        var completion=new WorkOrderLineCompleteTransaction();completion.setWorkOrderLine(line);
        transaction.getWorkOrderLineCompleteTransactions().add(completion);
        assertThrows(WorkOrderException.class,()->real.startNewTransaction(1L,transaction,null));
    }
    @Test void zeroMaterialCompletionRunsSettlementKpiAndStatusUpdate() {
        var real=spy(new WorkOrderCompleteTransactionService());
        var orderService=mock(WorkOrderService.class);
        var lineService=mock(WorkOrderLineService.class);
        var kpiService=mock(WorkOrderKPITransactionService.class);
        var inventory=mock(com.garyzhangscm.cwms.workorder.clients.InventoryServiceRestemplateClient.class);
        ReflectionTestUtils.setField(real,"workOrderService",orderService);
        ReflectionTestUtils.setField(real,"workOrderLineService",lineService);
        ReflectionTestUtils.setField(real,"workOrderKPITransactionService",kpiService);
        ReflectionTestUtils.setField(real,"inventoryServiceRestemplateClient",inventory);
        ReflectionTestUtils.setField(real,"productionLineAssignmentService",assignments);
        ReflectionTestUtils.setField(real,"warehouseLayoutServiceRestemplateClient",layout);
        doReturn(transaction).when(real).save(transaction);
        var line=new WorkOrderLine();line.setNumber("1");line.setDeliveredQuantity(0L);line.setConsumedQuantity(0L);
        var completion=new WorkOrderLineCompleteTransaction();completion.setWorkOrderLine(line);
        transaction.getWorkOrderLineCompleteTransactions().add(completion);
        assertSame(transaction,real.startNewTransaction(1L,transaction,null));
        verify(lineService).completeWorkOrderLine(line,0L,0L,0L);
        verify(kpiService).processWorkOrderKIPTransaction(transaction);
        verify(orderService).completeWorkOrder(transaction.getWorkOrder());
        verifyNoInteractions(assignments,layout,inventory);
    }

    @Test void materialSettlementPersistsWithoutLoadingDisplayDetails() {
        var lines=spy(new WorkOrderLineService());
        var line=new WorkOrderLine();line.setInprocessQuantity(100L);
        doReturn(line).when(lines).saveOrUpdate(line,false);
        assertSame(line,lines.completeWorkOrderLine(line,70L,10L,20L));
        assertEquals(70L,line.getConsumedQuantity());assertEquals(10L,line.getScrappedQuantity());
        assertEquals(20L,line.getReturnedQuantity());assertEquals(0L,line.getInprocessQuantity());
        verify(lines).saveOrUpdate(line,false);
        verify(lines,never()).loadAttribute(any(WorkOrderLine.class),anyBoolean(),anyBoolean());
    }

}
