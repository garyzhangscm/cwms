package com.garyzhangscm.cwms.auth.exception;


import java.util.Map;

public class LoginException extends GenericException {
    public LoginException(Map<String, Object> data){
        super(ExceptionCode.LOGIN_ERROR, data);
    }

    public static LoginException raiseException(String message) {
        return new LoginException(createDefaultData(message));
    }



}
