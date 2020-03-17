package com.garyzhangscm.cwms.inventory.exception;


import java.util.Map;

public class InventoryConsolidationException extends GenericException {
    public InventoryConsolidationException(Map<String, Object> data){
        super(ExceptionCode.INVENTORY_CONSOLIDATION_EXCEPTION, data);
    }

    public static InventoryConsolidationException raiseException(String message) {
        return new InventoryConsolidationException(createDefaultData(message));
    }
}
