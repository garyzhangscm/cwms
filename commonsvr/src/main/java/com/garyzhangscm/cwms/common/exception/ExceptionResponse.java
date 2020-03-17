package com.garyzhangscm.cwms.common.exception;

import org.springframework.util.ObjectUtils;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

public class ExceptionResponse {
    private int code;
    private int status;
    private String message;
    private String path;
    private Instant timestamp;
    private HashMap<String, Object> data = new HashMap<String, Object>();

    public ExceptionResponse() {
    }

    public ExceptionResponse(GenericException ex, String path) {
        this(ex.getExceptionCode().getCode(), ex.getExceptionCode().getStatus().value(), ex.getExceptionCode().getMessage(), path, ex.getData());
    }

    public ExceptionResponse(int code, int status, String message, String path, Map<String, Object> data) {
        this.code = code;
        this.status = status;
        this.message = message;
        this.path = path;
        this.timestamp = Instant.now();
        if (!ObjectUtils.isEmpty(data)) {
            this.data.putAll(data);
        }
    }

    @Override
    public String toString() {
        return"ExceptionResponse{" +
                "code=" + code +
                ", status=" + status +
                ", message='" + message + '\'' +
                ", path='" + path + '\'' +
                ", timestamp=" + timestamp +
                ", data=" + data +
                '}';
    }
}
