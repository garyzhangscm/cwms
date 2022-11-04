package com.garyzhangscm.cwms.adminserver.model;

import com.garyzhangscm.cwms.adminserver.model.wms.*;
import org.codehaus.jackson.annotate.JsonProperty;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * A container that has all the information we will
 * need to initiate a instance for one company
 * This works as a start point to use the system.
 * It will only includes the basic information
 * to start with and the user can create more
 * data after log in
 * The user will need to pass in
 * - one company
 * - one warehouse
 * - all necessary logic location and policy
 * - one storage area
 * - ten storage location
 * - one super user
 *
 */
@Entity
@Table(name = "data_initial_request")
public class DataInitialRequest extends AuditibleEntity<String>{


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "data_initial_request_id")
    @JsonProperty(value="id")
    private Long id;


    @Column(name = "company_name")
    private String companyName;

    @Column(name = "company_code")
    private String companyCode;

    @Column(name = "warehouse_name")
    private String warehouseName;

    @Column(name = "admin_username")
    private String adminUserName;


    @Column(name = "request_username")
    private String requestUsername;


    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private DataInitialRequestStatus status;

    // if the company marked as production
    // then we won't massive CRUD the
    // data that belongs to this company
    @Column(name = "production")
    private Boolean production;

    @Transient
    private User requestUser;

    public  DataInitialRequest(){}

    public DataInitialRequest(String companyName,
                              String warehouseName,
                              String adminUserName,
                              String requestUsername,
                              Boolean production) {
        this.companyName = companyName;
        this.warehouseName = warehouseName;
        this.adminUserName = adminUserName;
        this.requestUsername = requestUsername;
        this.status = DataInitialRequestStatus.PENDING;
        this.production = Objects.isNull(production) ?
                true : production;

    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public String getWarehouseName() {
        return warehouseName;
    }

    public void setWarehouseName(String warehouseName) {
        this.warehouseName = warehouseName;
    }

    public String getAdminUserName() {
        return adminUserName;
    }

    public void setAdminUserName(String adminUserName) {
        this.adminUserName = adminUserName;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getRequestUsername() {
        return requestUsername;
    }

    public void setRequestUsername(String requestUsername) {
        this.requestUsername = requestUsername;
    }

    public User getRequestUser() {
        return requestUser;
    }

    public void setRequestUser(User requestUser) {
        this.requestUser = requestUser;
    }

    public DataInitialRequestStatus getStatus() {
        return status;
    }

    public void setStatus(DataInitialRequestStatus status) {
        this.status = status;
    }

    public String getCompanyCode() {
        return companyCode;
    }

    public void setCompanyCode(String companyCode) {
        this.companyCode = companyCode;
    }

    public Boolean getProduction() {
        return production;
    }

    public void setProduction(Boolean production) {
        this.production = production;
    }
}
