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

import java.io.Serializable;

public class GridDistributionWork implements Serializable {

   private String itemName;

   private Long quantity;

   private String gridLocationName;

   public GridDistributionWork() {}

   public GridDistributionWork(String gridLocationName, String itemName) {
       this.gridLocationName = gridLocationName;
       this.itemName = itemName;
       this.quantity = 0L;
   }
    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public Long getQuantity() {
        return quantity;
    }

    public void addQuantity(Long quantity) {
       this.quantity += quantity;
    }

    public void setQuantity(Long quantity) {
        this.quantity = quantity;
    }

    public String getGridLocationName() {
        return gridLocationName;
    }

    public void setGridLocationName(String gridLocationName) {
        this.gridLocationName = gridLocationName;
    }
}
