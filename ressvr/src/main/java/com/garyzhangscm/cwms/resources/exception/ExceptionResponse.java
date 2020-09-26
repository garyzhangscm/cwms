package com.garyzhangscm.cwms.resources.exception;

import org.springframework.util.ObjectUtils;

import java.io.Serializable;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

public class ExceptionResponse implements Serializable {
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
    public ExceptionResponse(RuntimeException ex, String path) {
        this(-1, -1, ex.getMessage(), path, new HashMap<>());
    }

    public ExceptionResponse(Exception ex, String path) {
        this(-1, -1, ex.getMessage(), path, new HashMap<>());
    }
    public ExceptionResponse(int code, int status, String message, String path, Map<String, Object> data) {
        this.code = code;
        this.status = status;
        this.message = message;
        if (data.containsKey("error_message")) {
            this.message += "-" + data.get("error_message");
        }
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

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public HashMap<String, Object> getData() {
        return data;
    }

    public void setData(HashMap<String, Object> data) {
        this.data = data;
    }
}
