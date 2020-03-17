package com.garyzhangscm.cwms.outbound.exception;


import java.util.Map;

public class ReplenishmentException extends GenericException {
    public ReplenishmentException(Map<String, Object> data){
        super(ExceptionCode.REPLENISHMENT_EXCEPTION, data);
    }

    public static ReplenishmentException raiseException(String message) {
        return new ReplenishmentException(createDefaultData(message));
    }
}
