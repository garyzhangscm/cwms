package com.garyzhangscm.cwms.outbound.exception;


import java.util.Map;

public class GridException extends GenericException {
    public GridException(Map<String, Object> data){
        super(ExceptionCode.GRID_EXCEPTION, data);
    }

    public static GridException raiseException(String message) {
        return new GridException(createDefaultData(message));
    }
}
