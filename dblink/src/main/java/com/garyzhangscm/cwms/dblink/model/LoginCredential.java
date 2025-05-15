package com.garyzhangscm.cwms.dblink.model;

public class LoginCredential {
    String username;
    String password;


    Long companyId;

    public LoginCredential(Long companyId, String username, String password) {
        this.companyId = companyId;
        this.username = username;
        this.password = password;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public Long getCompanyId() {
        return companyId;
    }
}
