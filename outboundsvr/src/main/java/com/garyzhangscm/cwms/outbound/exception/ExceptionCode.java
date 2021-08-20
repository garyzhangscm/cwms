package com.garyzhangscm.cwms.outbound.exception;

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
// =======    outbound service   ======
// =======      45000 ~ 49999  =========
//
public enum ExceptionCode {
    SYSTEM_FATAL_ERROR(1, HttpStatus.REQUEST_TIMEOUT, "System Fatal Error"),
    RESOURCE_NOT_FOUND(45000, HttpStatus.NOT_FOUND, "Can't Find the Resource"),
    REQUEST_VALIDATION_FAILED(45001, HttpStatus.BAD_REQUEST, "Validation Fail"),
    MISSING_INFORMATION(45002, HttpStatus.BAD_REQUEST, "Can't proceed due to missing information"),
    PICKING_EXCEPTION(45003, HttpStatus.BAD_REQUEST, "Picking Exception"),
    SHIPPING_EXCEPTION(45004, HttpStatus.BAD_REQUEST, "Shipping Exception"),
    REPLENISHMENT_EXCEPTION(45005, HttpStatus.BAD_REQUEST, "Replenishment Exception"),
    ORDER_OPERATION_EXCEPTION(45006, HttpStatus.BAD_REQUEST, "Order Operation Exception"),
    SHORT_ALLOCATION_EXCEPTION(45007, HttpStatus.BAD_REQUEST, "Short Allocation Operation Exception"),
    GRID_EXCEPTION(45008, HttpStatus.BAD_REQUEST, "Grid Exception"),
    PACKING_EXCEPTION(45009, HttpStatus.BAD_REQUEST, "Packing Exception"),
    ALLOCATION_EXCEPTION(45010, HttpStatus.BAD_REQUEST, "Allocation Exception");

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
