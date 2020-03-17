package com.garyzhangscm.cwms.inbound.exception;

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
// =======    inbound service   ======
// =======      25000 ~ 29999  =========
//
public enum ExceptionCode {
    RESOURCE_NOT_FOUND(25000, HttpStatus.NOT_FOUND, "Can't Find the Resource"),
    REQUEST_VALIDATION_FAILED(25001, HttpStatus.BAD_REQUEST, "Validation Fail"),
    MISSING_INFORMATION(25002, HttpStatus.BAD_REQUEST, "Can't proceed due to missing information"),
    RECEIPT_OPERATION_EXCEPTION(25003, HttpStatus.BAD_REQUEST, "Can't process the receipt"),
    PUTAWAY_EXCEPTION(25004, HttpStatus.BAD_REQUEST, "Putaway error");

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
