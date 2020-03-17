package com.garyzhangscm.cwms.workorder.exception;


import java.util.Map;

public class WorkOrderException extends GenericException {
    public WorkOrderException(Map<String, Object> data){
        super(ExceptionCode.WORK_ORDER_EXCEPTION, data);
    }

    public static WorkOrderException raiseException(String message) {
        return new WorkOrderException(createDefaultData(message));
    }
}
