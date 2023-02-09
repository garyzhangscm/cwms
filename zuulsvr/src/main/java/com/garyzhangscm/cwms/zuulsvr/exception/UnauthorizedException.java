package com.garyzhangscm.cwms.zuulsvr.exception;


import java.util.Map;

public class UnauthorizedException extends GenericException {
    public UnauthorizedException(Map<String, Object> data){
        super(ExceptionCode.SYSTEM_FATAL_ERROR, data);
    }

    public static UnauthorizedException raiseException(String message) {
        return new UnauthorizedException(createDefaultData(message));
    }
}
