package com.garyzhangscm.cwms.inventory.exception;


import java.util.Map;

public class SystemFatalException extends GenericException {
    public SystemFatalException(Map<String, Object> data){
        super(ExceptionCode.SYSTEM_FATAL_ERROR, data);
    }

    public static SystemFatalException raiseException(String message) {
        return new SystemFatalException(createDefaultData(message));
    }
}
