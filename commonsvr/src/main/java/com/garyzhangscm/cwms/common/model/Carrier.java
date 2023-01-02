package com.garyzhangscm.cwms.common.model;

import org.codehaus.jackson.annotate.JsonProperty;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "carrier")
public class Carrier extends AuditibleEntity<String> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "carrier_id")
    @JsonProperty(value="id")
    private Long id;

    @Column(name = "warehouse_id")
    private Long warehouseId;

    @Transient
    private Warehouse warehouse;

    @Column(name = "name")
    private String name;

    @Column(name = "description")
    private String description;

    @Column(name = "enabled")
    private Boolean enabled;


    @Column(name = "contactor_firstname")
    private String contactorFirstname;
    @Column(name = "contactor_lastname")
    private String contactorLastname;

    @Column(name = "address_country")
    private String addressCountry;
    @Column(name = "address_state")
    private String addressState;
    @Column(name = "address_county")
    private String addressCounty;
    @Column(name = "address_city")
    private String addressCity;
    @Column(name = "address_district")
    private String addressDistrict;
    @Column(name = "address_line1")
    private String addressLine1;
    @Column(name = "address_line2")
    private String addressLine2;
    @Column(name = "address_postcode")
    private String addressPostcode;


    @Column(name = "user_defined_field_1")
    private String userDefinedField1;
    @Column(name = "user_defined_field_2")
    private String userDefinedField2;
    @Column(name = "user_defined_field_3")
    private String userDefinedField3;
    @Column(name = "user_defined_field_4")
    private String userDefinedField4;
    @Column(name = "user_defined_field_5")
    private String userDefinedField5;

    @OneToMany(
            mappedBy = "carrier",
            orphanRemoval = true,
            cascade = CascadeType.ALL
    )
    private List<CarrierServiceLevel> carrierServiceLevels = new ArrayList<>();

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

    public Warehouse getWarehouse() {
        return warehouse;
    }

    public void setWarehouse(Warehouse warehouse) {
        this.warehouse = warehouse;
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

    public String getContactorFirstname() {
        return contactorFirstname;
    }

    public void setContactorFirstname(String contactorFirstname) {
        this.contactorFirstname = contactorFirstname;
    }

    public String getContactorLastname() {
        return contactorLastname;
    }

    public void setContactorLastname(String contactorLastname) {
        this.contactorLastname = contactorLastname;
    }

    public String getAddressCountry() {
        return addressCountry;
    }

    public void setAddressCountry(String addressCountry) {
        this.addressCountry = addressCountry;
    }

    public String getAddressState() {
        return addressState;
    }

    public void setAddressState(String addressState) {
        this.addressState = addressState;
    }

    public String getAddressCounty() {
        return addressCounty;
    }

    public void setAddressCounty(String addressCounty) {
        this.addressCounty = addressCounty;
    }

    public String getAddressCity() {
        return addressCity;
    }

    public void setAddressCity(String addressCity) {
        this.addressCity = addressCity;
    }

    public String getAddressDistrict() {
        return addressDistrict;
    }

    public void setAddressDistrict(String addressDistrict) {
        this.addressDistrict = addressDistrict;
    }

    public String getAddressLine1() {
        return addressLine1;
    }

    public void setAddressLine1(String addressLine1) {
        this.addressLine1 = addressLine1;
    }

    public String getAddressLine2() {
        return addressLine2;
    }

    public void setAddressLine2(String addressLine2) {
        this.addressLine2 = addressLine2;
    }

    public String getAddressPostcode() {
        return addressPostcode;
    }

    public void setAddressPostcode(String addressPostcode) {
        this.addressPostcode = addressPostcode;
    }

    public List<CarrierServiceLevel> getCarrierServiceLevels() {
        return carrierServiceLevels;
    }

    public void setCarrierServiceLevels(List<CarrierServiceLevel> carrierServiceLevels) {
        this.carrierServiceLevels = carrierServiceLevels;
    }

    public String getUserDefinedField1() {
        return userDefinedField1;
    }

    public void setUserDefinedField1(String userDefinedField1) {
        this.userDefinedField1 = userDefinedField1;
    }

    public String getUserDefinedField2() {
        return userDefinedField2;
    }

    public void setUserDefinedField2(String userDefinedField2) {
        this.userDefinedField2 = userDefinedField2;
    }

    public String getUserDefinedField3() {
        return userDefinedField3;
    }

    public void setUserDefinedField3(String userDefinedField3) {
        this.userDefinedField3 = userDefinedField3;
    }

    public String getUserDefinedField4() {
        return userDefinedField4;
    }

    public void setUserDefinedField4(String userDefinedField4) {
        this.userDefinedField4 = userDefinedField4;
    }

    public String getUserDefinedField5() {
        return userDefinedField5;
    }

    public void setUserDefinedField5(String userDefinedField5) {
        this.userDefinedField5 = userDefinedField5;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }
}
