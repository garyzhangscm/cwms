package com.garyzhangscm.cwms.quickbook.exception;


import java.util.Map;

public class ResourceNotFoundException extends GenericException {
    public ResourceNotFoundException(Map<String, Object> data){
        super(ExceptionCode.RESOURCE_NOT_FOUND, data);
    }

    public static ResourceNotFoundException raiseException(String message) {
        return new ResourceNotFoundException(createDefaultData(message));
    }
}
