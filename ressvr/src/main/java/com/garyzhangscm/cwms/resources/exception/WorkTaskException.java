package com.garyzhangscm.cwms.resources.exception;


import java.util.Map;

public class WorkTaskException extends GenericException {
    public WorkTaskException(Map<String, Object> data){
        super(ExceptionCode.WORK_TASK_EXCEPTION, data);
    }

    public static WorkTaskException raiseException(String message) {
        return new WorkTaskException(GenericException.createDefaultData(message));
    }
}
