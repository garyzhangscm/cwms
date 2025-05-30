package com.garyzhangscm.cwms.common.exception;


import java.util.Map;

public class RequestValidationFailException extends GenericException {
    public RequestValidationFailException(Map<String, Object> data){
        super(ExceptionCode.REQUEST_VALIDATION_FAILED, data);
    }



    public static RequestValidationFailException raiseException(String message) {
        return new RequestValidationFailException(createDefaultData(message));
    }
}
