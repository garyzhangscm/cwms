package com.garyzhangscm.cwms.inventory.exception;


import java.util.Map;

public class InventoryException extends GenericException {
    public InventoryException(Map<String, Object> data){
        super(ExceptionCode.INVENTORY_ERROR, data);
    }

    public static InventoryException raiseException(String message) {
        return new InventoryException(createDefaultData(message));
    }
}
