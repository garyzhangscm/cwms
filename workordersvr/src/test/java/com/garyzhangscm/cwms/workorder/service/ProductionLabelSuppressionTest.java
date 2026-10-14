package com.garyzhangscm.cwms.workorder.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.garyzhangscm.cwms.workorder.clients.*;
import com.garyzhangscm.cwms.workorder.model.*;
import org.junit.jupiter.api.*;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ProductionLabelSuppressionTest {
    WorkOrderProduceTransactionService service;
    InventoryServiceRestemplateClient inventoryClient;
    WarehouseLayoutServiceRestemplateClient layout;
    ResourceServiceRestemplateClient resource;
    WorkOrderService orders;
    WorkOrder order;
    WorkOrderProducedInventory produced;
    WorkOrderProduceTransaction transaction;
    Inventory inventory;

    @BeforeEach void setup() throws Exception {
        service = new WorkOrderProduceTransactionService();
        inventoryClient = mock(InventoryServiceRestemplateClient.class);
        layout = mock(WarehouseLayoutServiceRestemplateClient.class);
        resource = mock(ResourceServiceRestemplateClient.class);
        orders = mock(WorkOrderService.class);
        ReflectionTestUtils.setField(service, "inventoryServiceRestemplateClient", inventoryClient);
        ReflectionTestUtils.setField(service, "warehouseLayoutServiceRestemplateClient", layout);
        ReflectionTestUtils.setField(service, "resourceServiceRestemplateClient", resource);
        ReflectionTestUtils.setField(service, "workOrderService", orders);
        order = new WorkOrder(); order.setWarehouseId(1L); order.setNumber("TEST");
        transaction = new WorkOrderProduceTransaction();
        ProductionLine line = new ProductionLine(); line.setName("TEST-LINE");
        transaction.setProductionLine(line);
        produced = mock(WorkOrderProducedInventory.class);
        when(produced.getLpn()).thenReturn("L0000116016");
        inventory = new Inventory(); inventory.setLpn("L0000116016"); inventory.setQuantity(5L);
        when(produced.createInventory(order, transaction)).thenReturn(inventory);
        when(inventoryClient.receiveInventoryFromWorkOrder(order, inventory)).thenReturn(inventory);
        when(inventoryClient.findInventoryByLPN(1L, "L0000116016")).thenReturn(List.of());
        WarehouseConfiguration configuration = new WarehouseConfiguration();
        configuration.setNewLPNPrintLabelAtProducingFlag(true);
        when(layout.getWarehouseConfiguration(1L)).thenReturn(configuration);
        Warehouse warehouse = new Warehouse(); warehouse.setCompanyId(1L);
        when(layout.getWarehouseById(1L)).thenReturn(warehouse);
        RF rf = new RF(); rf.setPrinterName("TEST-PRINTER");
        when(resource.getRFByCode(1L, "RF001")).thenReturn(rf);
        ReportHistory report = new ReportHistory(); report.setFileName("TEST.lbl");
        when(orders.generatePrePrintLPNLabel(order, "L0000116016", 5L,
                "TEST-LINE", "", "TEST-PRINTER")).thenReturn(report);
    }

    void receive() {
        assertSame(inventory, ReflectionTestUtils.invokeMethod(service,
                "receiveInventoryFromWorkOrder", order, produced, transaction, "RF001"));
        verify(inventoryClient).receiveInventoryFromWorkOrder(order, inventory);
    }

    @Test void preprintedDefectiveStillReceivesInventoryButNeverPrints() {
        transaction.setSuppressLabelPrinting(true);
        receive();
        verify(inventoryClient, never()).findInventoryByLPN(any(), any());
        verifyNoInteractions(layout, resource, orders);
    }

    @Test void ordinaryProduceStillPrintsConfiguredNewLpn() {
        transaction.setSuppressLabelPrinting(false);
        receive();
        verify(resource).printReport(1L, 1L, ReportType.PRODUCTION_LINE_ASSIGNMENT_LABEL,
                "TEST.lbl", "TEST-PRINTER", 2);
    }

    @Test void omittedFlagPreservesLegacyPrintBehavior() throws Exception {
        assertFalse(new ObjectMapper().readValue("{}", WorkOrderProduceTransaction.class)
                .getSuppressLabelPrinting());
        transaction.setSuppressLabelPrinting(null);
        receive();
        verify(resource).printReport(1L, 1L, ReportType.PRODUCTION_LINE_ASSIGNMENT_LABEL,
                "TEST.lbl", "TEST-PRINTER", 2);
    }

    @Test void existingLpnStillAvoidsPrintingInOrdinaryProduce() {
        when(inventoryClient.findInventoryByLPN(1L, "L0000116016"))
                .thenReturn(List.of(new Inventory()));
        receive();
        verifyNoInteractions(layout, resource, orders);
    }
}
