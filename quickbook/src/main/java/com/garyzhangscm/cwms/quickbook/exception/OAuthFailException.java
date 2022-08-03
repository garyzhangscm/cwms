package com.garyzhangscm.cwms.quickbook.exception;


import java.util.Map;

public class OAuthFailException extends GenericException {
    public OAuthFailException(Map<String, Object> data){
        super(ExceptionCode.OAUTH_FAIL, data);
    }

    public static OAuthFailException raiseException(String message) {
        return new OAuthFailException(createDefaultData(message));
    }
}
