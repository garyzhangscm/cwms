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

package com.garyzhangscm.cwms.quickbook.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


public class WMSOrderWrapper implements Serializable {


    private String number;


    private Long shipToCustomerId;
    private String shipToCustomerName;


    private Long warehouseId;
    private String warehouseName;


    private String category;

    private Long billToCustomerId;
    private String billToCustomerName;


    private String shipToContactorFirstname;

    private String shipToContactorLastname;


    private String shipToAddressCountry;

    private String shipToAddressState;

    private String shipToAddressCounty;

    private String shipToAddressCity;

    private String shipToAddressDistrict;

    private String shipToAddressLine1;

    private String shipToAddressLine2;

    private String shipToAddressPostcode;


    private String billToContactorFirstname;
    private String billToContactorLastname;

    private String billToAddressCountry;
    private String billToAddressState;
    private String billToAddressCounty;
    private String billToAddressCity;
    private String billToAddressDistrict;
    private String billToAddressLine1;
    private String billToAddressLine2;
    private String billToAddressPostcode;


    private List<WMSOrderLineWrapper> orderLines = new ArrayList<>();

    public WMSOrderWrapper() {}

    public WMSOrderWrapper(Invoice invoice) {
        setNumber(invoice.getDocNumber());
        setWarehouseId(invoice.getWarehouseId());
        setCategory(OrderCategory.SALES_ORDER.toString());

        setShipToCustomerName(invoice.getCustomerRef().getName());

        setBillToCustomerName(invoice.getCustomerRef().getName());

        Address shipToAddress = invoice.getShipAddr();

        if (Objects.nonNull(shipToAddress)) {
            setShipToAddressCountry(shipToAddress.getCountry());
            setShipToAddressState(shipToAddress.getCountrySubDivisionCode());
            setShipToAddressCounty(shipToAddress.getCounty());
            setShipToAddressCity(shipToAddress.getCity());
            setShipToAddressDistrict(shipToAddress.getCity());
            setShipToAddressLine1(shipToAddress.getLine1());
            setShipToAddressLine2(shipToAddress.getLine2());
            setShipToAddressPostcode(shipToAddress.getPostalCode());
        }

        Address billToAddress = invoice.getBillAddr();
        if (Objects.nonNull(billToAddress)) {
            setBillToAddressCountry(shipToAddress.getCountry());
            setBillToAddressState(shipToAddress.getCountrySubDivisionCode());
            setBillToAddressCounty(shipToAddress.getCounty());
            setBillToAddressCity(shipToAddress.getCity());
            setBillToAddressDistrict(shipToAddress.getCity());
            setBillToAddressLine1(shipToAddress.getLine1());
            setBillToAddressLine2(shipToAddress.getLine2());
            setBillToAddressPostcode(shipToAddress.getPostalCode());
        }
        orderLines = new ArrayList<>();
        invoice.getLine().stream().filter(
                invoiceLine -> Objects.nonNull(invoiceLine.getLineNum()) && Objects.nonNull(invoiceLine.getSalesItemLineDetail())
        ).forEach(
                invoiceLine ->  addOrderLine(
                        new WMSOrderLineWrapper(
                                invoiceLine, invoice.getCompanyId(),
                                invoice.getWarehouseId()))
        );
    }

    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    public Long getShipToCustomerId() {
        return shipToCustomerId;
    }

    public void setShipToCustomerId(Long shipToCustomerId) {
        this.shipToCustomerId = shipToCustomerId;
    }

    public String getShipToCustomerName() {
        return shipToCustomerName;
    }

    public void setShipToCustomerName(String shipToCustomerName) {
        this.shipToCustomerName = shipToCustomerName;
    }
    public Long getWarehouseId() {
        return warehouseId;
    }

    public void setWarehouseId(Long warehouseId) {
        this.warehouseId = warehouseId;
    }

    public String getWarehouseName() {
        return warehouseName;
    }

    public void setWarehouseName(String warehouseName) {
        this.warehouseName = warehouseName;
    }

    public Long getBillToCustomerId() {
        return billToCustomerId;
    }

    public void setBillToCustomerId(Long billToCustomerId) {
        this.billToCustomerId = billToCustomerId;
    }

    public String getBillToCustomerName() {
        return billToCustomerName;
    }

    public void setBillToCustomerName(String billToCustomerName) {
        this.billToCustomerName = billToCustomerName;
    }

    public String getShipToContactorFirstname() {
        return shipToContactorFirstname;
    }

    public void setShipToContactorFirstname(String shipToContactorFirstname) {
        this.shipToContactorFirstname = shipToContactorFirstname;
    }

    public String getShipToContactorLastname() {
        return shipToContactorLastname;
    }

    public void setShipToContactorLastname(String shipToContactorLastname) {
        this.shipToContactorLastname = shipToContactorLastname;
    }

    public String getShipToAddressCountry() {
        return shipToAddressCountry;
    }

    public void setShipToAddressCountry(String shipToAddressCountry) {
        this.shipToAddressCountry = shipToAddressCountry;
    }

    public String getShipToAddressState() {
        return shipToAddressState;
    }

    public void setShipToAddressState(String shipToAddressState) {
        this.shipToAddressState = shipToAddressState;
    }

    public String getShipToAddressCounty() {
        return shipToAddressCounty;
    }

    public void setShipToAddressCounty(String shipToAddressCounty) {
        this.shipToAddressCounty = shipToAddressCounty;
    }

    public String getShipToAddressCity() {
        return shipToAddressCity;
    }

    public void setShipToAddressCity(String shipToAddressCity) {
        this.shipToAddressCity = shipToAddressCity;
    }

    public String getShipToAddressDistrict() {
        return shipToAddressDistrict;
    }

    public void setShipToAddressDistrict(String shipToAddressDistrict) {
        this.shipToAddressDistrict = shipToAddressDistrict;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getShipToAddressLine1() {
        return shipToAddressLine1;
    }

    public void setShipToAddressLine1(String shipToAddressLine1) {
        this.shipToAddressLine1 = shipToAddressLine1;
    }

    public String getShipToAddressLine2() {
        return shipToAddressLine2;
    }

    public void setShipToAddressLine2(String shipToAddressLine2) {
        this.shipToAddressLine2 = shipToAddressLine2;
    }

    public String getShipToAddressPostcode() {
        return shipToAddressPostcode;
    }

    public void setShipToAddressPostcode(String shipToAddressPostcode) {
        this.shipToAddressPostcode = shipToAddressPostcode;
    }

    public String getBillToContactorFirstname() {
        return billToContactorFirstname;
    }

    public void setBillToContactorFirstname(String billToContactorFirstname) {
        this.billToContactorFirstname = billToContactorFirstname;
    }

    public String getBillToContactorLastname() {
        return billToContactorLastname;
    }

    public void setBillToContactorLastname(String billToContactorLastname) {
        this.billToContactorLastname = billToContactorLastname;
    }

    public String getBillToAddressCountry() {
        return billToAddressCountry;
    }

    public void setBillToAddressCountry(String billToAddressCountry) {
        this.billToAddressCountry = billToAddressCountry;
    }

    public String getBillToAddressState() {
        return billToAddressState;
    }

    public void setBillToAddressState(String billToAddressState) {
        this.billToAddressState = billToAddressState;
    }

    public String getBillToAddressCounty() {
        return billToAddressCounty;
    }

    public void setBillToAddressCounty(String billToAddressCounty) {
        this.billToAddressCounty = billToAddressCounty;
    }

    public String getBillToAddressCity() {
        return billToAddressCity;
    }

    public void setBillToAddressCity(String billToAddressCity) {
        this.billToAddressCity = billToAddressCity;
    }

    public String getBillToAddressDistrict() {
        return billToAddressDistrict;
    }

    public void setBillToAddressDistrict(String billToAddressDistrict) {
        this.billToAddressDistrict = billToAddressDistrict;
    }

    public String getBillToAddressLine1() {
        return billToAddressLine1;
    }

    public void setBillToAddressLine1(String billToAddressLine1) {
        this.billToAddressLine1 = billToAddressLine1;
    }

    public String getBillToAddressLine2() {
        return billToAddressLine2;
    }

    public void setBillToAddressLine2(String billToAddressLine2) {
        this.billToAddressLine2 = billToAddressLine2;
    }

    public String getBillToAddressPostcode() {
        return billToAddressPostcode;
    }

    public void setBillToAddressPostcode(String billToAddressPostcode) {
        this.billToAddressPostcode = billToAddressPostcode;
    }

    public List<WMSOrderLineWrapper> getOrderLines() {
        return orderLines;
    }

    public void setOrderLines(List<WMSOrderLineWrapper> orderLines) {
        this.orderLines = orderLines;
    }

    public void addOrderLine(WMSOrderLineWrapper orderLine) {
        this.orderLines.add(orderLine);
    }
}
