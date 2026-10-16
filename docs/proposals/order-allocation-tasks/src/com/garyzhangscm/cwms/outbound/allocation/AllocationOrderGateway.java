package com.garyzhangscm.cwms.outbound.allocation;
public interface AllocationOrderGateway {
 // Called inside the task transaction; must lock the real order and verify its warehouse.
 String lockAndGetState(long warehouseId, long orderId);
 void validate(long warehouseId, long orderId);
 void markAllocating(long warehouseId, long orderId);
 void execute(long orderId);
 // Preserve COMPLETE/CANCELLED and avoid restoring OPEN over partially planned work.
 void finish(long warehouseId, long orderId, String originalState, boolean successful);
 boolean hasPlannedWork(long orderId);
}
