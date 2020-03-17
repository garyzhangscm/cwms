package com.garyzhangscm.cwms.outbound.exception;


import java.util.Map;

public class MissingInformationException extends GenericException {
    public MissingInformationException(Map<String, Object> data){
        super(ExceptionCode.MISSING_INFORMATION, data);
    }

    public static MissingInformationException raiseException(String message) {
        return new MissingInformationException(createDefaultData(message));
    }


}
