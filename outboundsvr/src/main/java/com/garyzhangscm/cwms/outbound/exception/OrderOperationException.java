package com.garyzhangscm.cwms.outbound.exception;


import java.util.Map;

public class OrderOperationException extends GenericException {
    public OrderOperationException(Map<String, Object> data){
        super(ExceptionCode.ORDER_OPERATION_EXCEPTION, data);
    }

    public static OrderOperationException raiseException(String message) {
        return new OrderOperationException(createDefaultData(message));
    }
}
