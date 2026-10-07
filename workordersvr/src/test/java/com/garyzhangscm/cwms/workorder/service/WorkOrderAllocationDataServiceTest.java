package com.garyzhangscm.cwms.workorder.service;

import com.garyzhangscm.cwms.workorder.clients.*;
import com.garyzhangscm.cwms.workorder.exception.WorkOrderException;
import com.garyzhangscm.cwms.workorder.model.*;
import org.junit.jupiter.api.*;
import org.springframework.http.HttpMethod;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.List;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class WorkOrderAllocationDataServiceTest {
    InventoryServiceRestemplateClient inventory;
    WarehouseLayoutServiceRestemplateClient layout;
    RestTemplateProxy outbound;
    OutboundServiceRestemplateClient client;
    WorkOrder order;
    WorkOrderLine line;
    @BeforeEach void setup() {
        inventory=mock(InventoryServiceRestemplateClient.class);layout=mock(WarehouseLayoutServiceRestemplateClient.class);
        outbound=mock(RestTemplateProxy.class);client=new OutboundServiceRestemplateClient();
        ReflectionTestUtils.setField(client,"restTemplateProxy",outbound);
        ReflectionTestUtils.setField(client,"allocationDataService",new WorkOrderAllocationDataService(inventory,layout,new ObjectMapper().findAndRegisterModules()));
        order=new WorkOrder();order.setId(2586L);order.setNumber("WO0000000390");order.setWarehouseId(1L);
        order.setItemId(8392L);order.setStatus(WorkOrderStatus.INPROCESS);order.setExpectedQuantity(10000L);
        line=new WorkOrderLine();line.setId(11901L);line.setNumber("1");line.setItemId(6402L);
        line.setOpenQuantity(50000L);line.setExpectedQuantity(50000L);line.setInventoryStatusId(1L);
        order.setWorkOrderLines(List.of(line));
        Warehouse warehouse=new Warehouse();warehouse.setId(1L);when(layout.getWarehouseById(1L)).thenReturn(warehouse);
        when(inventory.getItemById(8392L)).thenReturn(item(8392L,"finished"));
        when(inventory.getItemById(6402L)).thenReturn(item(6402L,"material"));
        InventoryStatus status=new InventoryStatus();status.setId(1L);when(inventory.getInventoryStatusById(1L)).thenReturn(status);
        when(outbound.exchange(eq(AllocationResult.class),anyString(),eq(HttpMethod.POST),any(Map.class))).thenAnswer(call->{
            assertNotNull(order.getItem());assertNotNull(order.getWarehouse());
            for(WorkOrderLine l:order.getWorkOrderLines())if(l.getOpenQuantity()>0)assertNotNull(l.getItem());
            var wire = new ObjectMapper().readTree(new ObjectMapper().writeValueAsBytes(call.getArgument(3)));
            assertEquals(1L,wire.path("warehouseId").asLong());
            assertEquals(1L,wire.path("warehouse").path("id").asLong());
            assertEquals(1L,wire.path("item").path("warehouseId").asLong());
            for(var l:wire.path("workOrderLines")) {
                assertEquals(1L,l.path("warehouseId").asLong());
                if(l.path("openQuantity").asLong()>0)assertEquals(1L,l.path("item").path("warehouseId").asLong());
            }
            return new AllocationResult();
        });
    }
    static Item item(Long id,String name){Item result=new Item();result.setId(id);result.setName(name);result.setWarehouseId(1L);return result;}
    @Test void bareDatabaseOrderIsHydratedBeforeWholeOrderPost(){
        client.allocateWorkOrder(order,null,null);
        assertEquals(6402L,line.getItem().getId());assertEquals(1L,line.getInventoryStatus().getId());
        assertEquals(50000L,line.getOpenQuantity());assertEquals(WorkOrderStatus.INPROCESS,order.getStatus());
        verify(outbound).exchange(eq(AllocationResult.class),eq("http://apigateway:5555/api/outbound/allocation/work-order"),eq(HttpMethod.POST),any(Map.class));
    }
    @Test void productionLineAndPartialQuantityArePreserved(){
        client.allocateWorkOrder(order,42L,350L);
        verify(outbound).exchange(eq(AllocationResult.class),eq("http://apigateway:5555/api/outbound/allocation/work-order?productionLineId=42&quantity=350"),eq(HttpMethod.POST),any(Map.class));
    }
    @Test void duplicateMaterialAndStatusAreLoadedOnce(){
        WorkOrderLine duplicate=new WorkOrderLine();duplicate.setItemId(6402L);duplicate.setOpenQuantity(10L);duplicate.setInventoryStatusId(1L);
        order.setWorkOrderLines(List.of(line,duplicate));client.allocateWorkOrder(order,null,null);
        verify(inventory,times(1)).getItemById(6402L);verify(inventory,times(1)).getInventoryStatusById(1L);
    }
    @Test void exhaustedLineDoesNotRequireOrFetchMaterial(){
        line.setOpenQuantity(0L);line.setItemId(null);client.allocateWorkOrder(order,null,null);
        verify(inventory,never()).getItemById(6402L);verify(inventory,never()).getInventoryStatusById(anyLong());
    }
    @Test void missingLaterMaterialPreventsAnyAllocationPost(){
        WorkOrderLine bad=new WorkOrderLine();bad.setId(11902L);bad.setNumber("2");bad.setOpenQuantity(10L);bad.setItemId(999L);
        order.setWorkOrderLines(List.of(line,bad));
        assertTrue(assertThrows(WorkOrderException.class,()->client.allocateWorkOrder(order,null,null)).getMessage().contains("material line 2"));
        verifyNoInteractions(outbound);
    }
    @Test void missingWarehouseAndFinishedItemPreventPost(){
        when(layout.getWarehouseById(1L)).thenReturn(null);assertThrows(WorkOrderException.class,()->client.allocateWorkOrder(order,null,null));
        Warehouse warehouse=new Warehouse();warehouse.setId(1L);when(layout.getWarehouseById(1L)).thenReturn(warehouse);
        when(inventory.getItemById(8392L)).thenReturn(null);assertThrows(WorkOrderException.class,()->client.allocateWorkOrder(order,null,null));
        verifyNoInteractions(outbound);
    }
    @Test void wrongItemIdAndMissingStatusPreventPost(){
        when(inventory.getItemById(6402L)).thenReturn(item(999L,"wrong"));assertThrows(WorkOrderException.class,()->client.allocateWorkOrder(order,null,null));
        when(inventory.getItemById(6402L)).thenReturn(item(6402L,"material"));when(inventory.getInventoryStatusById(1L)).thenReturn(null);
        assertThrows(WorkOrderException.class,()->client.allocateWorkOrder(order,null,null));verifyNoInteractions(outbound);
    }
    @Test void foreignWarehouseItemPreventsPost(){
        Item foreign=item(6402L,"foreign");foreign.setWarehouseId(2L);when(inventory.getItemById(6402L)).thenReturn(foreign);
        assertThrows(WorkOrderException.class,()->client.allocateWorkOrder(order,null,null));verifyNoInteractions(outbound);
    }
    @Test void normalOrderSerializationStillHidesWarehouseButAllocationIncludesIt() throws Exception {
        var loader=new WorkOrderAllocationDataService(inventory,layout,new ObjectMapper().findAndRegisterModules());loader.prepare(order);
        var mapper=new ObjectMapper().findAndRegisterModules();assertFalse(mapper.readTree(mapper.writeValueAsBytes(order)).has("warehouse"));
        var wire=mapper.readTree(mapper.writeValueAsBytes(loader.requestBody(order)));
        assertEquals(1L,wire.path("warehouse").path("id").asLong());assertEquals(1L,wire.path("workOrderLines").get(0).path("warehouseId").asLong());
    }
    @Test void lookupFailureDoesNotSendPost(){
        when(inventory.getItemById(6402L)).thenThrow(new IllegalStateException("inventory unavailable"));
        assertThrows(IllegalStateException.class,()->client.allocateWorkOrder(order,null,null));verifyNoInteractions(outbound);
    }
}
