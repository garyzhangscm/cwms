package com.garyzhangscm.cwms.resources.exception;


import java.util.Map;

public class ReportFileMissingException extends GenericException {
    public ReportFileMissingException(Map<String, Object> data){
        super(ExceptionCode.REPORT_FILE_MISSING_EXCEPTION, data);
    }

    public static ReportFileMissingException raiseException(String message) {
        return new ReportFileMissingException(createDefaultData(message));
    }
}
