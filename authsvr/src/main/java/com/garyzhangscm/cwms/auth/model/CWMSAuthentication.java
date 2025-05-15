package com.garyzhangscm.cwms.auth.model;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;

public class CWMSAuthentication implements Authentication {

    private Long companyId;
    private String username;
    private String password;

    private boolean isAuthenticated;


    public CWMSAuthentication(Long companyId, String username, String password) {
        this.companyId = companyId;
        this.username = username;
        this.password = password;
        this.isAuthenticated = false;

    }
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return null;
    }

    @Override
    public Object getCredentials() {
        return password;
    }

    @Override
    public Object getDetails() {
        return companyId;
    }

    @Override
    public Object getPrincipal() {
        return username;
    }

    @Override
    public boolean isAuthenticated() {
        return isAuthenticated;
    }

    @Override
    public void setAuthenticated(boolean isAuthenticated) throws IllegalArgumentException {
        this.isAuthenticated = isAuthenticated;
    }

    @Override
    public String getName() {
        return username;
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
