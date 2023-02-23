package com.garyzhangscm.cwms.common.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Role extends AuditibleEntity<String>  {

    private Long id;

    private String name;

    private String description;

    private Boolean enabled;

    private List<RoleClientAccess> clientAccesses = new ArrayList<>();

    private Boolean nonClientDataAccessible;
    private Boolean allClientAccess;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Role role = (Role) o;
        return Objects.equals(id, role.id);
    }


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

    public Boolean getAllClientAccess() {
        return allClientAccess;
    }

    public void setAllClientAccess(Boolean allClientAccess) {
        this.allClientAccess = allClientAccess;
    }
}
