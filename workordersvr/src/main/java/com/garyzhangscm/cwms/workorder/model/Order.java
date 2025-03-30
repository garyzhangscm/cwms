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

package com.garyzhangscm.cwms.workorder.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Order implements Serializable {

    private Long id;

    private String number;

    private Long shipToCustomerId;

    private Customer shipToCustomer;

    private Long warehouseId;

    private OrderStatus status = OrderStatus.OPEN;

    private Warehouse warehouse;

    private Long billToCustomerId;

    private Customer billToCustomer;

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

    private Long carrierId;

    private Carrier carrier;

    private Long carrierServiceLevelId;

    private CarrierServiceLevel carrierServiceLevel;


    private Long clientId;

    private Client client;

    private List<OrderLine> orderLines = new ArrayList<>();



    // Some statistics numbers that we can show
    // in the frontend

    private Integer totalLineCount;

    private Integer totalItemCount;

    private Long totalExpectedQuantity;


    private Long totalOpenQuantity; // Open quantity that is not in shipment yet

    private Long totalInprocessQuantity; // Total quantity that is in shipment
    // totalInprocessQuantity = totalPendingAllocationQuantity + totalOpenPickQuantity + totalPickedQuantity

    private Long totalPendingAllocationQuantity;

    private Long totalOpenPickQuantity;

    private Long totalPickedQuantity;

    private Long totalShippedQuantity;

    @Override
    public boolean equals(Object anotherObj){
        if (this == anotherObj) {
            return true;
        }
        if (!(anotherObj instanceof Order)) {
            return false;
        }
        Order anotherOrder = (Order)anotherObj;
        return getNumber().equals(anotherOrder.getNumber());
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public Customer getShipToCustomer() {
        return shipToCustomer;
    }

    public void setShipToCustomer(Customer shipToCustomer) {
        this.shipToCustomer = shipToCustomer;
    }

    public Long getBillToCustomerId() {
        return billToCustomerId;
    }

    public void setBillToCustomerId(Long billToCustomerId) {
        this.billToCustomerId = billToCustomerId;
    }

    public Customer getBillToCustomer() {
        return billToCustomer;
    }

    public void setBillToCustomer(Customer billToCustomer) {
        this.billToCustomer = billToCustomer;
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

    public Long getClientId() {
        return clientId;
    }

    public void setClientId(Long clientId) {
        this.clientId = clientId;
    }

    public Client getClient() {
        return client;
    }

    public void setClient(Client client) {
        this.client = client;
    }

    public List<OrderLine> getOrderLines() {
        return orderLines;
    }

    public void setOrderLines(List<OrderLine> orderLines) {
        this.orderLines = orderLines;
    }

    public void addOrderLine(OrderLine orderLine) {
        orderLines.add(orderLine);
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

    public Integer getTotalLineCount() {
        return totalLineCount;
    }

    public void setTotalLineCount(Integer totalLineCount) {
        this.totalLineCount = totalLineCount;
    }

    public Integer getTotalItemCount() {
        return totalItemCount;
    }

    public void setTotalItemCount(Integer totalItemCount) {
        this.totalItemCount = totalItemCount;
    }

    public Long getTotalExpectedQuantity() {
        return totalExpectedQuantity;
    }

    public void setTotalExpectedQuantity(Long totalExpectedQuantity) {
        this.totalExpectedQuantity = totalExpectedQuantity;
    }

    public Long getTotalOpenQuantity() {
        return totalOpenQuantity;
    }

    public void setTotalOpenQuantity(Long totalOpenQuantity) {
        this.totalOpenQuantity = totalOpenQuantity;
    }

    public Long getTotalInprocessQuantity() {
        return totalInprocessQuantity;
    }

    public void setTotalInprocessQuantity(Long totalInprocessQuantity) {
        this.totalInprocessQuantity = totalInprocessQuantity;
    }

    public Long getTotalPendingAllocationQuantity() {
        return totalPendingAllocationQuantity;
    }

    public void setTotalPendingAllocationQuantity(Long totalPendingAllocationQuantity) {
        this.totalPendingAllocationQuantity = totalPendingAllocationQuantity;
    }

    public Long getTotalOpenPickQuantity() {
        return totalOpenPickQuantity;
    }

    public void setTotalOpenPickQuantity(Long totalOpenPickQuantity) {
        this.totalOpenPickQuantity = totalOpenPickQuantity;
    }

    public Long getTotalPickedQuantity() {
        return totalPickedQuantity;
    }

    public void setTotalPickedQuantity(Long totalPickedQuantity) {
        this.totalPickedQuantity = totalPickedQuantity;
    }

    public Long getTotalShippedQuantity() {
        return totalShippedQuantity;
    }

    public void setTotalShippedQuantity(Long totalShippedQuantity) {
        this.totalShippedQuantity = totalShippedQuantity;
    }

    public Long getCarrierId() {
        return carrierId;
    }

    public void setCarrierId(Long carrierId) {
        this.carrierId = carrierId;
    }

    public Carrier getCarrier() {
        return carrier;
    }

    public void setCarrier(Carrier carrier) {
        this.carrier = carrier;
    }

    public Long getCarrierServiceLevelId() {
        return carrierServiceLevelId;
    }

    public void setCarrierServiceLevelId(Long carrierServiceLevelId) {
        this.carrierServiceLevelId = carrierServiceLevelId;
    }

    public CarrierServiceLevel getCarrierServiceLevel() {
        return carrierServiceLevel;
    }

    public void setCarrierServiceLevel(CarrierServiceLevel carrierServiceLevel) {
        this.carrierServiceLevel = carrierServiceLevel;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }
}
