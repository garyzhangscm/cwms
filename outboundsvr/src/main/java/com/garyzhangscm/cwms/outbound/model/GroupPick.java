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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;

import javax.persistence.*;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public interface GroupPick {

    public String getPickListNumber();
    public Long getId();

    public String getNumber();
    public PickGroupType getGroupType();

    public Long getSourceLocationId();

    public Location getSourceLocation();

    public ShipmentLine getShipmentLine();

    public Long getDestinationLocationId();

    public Location getDestinationLocation();

    public Long getItemId();

    public Item getItem();

    public Long getQuantity();

    public Long getPickedQuantity();

    public PickStatus getStatus();

    public String getOrderNumber();
    public List<PickMovement> getPickMovements();

    public InventoryStatus getInventoryStatus();

    public ShortAllocation getShortAllocation();

    public PickList getPickList();

    public Long getWarehouseId();

    public BulkPick getBulkPick();

    public Warehouse getWarehouse();

    public Client getClient();

    public PickType getPickType();

    public Long getWorkOrderLineId();

    public Long getInventoryStatusId();

    public Cartonization getCartonization();

    public String getCartonizationNumber();
    public Long getUnitOfMeasureId();

    public String getLpn();

    public boolean isConfirmItemFlag();

    public boolean isConfirmLocationFlag();

    public boolean isConfirmLocationCodeFlag();

    public Long getWorkId();
    public boolean isConfirmLpnFlag();

    // for pick sheet display only
    public String getDefaultPickableStockUomName();

    public Long getPickingByUserId();

    public Long getAssignedToUserId();

    public String getColor();

    public String getProductSize();

    public String getStyle();
    public String getBulkPickNumber();

    public User getPickingByUser();
    public User getAssignedToUser();
}
