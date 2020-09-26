package com.garyzhangscm.cwms.inventory.exception;


import java.util.Map;

public class ItemException extends GenericException {
    public ItemException(Map<String, Object> data){
        super(ExceptionCode.ITEM_ERROR, data);
    }

    public static ItemException raiseException(String message) {
        return new ItemException(createDefaultData(message));
    }
}
