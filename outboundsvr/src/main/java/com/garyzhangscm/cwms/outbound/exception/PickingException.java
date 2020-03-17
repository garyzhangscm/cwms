package com.garyzhangscm.cwms.outbound.exception;


import java.util.Map;

public class PickingException extends GenericException {
    public PickingException(Map<String, Object> data){
        super(ExceptionCode.PICKING_EXCEPTION, data);
    }

    public static PickingException raiseException(String message) {
        return new PickingException(createDefaultData(message));
    }
}
