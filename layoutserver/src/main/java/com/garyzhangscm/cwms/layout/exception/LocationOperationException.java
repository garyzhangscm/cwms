package com.garyzhangscm.cwms.layout.exception;


import java.util.Map;

public class LocationOperationException extends GenericException {
    public LocationOperationException(Map<String, Object> data){
        super(ExceptionCode.LOCATION_OPERATION_EXCEPTION, data);
    }

    public static LocationOperationException raiseException(String message) {
        return new LocationOperationException(createDefaultData(message));
    }
}
