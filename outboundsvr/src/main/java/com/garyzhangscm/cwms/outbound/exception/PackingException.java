package com.garyzhangscm.cwms.outbound.exception;


import java.util.Map;

public class PackingException extends GenericException {
    public PackingException(Map<String, Object> data){
        super(ExceptionCode.PACKING_EXCEPTION, data);
    }

    public static PackingException raiseException(String message) {
        return new PackingException(createDefaultData(message));
    }
}
