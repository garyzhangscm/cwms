package com.garyzhangscm.cwms.common.exception;


import java.util.Map;

public class TrailerException extends GenericException {
    public TrailerException(Map<String, Object> data){
        super(ExceptionCode.TRAILER_EXCEPTION, data);
    }

    public static TrailerException raiseException(String message) {
        return new TrailerException(createDefaultData(message));
    }
}
