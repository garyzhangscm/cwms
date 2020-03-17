package com.garyzhangscm.cwms.outbound.exception;


import java.util.Map;

public class ShortAllocationException extends GenericException {
    public ShortAllocationException(Map<String, Object> data){
        super(ExceptionCode.SHORT_ALLOCATION_EXCEPTION, data);
    }

    public static ShortAllocationException raiseException(String message) {
        return new ShortAllocationException(createDefaultData(message));
    }
}
