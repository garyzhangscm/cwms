package com.garyzhangscm.cwms.inventory.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.persistence.*;
import java.util.ArrayList;

import java.util.List;


public class Role {

    private Long id;

    private String name;

    private String description;

    private Boolean enabled;

    private List<User> users = new ArrayList<>();

    private List<RoleClientAccess> clientAccesses = new ArrayList<>();

    private Boolean nonClientDataAccessible;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public List<User> getUsers() {
        return users;
    }

    public void setUsers(List<User> users) {
        this.users = users;
    }

    public List<RoleClientAccess> getClientAccesses() {
        return clientAccesses;
    }

    public void setClientAccesses(List<RoleClientAccess> clientAccesses) {
        this.clientAccesses = clientAccesses;
    }

    public Boolean getNonClientDataAccessible() {
        return nonClientDataAccessible;
    }

    public void setNonClientDataAccessible(Boolean nonClientDataAccessible) {
        this.nonClientDataAccessible = nonClientDataAccessible;
    }
}
