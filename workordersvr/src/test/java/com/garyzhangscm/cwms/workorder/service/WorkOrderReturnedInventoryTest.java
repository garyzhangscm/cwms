package com.garyzhangscm.cwms.workorder.service;
import com.garyzhangscm.cwms.workorder.clients.InventoryServiceRestemplateClient;
import com.garyzhangscm.cwms.workorder.model.*;
import org.junit.jupiter.api.*;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class WorkOrderReturnedInventoryTest {
 WorkOrderService service;InventoryServiceRestemplateClient inventory;WorkOrder order;
 @BeforeEach void setup(){service=spy(new WorkOrderService());inventory=mock(InventoryServiceRestemplateClient.class);ReflectionTestUtils.setField(service,"inventoryServiceRestemplateClient",inventory);order=new WorkOrder();order.setWarehouseId(1L);doReturn(order).when(service).findById(2310L);}
 @Test void noMaterialLinesNeverQueriesWarehouseInventory(){assertTrue(service.getReturnedInventory(2310L).isEmpty());verifyNoInteractions(inventory);}
 @Test void unsavedLinesNeverProduceUnrestrictedOrNullFilter(){order.getWorkOrderLines().add(new WorkOrderLine());assertTrue(service.getReturnedInventory(2310L).isEmpty());verifyNoInteractions(inventory);}
 @Test void validLinesPreserveWarehouseAndExactFilter(){for(Long id:List.of(11901L,11902L)){var line=new WorkOrderLine();line.setId(id);order.getWorkOrderLines().add(line);}order.getWorkOrderLines().add(new WorkOrderLine());var expected=List.of(new Inventory());when(inventory.getReturnedInventory(1L,"11901,11902")).thenReturn(expected);assertSame(expected,service.getReturnedInventory(2310L));verify(inventory).getReturnedInventory(1L,"11901,11902");}
 @Test void clientRejectsNullFilterWithoutAnyDependencies(){assertTrue(new InventoryServiceRestemplateClient().getReturnedInventory(1L,null).isEmpty());}
 @Test void clientRejectsEmptyFilterWithoutAnyDependencies(){assertTrue(new InventoryServiceRestemplateClient().getReturnedInventory(1L,"").isEmpty());}
 @Test void clientRejectsWhitespaceFilterWithoutAnyDependencies(){assertTrue(new InventoryServiceRestemplateClient().getReturnedInventory(1L,"  ").isEmpty());}
}
