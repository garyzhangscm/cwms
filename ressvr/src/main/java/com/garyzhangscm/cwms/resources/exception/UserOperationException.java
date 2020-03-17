package com.garyzhangscm.cwms.resources.exception;


import java.util.Map;

public class UserOperationException extends GenericException {
    public UserOperationException(Map<String, Object> data){
        super(ExceptionCode.USER_OPERATION_EXCEPTION, data);
    }

    public static UserOperationException raiseException(String message) {
        return new UserOperationException(createDefaultData(message));
    }
}
