package com.garyzhangscm.cwms.outbound.exception;


import java.util.Map;

public class ShippingException extends GenericException {
    public ShippingException(Map<String, Object> data){
        super(ExceptionCode.SHIPPING_EXCEPTION, data);
    }

    public static ShippingException raiseException(String message) {
        return new ShippingException(createDefaultData(message));
    }
}
