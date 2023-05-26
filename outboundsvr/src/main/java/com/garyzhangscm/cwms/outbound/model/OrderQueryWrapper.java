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

package com.garyzhangscm.cwms.outbound.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.garyzhangscm.cwms.outbound.service.ObjectCopyUtil;

import java.io.Serializable;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

public class OrderQueryWrapper implements Serializable {


    private String number;

    private Customer shipToCustomer;
    private Customer billToCustomer;

    private OrderCategory category;

    private String transferReceiptNumber;
    private Long transferReceiptWarehouseId;

    private OrderStatus status = OrderStatus.OPEN;


    private String shipToContactorFirstname;
    private String shipToContactorLastname;
    private String shipToContactorPhoneNumber;

    private String shipToAddressCountry;
    private String shipToAddressState;
    private String shipToAddressCounty;
    private String shipToAddressCity;
    private String shipToAddressLine1;
    private String shipToAddressLine2;
    private String shipToAddressLine3;
    private String shipToAddressPostcode;


    private String billToContactorFirstname;
    private String billToContactorLastname;

    private String billToAddressCountry;
    private String billToAddressState;
    private String billToAddressCounty;
    private String billToAddressCity;
    private String billToAddressLine1;
    private String billToAddressLine2;
    private String billToAddressLine3;
    private String billToAddressPostcode;


    private ZonedDateTime completeTime;


    private Supplier supplier;


    private Client client;

    private List<OrderLineQueryWrapper> orderLines = new ArrayList<>();
    private List<ParcelPackage> parcelPackages = new ArrayList<>();

    public OrderQueryWrapper() {}

    public OrderQueryWrapper(Order order) {

        String[] fieldNames = {
                "number", "shipToCustomer", "billToCustomer", "category", "transferReceiptNumber", "transferReceiptWarehouseId", "status",
                "shipToContactorFirstname", "shipToContactorLastname", "shipToAddressCountry", "shipToAddressState", "shipToAddressCounty", "shipToAddressCity",
                "shipToAddressLine1", "shipToAddressLine2", "shipToAddressLine3", "shipToAddressPostcode",
                "billToContactorFirstname", "billToContactorLastname", "billToAddressCountry", "billToAddressState", "billToAddressCounty", "billToAddressCity",
                "billToAddressLine1", "billToAddressLine2", "billToAddressLine3", "billToAddressPostcode",
                "completeTime", "supplier","client", "parcelPackages","shipToContactorPhoneNumber"

        };

        ObjectCopyUtil.copyValue(order, this,  fieldNames);
        order.getOrderLines().forEach(
                orderLine ->
                    orderLines.add(new OrderLineQueryWrapper(orderLine))
        );
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

    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    public Customer getShipToCustomer() {
        return shipToCustomer;
    }

    public void setShipToCustomer(Customer shipToCustomer) {
        this.shipToCustomer = shipToCustomer;
    }

    public Customer getBillToCustomer() {
        return billToCustomer;
    }

    public void setBillToCustomer(Customer billToCustomer) {
        this.billToCustomer = billToCustomer;
    }

    public OrderCategory getCategory() {
        return category;
    }

    public void setCategory(OrderCategory category) {
        this.category = category;
    }

    public String getTransferReceiptNumber() {
        return transferReceiptNumber;
    }

    public void setTransferReceiptNumber(String transferReceiptNumber) {
        this.transferReceiptNumber = transferReceiptNumber;
    }

    public Long getTransferReceiptWarehouseId() {
        return transferReceiptWarehouseId;
    }

    public void setTransferReceiptWarehouseId(Long transferReceiptWarehouseId) {
        this.transferReceiptWarehouseId = transferReceiptWarehouseId;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
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

    public String getShipToAddressLine3() {
        return shipToAddressLine3;
    }

    public void setShipToAddressLine3(String shipToAddressLine3) {
        this.shipToAddressLine3 = shipToAddressLine3;
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

    public String getBillToAddressLine3() {
        return billToAddressLine3;
    }

    public void setBillToAddressLine3(String billToAddressLine3) {
        this.billToAddressLine3 = billToAddressLine3;
    }

    public String getBillToAddressPostcode() {
        return billToAddressPostcode;
    }

    public void setBillToAddressPostcode(String billToAddressPostcode) {
        this.billToAddressPostcode = billToAddressPostcode;
    }

    public ZonedDateTime getCompleteTime() {
        return completeTime;
    }

    public void setCompleteTime(ZonedDateTime completeTime) {
        this.completeTime = completeTime;
    }

    public Supplier getSupplier() {
        return supplier;
    }

    public void setSupplier(Supplier supplier) {
        this.supplier = supplier;
    }


    public Client getClient() {
        return client;
    }

    public void setClient(Client client) {
        this.client = client;
    }

    public List<OrderLineQueryWrapper> getOrderLines() {
        return orderLines;
    }

    public void setOrderLines(List<OrderLineQueryWrapper> orderLines) {
        this.orderLines = orderLines;
    }

    public List<ParcelPackage> getParcelPackages() {
        return parcelPackages;
    }

    public void setParcelPackages(List<ParcelPackage> parcelPackages) {
        this.parcelPackages = parcelPackages;
    }

    public String getShipToContactorPhoneNumber() {
        return shipToContactorPhoneNumber;
    }

    public void setShipToContactorPhoneNumber(String shipToContactorPhoneNumber) {
        this.shipToContactorPhoneNumber = shipToContactorPhoneNumber;
    }
}
