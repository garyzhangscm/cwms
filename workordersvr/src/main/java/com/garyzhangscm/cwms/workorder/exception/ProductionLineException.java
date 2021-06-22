package com.garyzhangscm.cwms.workorder.exception;


import java.util.Map;

public class ProductionLineException extends GenericException {
    public ProductionLineException(Map<String, Object> data){
        super(ExceptionCode.PRODUCTION_LINE_EXCEPTION, data);
    }

    public static ProductionLineException raiseException(String message) {
        return new ProductionLineException(createDefaultData(message));
    }
}
