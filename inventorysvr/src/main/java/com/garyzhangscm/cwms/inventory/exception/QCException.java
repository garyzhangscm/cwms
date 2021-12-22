package com.garyzhangscm.cwms.inventory.exception;


import java.util.Map;

public class QCException extends GenericException {
    public QCException(Map<String, Object> data){
        super(ExceptionCode.QC_ERROR, data);
    }

    public static QCException raiseException(String message) {
        return new QCException(createDefaultData(message));
    }
}
