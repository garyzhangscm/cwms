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

package com.garyzhangscm.cwms.outbound.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.Serializable;

public class BillOfLadingData implements Serializable {


    private String itemName;
    private String itemDescription;

    private Long quantity;

    private Integer lpnCount;

    private String stockUOMName;
    private String itemFamilyName;
    private String comment;

    public BillOfLadingData(){}


    public BillOfLadingData(String itemName, String itemDescription,
                            Long quantity, Integer lpnCount,
                            String stockUOMName, String itemFamilyName,
                            String comment){
        this.itemName = itemName;
        this.itemDescription = itemDescription;
        this.quantity = quantity;
        this.lpnCount = lpnCount;
        this.stockUOMName = stockUOMName;
        this.itemFamilyName = itemFamilyName;
        this.comment = comment;
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

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public String getItemDescription() {
        return itemDescription;
    }

    public void setItemDescription(String itemDescription) {
        this.itemDescription = itemDescription;
    }

    public Long getQuantity() {
        return quantity;
    }

    public void setQuantity(Long quantity) {
        this.quantity = quantity;
    }

    public Integer getLpnCount() {
        return lpnCount;
    }

    public void setLpnCount(Integer lpnCount) {
        this.lpnCount = lpnCount;
    }

    public String getStockUOMName() {
        return stockUOMName;
    }

    public void setStockUOMName(String stockUOMName) {
        this.stockUOMName = stockUOMName;
    }

    public String getItemFamilyName() {
        return itemFamilyName;
    }

    public void setItemFamilyName(String itemFamilyName) {
        this.itemFamilyName = itemFamilyName;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }
}
