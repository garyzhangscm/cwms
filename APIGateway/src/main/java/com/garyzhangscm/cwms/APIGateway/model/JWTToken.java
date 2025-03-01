package com.garyzhangscm.cwms.APIGateway.model;

public class JWTToken {

    private String token;

    private Long companyId;

    private String username;

    private boolean isExpired;

    private boolean isValid;

    public JWTToken(String token, Long companyId, String username, boolean isExpired, boolean isValid) {
        this.token = token;
        this.companyId = companyId;
        this.username = username;
        this.isExpired = isExpired;
        this.isValid = isValid;
    }

    public static JWTToken EMPTY_TOKEN() {
        return new JWTToken("EMPTY", -1l, "EMPTY", false, true);
    }


    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public Long getCompanyId() {
        return companyId;
    }

    public void setCompanyId(Long companyId) {
        this.companyId = companyId;
    }

    public boolean isExpired() {
        return isExpired;
    }

    public void setExpired(boolean expired) {
        isExpired = expired;
    }

    public boolean isValid() {
        return isValid;
    }

    public void setValid(boolean valid) {
        isValid = valid;
    }
}
