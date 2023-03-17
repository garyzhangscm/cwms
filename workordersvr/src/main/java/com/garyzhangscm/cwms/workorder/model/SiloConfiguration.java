package com.garyzhangscm.cwms.workorder.model;

import org.codehaus.jackson.annotate.JsonProperty;

import javax.persistence.*;

@Entity
@Table(name = "silo_configuration")
public class SiloConfiguration extends AuditibleEntity<String>{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "silo_configuration_id")
    @JsonProperty(value="id")
    private Long id;

    @Column(name = "warehouse_id")
    private Long warehouseId;


    @Column(name = "web_api_protocol")
    private String webAPIProtocol;
    @Column(name = "web_api_url")
    private String webAPIUrl;

    @Column(name = "web_api_username")
    private String webAPIUsername;
    @Column(name = "web_api_password")
    private String webAPIPassword;


    @Column(name = "enabled")
    private boolean enabled;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getWarehouseId() {
        return warehouseId;
    }

    public void setWarehouseId(Long warehouseId) {
        this.warehouseId = warehouseId;
    }

    public String getWebAPIProtocol() {
        return webAPIProtocol;
    }

    public void setWebAPIProtocol(String webAPIProtocol) {
        this.webAPIProtocol = webAPIProtocol;
    }

    public String getWebAPIUrl() {
        return webAPIUrl;
    }

    public void setWebAPIUrl(String webAPIUrl) {
        this.webAPIUrl = webAPIUrl;
    }

    public String getWebAPIUsername() {
        return webAPIUsername;
    }

    public void setWebAPIUsername(String webAPIUsername) {
        this.webAPIUsername = webAPIUsername;
    }

    public String getWebAPIPassword() {
        return webAPIPassword;
    }

    public void setWebAPIPassword(String webAPIPassword) {
        this.webAPIPassword = webAPIPassword;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
