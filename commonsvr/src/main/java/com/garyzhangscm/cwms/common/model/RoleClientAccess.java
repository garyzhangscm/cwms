package com.garyzhangscm.cwms.common.model;

public class RoleClientAccess extends AuditibleEntity<String>  {

    private Long id;

    private Role role;

    private Long clientId;



    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public Long getClientId() {
        return clientId;
    }

    public void setClientId(Long clientId) {
        this.clientId = clientId;
    }


}
