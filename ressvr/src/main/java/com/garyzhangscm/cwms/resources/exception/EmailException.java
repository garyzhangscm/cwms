package com.garyzhangscm.cwms.resources.exception;


import java.util.Map;

public class EmailException extends GenericException {
    public EmailException(Map<String, Object> data){
        super(ExceptionCode.EMAIL_EXCEPTION, data);
    }

    public static EmailException raiseException(String message) {
        return new EmailException(createDefaultData(message));
    }
}
