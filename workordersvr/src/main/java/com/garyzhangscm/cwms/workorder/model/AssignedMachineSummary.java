package com.garyzhangscm.cwms.workorder.model;

/** Scalar projection: never serialize the full work-order entity graph. */
public record AssignedMachineSummary(
        Long productionLineId, String productionLineName,
        Long workOrderId, String workOrderNumber, Long itemId,
        Long warehouseId, WorkOrderStatus status,
        Long expectedQuantity, Long producedQuantity) {
}
