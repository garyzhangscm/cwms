package com.garyzhangscm.cwms.inbound.usercontext;

import org.springframework.stereotype.Component;

@Component
public class UserContext {
    public static final String CORRELATION_ID = "gzcwms-correlation-id";
    public static final String AUTH_TOKEN = "Authorization";
    public static final String USER_ID = "gzcwms-user-id";
    public static final String ORG_ID = "gzcwms-org-id";
    public static final String RF_CODE = "rfCode";

    private String correlationId = new String();
    private String authToken = new String();
    private String userId = new String();
    private String orgId = new String();
    private String rfCode = new String();

    public String getCorrelationId() {
        return correlationId;
    }

    public void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
    }

    public String getAuthToken() {
        return authToken;
    }

    public void setAuthToken(String authToken) {
        this.authToken = authToken;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getOrgId() {
        return orgId;
    }

    public void setOrgId(String orgId) {
        this.orgId = orgId;
    }

    public String getRfCode() {
        return rfCode;
    }

    public void setRfCode(String rfCode) {
        this.rfCode = rfCode;
    }
}
