package com.garyzhangscm.cwms.workorder.model;

public class LoginCredential {
    Long companyId;
    String username;
    String password;
    public LoginCredential(Long companyId, String username, String password) {
        this.companyId = companyId;
        this.username = username;
        this.password = password;
    }

    public Long getCompanyId() {
        return companyId;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }
}
