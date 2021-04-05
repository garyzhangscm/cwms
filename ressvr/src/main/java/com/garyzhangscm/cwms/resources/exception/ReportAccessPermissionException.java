package com.garyzhangscm.cwms.resources.exception;


import java.util.Map;

public class ReportAccessPermissionException extends GenericException {
    public ReportAccessPermissionException(Map<String, Object> data){
        super(ExceptionCode.REPORT_ACCESS_PERMISSION_EXCEPTION, data);
    }

    public static ReportAccessPermissionException raiseException(String message) {
        return new ReportAccessPermissionException(createDefaultData(message));
    }
}
