package com.garyzhangscm.cwms.common.exception;


import java.util.Map;

public class SystemControlledNumberException extends GenericException {
    public SystemControlledNumberException(Map<String, Object> data){
        super(ExceptionCode.SYSTEM_CONTROLLED_NUMBER_EXCEPTION, data);
    }

    public static SystemControlledNumberException raiseException(String message) {
        return new SystemControlledNumberException(createDefaultData(message));
    }
}
