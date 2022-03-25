package com.garyzhangscm.cwms.inbound.exception;


import java.util.Map;

public class CustomerReturnOrderException extends GenericException {
    public CustomerReturnOrderException(Map<String, Object> data){
        super(ExceptionCode.CUSTOMER_RETURN_ORDER, data);
    }

    public static CustomerReturnOrderException raiseException(String message) {
        return new CustomerReturnOrderException(createDefaultData(message));
    }
}
