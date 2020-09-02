package com.garyzhangscm.cwms.outbound.exception;


import java.util.Map;

public class AllocationException extends GenericException {
    public AllocationException(Map<String, Object> data){
        super(ExceptionCode.ALLOCATION_EXCEPTION, data);
    }

    public static AllocationException raiseException(String message) {
        return new AllocationException(createDefaultData(message));
    }
}
