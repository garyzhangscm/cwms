package com.garyzhangscm.cwms.workorder.service;

import com.garyzhangscm.cwms.workorder.model.*;
import com.garyzhangscm.cwms.workorder.exception.WorkOrderException;
import org.junit.jupiter.api.*;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class WorkOrderAllocationResultTest {
    WorkOrderService service;
    WorkOrderLineService lines;
    WorkOrder order;
    @BeforeEach void setup() {
        service = new WorkOrderService();
        lines = mock(WorkOrderLineService.class);
        ReflectionTestUtils.setField(service, "workOrderLineService", lines);
        order = new WorkOrder(); order.setId(2586L); order.setStatus(WorkOrderStatus.INPROCESS);
    }
    WorkOrderLine line(long id, long item, long open) {
        var line = new WorkOrderLine(); line.setId(id); line.setItemId(item);
        line.setWorkOrder(order); line.setNumber("0"); line.setOpenQuantity(open); line.setInprocessQuantity(10L);
        when(lines.findById(id, false)).thenReturn(line);
        return line;
    }
    Pick pick(long lineId, long item, long qty) {
        var pick = new Pick(); pick.setWorkOrderLineId(lineId); pick.setItemId(item); pick.setQuantity(qty); pick.setNumber("PICK-TEST"); return pick;
    }
    void spare(WorkOrderLine line, long item) {
        var group = new WorkOrderLineSparePart(); var detail = new WorkOrderLineSparePartDetail(); detail.setItemId(item);
        group.getWorkOrderLineSparePartDetails().add(detail); line.getWorkOrderLineSpareParts().add(group);
    }
    AllocationResult result(Pick... picks) { var r = new AllocationResult(); r.setPicks(Arrays.asList(picks)); return r; }
    void process(AllocationResult result) { ReflectionTestUtils.invokeMethod(service, "processAllocateResult", order, result); }
    void verifyLean(long id) {
        verify(lines, times(1)).findById(id, false);
        verify(lines, never()).findById(anyLong()); verify(lines, never()).save(any(WorkOrderLine.class));
    }
    @Test void fortySixPicksReadOneLineOnceAndPreserveQuantity() {
        var line = line(11901,6402,50000); var r = new AllocationResult();
        for(int i=0;i<45;i++) r.getPicks().add(pick(11901,6402,1000));
        r.getPicks().add(pick(11901,6402,5000)); process(r);
        assertEquals(0L,line.getOpenQuantity()); assertEquals(50010L,line.getInprocessQuantity());
        verifyLean(11901); verify(lines).save(same(line),eq(false));
    }
    @Test void multipleLinesAreReadAndSavedOnceEach() {
        var first=line(1,11,100); var second=line(2,22,100);
        process(result(pick(1,11,20),pick(2,22,30),pick(1,11,10)));
        assertEquals(70L,first.getOpenQuantity()); assertEquals(40L,first.getInprocessQuantity());
        assertEquals(70L,second.getOpenQuantity()); verifyLean(1);verifyLean(2);
        verify(lines).save(same(first),eq(false));verify(lines).save(same(second),eq(false));
    }
    @Test void sparePicksAreExcludedFromMainMaterialQuantity() {
        var line=line(1,11,100);spare(line,22);
        process(result(pick(1,22,25),pick(1,11,30),pick(1,22,10)));
        assertEquals(70L,line.getOpenQuantity());assertEquals(40L,line.getInprocessQuantity());verifyLean(1);
    }
    @Test void unknownItemStillBlocksBeforeAnyQuantitySave() {
        var line=line(1,11,100);spare(line,22);
        assertThrows(WorkOrderException.class,()->process(result(pick(1,11,20),pick(1,99,30))));
        assertEquals(100L,line.getOpenQuantity());assertEquals(10L,line.getInprocessQuantity());
        verify(lines,never()).save(any(WorkOrderLine.class),anyBoolean());verifyLean(1);
    }
    @Test void shortageAndPicksAreSummedTogether() {
        var line=line(1,11,100);var r=result(pick(1,11,20));
        var shortage=new ShortAllocation();shortage.setWorkOrderLineId(1L);shortage.setQuantity(30L);r.getShortAllocations().add(shortage);
        process(r);assertEquals(50L,line.getOpenQuantity());assertEquals(60L,line.getInprocessQuantity());verifyLean(1);
    }
    @Test void shortageOnlyLoadsLineOnce() {
        var line=line(1,11,100);var r=new AllocationResult();
        var shortage=new ShortAllocation();shortage.setWorkOrderLineId(1L);shortage.setQuantity(30L);r.getShortAllocations().add(shortage);
        process(r);assertEquals(70L,line.getOpenQuantity());verifyLean(1);
    }
    @Test void roundUpStillClampsOpenQuantityWithoutDroppingAllocatedQuantity() {
        var line=line(1,11,10);process(result(pick(1,11,15)));
        assertEquals(0L,line.getOpenQuantity());assertEquals(25L,line.getInprocessQuantity());verifyLean(1);
    }
    @Test void partialAllocationOnlyUpdatesRequestedLine() {
        var first=line(1,11,100);var second=line(2,22,100);
        ReflectionTestUtils.invokeMethod(service,"processAllocateResult",order,first,result(pick(1,11,20),pick(2,22,30)));
        assertEquals(80L,first.getOpenQuantity());assertEquals(100L,second.getOpenQuantity());
        verifyLean(1);verifyLean(2);verify(lines,never()).save(same(second),eq(false));
    }
    @Test void subsequentRequestReadsFreshEntityAgain() {
        var first=line(1,11,100);process(result(pick(1,11,20)));
        var fresh=line(1,11,60);process(result(pick(1,11,10)));
        assertEquals(80L,first.getOpenQuantity());assertEquals(50L,fresh.getOpenQuantity());
        verify(lines,times(2)).findById(1L,false);
    }
    @Test void spareSettlementReusesLineAndPreservesEachPickUpdate() {
        var line=line(1,11,100);spare(line,22);
        var detail=line.getWorkOrderLineSpareParts().get(0).getWorkOrderLineSparePartDetails().get(0);
        detail.setOpenQuantity(100L);detail.setInprocessQuantity(5L);
        var details=mock(WorkOrderLineSparePartDetailService.class);
        var groups=mock(WorkOrderLineSparePartService.class);
        ReflectionTestUtils.setField(service,"workOrderLineSparePartDetailService",details);
        ReflectionTestUtils.setField(service,"workOrderLineSparePartService",groups);
        ReflectionTestUtils.invokeMethod(service,"processAllocationResultForSpareParts",result(pick(1,11,20),pick(1,22,25),pick(1,22,10)));
        assertEquals(65L,detail.getOpenQuantity());assertEquals(40L,detail.getInprocessQuantity());
        verifyLean(1);verify(details,times(2)).saveOrUpdate(same(detail));
        verify(groups,times(2)).refreshInprocessQuantity(nullable(WorkOrderLineSparePart.class));
    }
    @Test void pendingOrderStillTransitionsToInprocess() {
        service=spy(service);order.setStatus(WorkOrderStatus.PENDING);
        doReturn(order).when(service).saveOrUpdate(same(order));
        var line=line(1,11,100);process(result(pick(1,11,20)));
        assertEquals(WorkOrderStatus.INPROCESS,order.getStatus());
        assertEquals(80L,line.getOpenQuantity());verify(service).saveOrUpdate(same(order));
    }

}
