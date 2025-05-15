/**
 * Copyright 2019
 *
 * @author gzhang
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.garyzhangscm.cwms.outbound.model.hualei;

import com.garyzhangscm.cwms.outbound.model.*;
import com.fasterxml.jackson.annotation.JsonProperty;

import javax.persistence.*;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "hualei_configuration")
public class HualeiConfiguration extends AuditibleEntity<String> implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "hualei_configuration_id")
    @JsonProperty(value="id")
    private Long id;

    @Column(name = "warehouse_id")
    private Long warehouseId;


    @Column(name = "customer_id")
    private String customerId;

    @Column(name = "customer_userid")
    private String customerUserid;


    @Column(name = "create_order_protocol")
    private String createOrderProtocol;
    @Column(name = "create_order_host")
    private String createOrderHost;
    @Column(name = "create_order_port")
    private String createOrderPort;
    @Column(name = "create_order_endpoint")
    private String createOrderEndpoint;

    @Column(name = "print_label_protocol")
    private String printLabelProtocol;
    @Column(name = "print_label_host")
    private String printLabelHost;
    @Column(name = "print_label_port")
    private String printLabelPort;
    @Column(name = "print_label_endpoint")
    private String printLabelEndpoint;


    @Column(name = "get_package_status_protocol")
    private String getPackageStatusProtocol;
    @Column(name = "get_package_status_host")
    private String getPackageStatusHost;
    @Column(name = "get_package_status_port")
    private String getPackageStatusPort;
    @Column(name = "get_package_status_endpoint")
    private String getPackageStatusEndpoint;

    @Column(name = "get_tracking_number_protocol")
    private String getTrackingNumberProtocol;
    @Column(name = "get_tracking_number_host")
    private String getTrackingNumberHost;
    @Column(name = "get_tracking_number_port")
    private String getTrackingNumberPort;
    @Column(name = "get_tracking_number_endpoint")
    private String getTrackingNumberEndpoint;

    @Column(name = "default_cargo_type")
    private String defaultCargoType;
    @Column(name = "default_customs_clearance")
    private String defaultCustomsClearance;
    @Column(name = "default_customs_declaration")
    private String defaultCustomsDeclaration;
    @Column(name = "default_duty_type")
    private String defaultDutyType;
    @Column(name = "default_from")
    private String defaultFrom;
    @Column(name = "default_is_fba")
    private String defaultIsFba;
    @Column(name = "default_order_returnsign")
    private String defaultOrderReturnSign;

    // default invoice parameters
    @Column(name = "default_hs_code")
    private String defaultHsCode;
    @Column(name = "default_invoice_title")
    private String defaultInvoiceTitle;
    @Column(name = "default_sku")
    private String defaultSku;
    @Column(name = "default_sku_code")
    private String defaultSkuCode;


    @Column(name = "weight_unit")
    private String weightUnit;
    @Column(name = "length_unit")
    private String lengthUnit;

    @OneToMany(
            mappedBy = "hualeiConfiguration",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.EAGER
    )
    private List<HualeiShippingLabelFormatByProduct> hualeiShippingLabelFormatByProducts = new ArrayList<>();

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

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public String getCustomerUserid() {
        return customerUserid;
    }

    public void setCustomerUserid(String customerUserid) {
        this.customerUserid = customerUserid;
    }

    public String getDefaultCargoType() {
        return defaultCargoType;
    }

    public void setDefaultCargoType(String defaultCargoType) {
        this.defaultCargoType = defaultCargoType;
    }

    public String getDefaultCustomsClearance() {
        return defaultCustomsClearance;
    }

    public void setDefaultCustomsClearance(String defaultCustomsClearance) {
        this.defaultCustomsClearance = defaultCustomsClearance;
    }

    public String getDefaultHsCode() {
        return defaultHsCode;
    }

    public void setDefaultHsCode(String defaultHsCode) {
        this.defaultHsCode = defaultHsCode;
    }

    public String getDefaultCustomsDeclaration() {
        return defaultCustomsDeclaration;
    }

    public void setDefaultCustomsDeclaration(String defaultCustomsDeclaration) {
        this.defaultCustomsDeclaration = defaultCustomsDeclaration;
    }

    public String getGetPackageStatusProtocol() {
        return getPackageStatusProtocol;
    }

    public void setGetPackageStatusProtocol(String getPackageStatusProtocol) {
        this.getPackageStatusProtocol = getPackageStatusProtocol;
    }

    public String getGetPackageStatusHost() {
        return getPackageStatusHost;
    }

    public void setGetPackageStatusHost(String getPackageStatusHost) {
        this.getPackageStatusHost = getPackageStatusHost;
    }

    public String getGetPackageStatusPort() {
        return getPackageStatusPort;
    }

    public void setGetPackageStatusPort(String getPackageStatusPort) {
        this.getPackageStatusPort = getPackageStatusPort;
    }

    public String getGetPackageStatusEndpoint() {
        return getPackageStatusEndpoint;
    }

    public void setGetPackageStatusEndpoint(String getPackageStatusEndpoint) {
        this.getPackageStatusEndpoint = getPackageStatusEndpoint;
    }

    public String getDefaultDutyType() {
        return defaultDutyType;
    }

    public void setDefaultDutyType(String defaultDutyType) {
        this.defaultDutyType = defaultDutyType;
    }

    public String getDefaultFrom() {
        return defaultFrom;
    }

    public void setDefaultFrom(String defaultFrom) {
        this.defaultFrom = defaultFrom;
    }

    public String getDefaultIsFba() {
        return defaultIsFba;
    }

    public void setDefaultIsFba(String defaultIsFba) {
        this.defaultIsFba = defaultIsFba;
    }

    public String getDefaultOrderReturnSign() {
        return defaultOrderReturnSign;
    }

    public void setDefaultOrderReturnSign(String defaultOrderReturnSign) {
        this.defaultOrderReturnSign = defaultOrderReturnSign;
    }

    public String getCreateOrderHost() {
        return createOrderHost;
    }

    public void setCreateOrderHost(String createOrderHost) {
        this.createOrderHost = createOrderHost;
    }

    public String getCreateOrderPort() {
        return createOrderPort;
    }

    public void setCreateOrderPort(String createOrderPort) {
        this.createOrderPort = createOrderPort;
    }

    public String getCreateOrderProtocol() {
        return createOrderProtocol;
    }

    public void setCreateOrderProtocol(String createOrderProtocol) {
        this.createOrderProtocol = createOrderProtocol;
    }

    public String getPrintLabelProtocol() {
        return printLabelProtocol;
    }

    public void setPrintLabelProtocol(String printLabelProtocol) {
        this.printLabelProtocol = printLabelProtocol;
    }

    public String getPrintLabelHost() {
        return printLabelHost;
    }

    public void setPrintLabelHost(String printLabelHost) {
        this.printLabelHost = printLabelHost;
    }

    public String getPrintLabelPort() {
        return printLabelPort;
    }

    public void setPrintLabelPort(String printLabelPort) {
        this.printLabelPort = printLabelPort;
    }

    public String getCreateOrderEndpoint() {
        return createOrderEndpoint;
    }

    public void setCreateOrderEndpoint(String createOrderEndpoint) {
        this.createOrderEndpoint = createOrderEndpoint;
    }

    public List<HualeiShippingLabelFormatByProduct> getHualeiShippingLabelFormatByProducts() {
        return hualeiShippingLabelFormatByProducts;
    }

    public void setHualeiShippingLabelFormatByProducts(List<HualeiShippingLabelFormatByProduct> hualeiShippingLabelFormatByProducts) {
        this.hualeiShippingLabelFormatByProducts = hualeiShippingLabelFormatByProducts;
    }

    public String getPrintLabelEndpoint() {
        return printLabelEndpoint;
    }

    public void setPrintLabelEndpoint(String printLabelEndpoint) {
        this.printLabelEndpoint = printLabelEndpoint;
    }

    public String getDefaultInvoiceTitle() {
        return defaultInvoiceTitle;
    }

    public void setDefaultInvoiceTitle(String defaultInvoiceTitle) {
        this.defaultInvoiceTitle = defaultInvoiceTitle;
    }

    public String getDefaultSku() {
        return defaultSku;
    }

    public void setDefaultSku(String defaultSku) {
        this.defaultSku = defaultSku;
    }

    public String getDefaultSkuCode() {
        return defaultSkuCode;
    }

    public void setDefaultSkuCode(String defaultSkuCode) {
        this.defaultSkuCode = defaultSkuCode;
    }

    public String getWeightUnit() {
        return weightUnit;
    }

    public void setWeightUnit(String weightUnit) {
        this.weightUnit = weightUnit;
    }

    public String getLengthUnit() {
        return lengthUnit;
    }

    public void setLengthUnit(String lengthUnit) {
        this.lengthUnit = lengthUnit;
    }

    public String getGetTrackingNumberProtocol() {
        return getTrackingNumberProtocol;
    }

    public void setGetTrackingNumberProtocol(String getTrackingNumberProtocol) {
        this.getTrackingNumberProtocol = getTrackingNumberProtocol;
    }

    public String getGetTrackingNumberHost() {
        return getTrackingNumberHost;
    }

    public void setGetTrackingNumberHost(String getTrackingNumberHost) {
        this.getTrackingNumberHost = getTrackingNumberHost;
    }

    public String getGetTrackingNumberPort() {
        return getTrackingNumberPort;
    }

    public void setGetTrackingNumberPort(String getTrackingNumberPort) {
        this.getTrackingNumberPort = getTrackingNumberPort;
    }

    public String getGetTrackingNumberEndpoint() {
        return getTrackingNumberEndpoint;
    }

    public void setGetTrackingNumberEndpoint(String getTrackingNumberEndpoint) {
        this.getTrackingNumberEndpoint = getTrackingNumberEndpoint;
    }
}
