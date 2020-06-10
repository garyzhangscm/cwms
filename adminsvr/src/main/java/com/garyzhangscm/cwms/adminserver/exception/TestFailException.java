package com.garyzhangscm.cwms.adminserver.exception;


import java.util.Map;

public class TestFailException extends GenericException {
    public TestFailException(Map<String, Object> data){
        super(ExceptionCode.TEST_FAIL, data);
    }

    public static TestFailException raiseException(String message) {
        return new TestFailException(createDefaultData(message));
    }
}
