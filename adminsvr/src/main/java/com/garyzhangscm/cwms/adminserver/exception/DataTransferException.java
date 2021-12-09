package com.garyzhangscm.cwms.adminserver.exception;


import java.util.Map;

public class DataTransferException extends GenericException {
    public DataTransferException(Map<String, Object> data){
        super(ExceptionCode.TEST_FAIL, data);
    }

    public static DataTransferException raiseException(String message) {
        return new DataTransferException(createDefaultData(message));
    }
}
