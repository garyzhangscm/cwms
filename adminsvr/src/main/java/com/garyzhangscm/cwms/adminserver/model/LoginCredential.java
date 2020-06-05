package com.garyzhangscm.cwms.adminserver.model;

public class LoginCredential {
    String username;
    String password;
    public LoginCredential(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }
}
