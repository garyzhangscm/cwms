package com.garyzhangscm.cwms.common.exception;


import java.util.Map;

public class SupplierException extends GenericException {
    public SupplierException(Map<String, Object> data){
        super(ExceptionCode.TRAILER_EXCEPTION, data);
    }

    public static SupplierException raiseException(String message) {
        return new SupplierException(createDefaultData(message));
    }
}
