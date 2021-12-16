package com.garyzhangscm.cwms.inventory.exception;


import java.util.Map;

public class InventoryLockException extends GenericException {
    public InventoryLockException(Map<String, Object> data){
        super(ExceptionCode.INVENTORY_LOCK_ERROR, data);
    }

    public static InventoryLockException raiseException(String message) {
        return new InventoryLockException(createDefaultData(message));
    }
}
