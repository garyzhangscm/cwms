package com.garyzhangscm.cwms.inbound.exception;


import java.util.Map;

public class PutawayException extends GenericException {
    public PutawayException(Map<String, Object> data){
        super(ExceptionCode.PUTAWAY_EXCEPTION, data);
    }

    public static PutawayException raiseException(String message) {
        return new PutawayException(createDefaultData(message));
    }
}
