package com.garyzhangscm.cwms.resources.exception;

import org.springframework.http.HttpStatus;

// Exception code for each service:
// auth service: 10000 ~ 14999
// common service: 15000 ~ 19999
// configure service: 20000 ~ 24999
// inbound service: 25000 ~ 29999
// integration service: 30000 ~ 34999
// inventory service: 35000 ~ 39999
// layout service: 40000 ~ 44999
// outbound service: 45000 ~ 49999
// resource service: 50000 ~ 54999
// work order service: 55000 ~ 59999
// zull service: 60000 ~ 65999
//
// =======    resource service   ======
// =======      50000 ~ 54999  =========
//
public enum ExceptionCode {
    SYSTEM_FATAL_ERROR(1, HttpStatus.REQUEST_TIMEOUT, "System Fatal Error"),
    RESOURCE_NOT_FOUND(50000, HttpStatus.NOT_FOUND, "Can't Find the Resource"),
    REQUEST_VALIDATION_FAILED(50001, HttpStatus.BAD_REQUEST, "Validation Fail"),
    MISSING_INFORMATION(50002, HttpStatus.BAD_REQUEST, "Can't proceed due to missing information"),
    USER_OPERATION_EXCEPTION(50003, HttpStatus.BAD_REQUEST, "User Operation Exception"),
    REPORT_ACCESS_PERMISSION_EXCEPTION(50004, HttpStatus.BAD_REQUEST, "User Doesn't Have Permission to Access current Report"),
    REPORT_FILE_MISSING_EXCEPTION(50005, HttpStatus.BAD_REQUEST, "Cannot find the report file"),
    EMAIL_EXCEPTION(50006, HttpStatus.BAD_REQUEST, "Email Exception");

    private final int code;

    private final HttpStatus status;

    private final String message;

    ExceptionCode(int code, HttpStatus status, String message) {
        this.code = code;
        this.status = status;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }

    @Override
    public String toString() {
        return"ErrorCode{" +
                "code=" + code +
                ", status=" + status +
                ", message='" + message + '\'' +
                '}';
    }
}
