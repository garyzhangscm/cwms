package com.garyzhangscm.cwms.inventory.exception;

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
// =======    inventory service   ======
// =======      35000 ~ 39999  =========
//
public enum ExceptionCode {
    RESOURCE_NOT_FOUND(35000, HttpStatus.NOT_FOUND, "Can't Find the Resource"),
    REQUEST_VALIDATION_FAILED(35001, HttpStatus.BAD_REQUEST, "Validation Fail"),
    CAN_NOT_ADD_INVENTORY_MISSING_INFORMATION(35002, HttpStatus.BAD_REQUEST, "Can't add inventory due to missing information"),
    MISSING_INFORMATION(35002, HttpStatus.BAD_REQUEST, "Can't proceed due to missing information"),
    ID_IN_PATH_DOES_NOT_MATCH(35003, HttpStatus.BAD_REQUEST, "ID in the URL doesn't match with the data passed in the request"),
    INVENTORY_CONSOLIDATION_EXCEPTION(35004, HttpStatus.BAD_REQUEST, "Inventory Consolidation Exception");

    private final int code;

    private final HttpStatus status;

    private final String message;

    ExceptionCode(int code, org.springframework.http.HttpStatus status, String message) {
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
