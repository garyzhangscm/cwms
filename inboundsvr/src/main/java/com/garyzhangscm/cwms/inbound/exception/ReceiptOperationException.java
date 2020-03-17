package com.garyzhangscm.cwms.inbound.exception;


import java.util.Map;

public class ReceiptOperationException extends GenericException {
    public ReceiptOperationException(Map<String, Object> data){
        super(ExceptionCode.RECEIPT_OPERATION_EXCEPTION, data);
    }

    public static ReceiptOperationException raiseException(String message) {
        return new ReceiptOperationException(createDefaultData(message));
    }
}
