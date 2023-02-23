package com.garyzhangscm.cwms.adminserver.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.garyzhangscm.cwms.adminserver.model.wms.Role;

import javax.persistence.Column;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
@JsonIgnoreProperties(ignoreUnknown = true)
public class User {
    private Long companyId;

    private String username;
    private String firstname;
    private String lastname;
    private Boolean isSystemAdmin = false;

    private Boolean isAdmin = false;
    private Boolean changePasswordAtNextLogon = false;
    private String password;
    private boolean enabled;
    private boolean locked;
    private List<Role> roles = new ArrayList<>();

    private String token;
    private String refreshToken;
    private String name;
    private String email;
    private Long id;
    private Timestamp time;
    private int refreshIn;


    private Long lastLoginCompanyId;
    private Long lastLoginWarehouseId;
    private String lastLoginToken;

    public User(){}

    public User(Long companyId,
                String username,
                String firstname,
                String lastname,
                Boolean isAdmin,
                String password,
                String name,
                String email) {

        this.companyId = companyId;
        this.username = username;
        this.firstname = firstname;
        this.lastname = lastname;
        this.isSystemAdmin = false;
        this.isAdmin = isAdmin;
        this.changePasswordAtNextLogon = false;
        this.password = password;
        this.enabled = true;
        this.locked = false;
        this.roles = new ArrayList<>();

        this.token = "";
        this.refreshToken = "";
        this.name = name;
        this.email = email;

    }

    @Override
    public String toString() {
        try {
            return new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(this);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return null;
    }


    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Timestamp getTime() {
        return time;
    }

    public void setTime(Timestamp time) {
        this.time = time;
    }
    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public int getRefreshIn() {
        return refreshIn;
    }

    public void setRefreshIn(int refreshIn) {
        this.refreshIn = refreshIn;
    }

    public Long getCompanyId() {
        return companyId;
    }

    public void setCompanyId(Long companyId) {
        this.companyId = companyId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getFirstname() {
        return firstname;
    }

    public void setFirstname(String firstname) {
        this.firstname = firstname;
    }

    public String getLastname() {
        return lastname;
    }

    public Long getLastLoginCompanyId() {
        return lastLoginCompanyId;
    }

    public void setLastLoginCompanyId(Long lastLoginCompanyId) {
        this.lastLoginCompanyId = lastLoginCompanyId;
    }

    public Long getLastLoginWarehouseId() {
        return lastLoginWarehouseId;
    }

    public void setLastLoginWarehouseId(Long lastLoginWarehouseId) {
        this.lastLoginWarehouseId = lastLoginWarehouseId;
    }

    public String getLastLoginToken() {
        return lastLoginToken;
    }

    public void setLastLoginToken(String lastLoginToken) {
        this.lastLoginToken = lastLoginToken;
    }

    public void setLastname(String lastname) {
        this.lastname = lastname;
    }

    public Boolean getSystemAdmin() {
        return isSystemAdmin;
    }

    public void setSystemAdmin(Boolean systemAdmin) {
        isSystemAdmin = systemAdmin;
    }

    public Boolean getAdmin() {
        return isAdmin;
    }

    public void setAdmin(Boolean admin) {
        isAdmin = admin;
    }

    public Boolean getChangePasswordAtNextLogon() {
        return changePasswordAtNextLogon;
    }

    public void setChangePasswordAtNextLogon(Boolean changePasswordAtNextLogon) {
        this.changePasswordAtNextLogon = changePasswordAtNextLogon;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isLocked() {
        return locked;
    }

    public void setLocked(boolean locked) {
        this.locked = locked;
    }

    public List<Role> getRoles() {
        return roles;
    }

    public void setRoles(List<Role> roles) {
        this.roles = roles;
    }
}
