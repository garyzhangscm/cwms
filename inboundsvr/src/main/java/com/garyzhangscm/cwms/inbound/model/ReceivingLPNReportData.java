/**
 * Copyright 2018
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

package com.garyzhangscm.cwms.inbound.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.Serializable;

public class ReceivingLPNReportData implements Serializable {


    private String itemFamily;
    private String itemName;
    private String receiptNumber;
    private String supplier;
    private String checkInDate;
    private String quantity;
    private String lpn;

    public ReceivingLPNReportData(){}
    public ReceivingLPNReportData(String itemFamily, String itemName, String receiptNumber, String supplier, String checkInDate, String quantity, String lpn) {
        this.itemFamily = itemFamily;
        this.itemName = itemName;
        this.receiptNumber = receiptNumber;
        this.supplier = supplier;
        this.checkInDate = checkInDate;
        this.quantity = quantity;
        this.lpn = lpn;
    }

    public String getItemFamily() {
        return itemFamily;
    }

    public void setItemFamily(String itemFamily) {
        this.itemFamily = itemFamily;
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public String getReceiptNumber() {
        return receiptNumber;
    }

    public void setReceiptNumber(String receiptNumber) {
        this.receiptNumber = receiptNumber;
    }

    public String getSupplier() {
        return supplier;
    }

    public void setSupplier(String supplier) {
        this.supplier = supplier;
    }

    public String getCheckInDate() {
        return checkInDate;
    }

    public void setCheckInDate(String checkInDate) {
        this.checkInDate = checkInDate;
    }

    public String getQuantity() {
        return quantity;
    }

    public void setQuantity(String quantity) {
        this.quantity = quantity;
    }

    public String getLpn() {
        return lpn;
    }

    public void setLpn(String lpn) {
        this.lpn = lpn;
    }
}
