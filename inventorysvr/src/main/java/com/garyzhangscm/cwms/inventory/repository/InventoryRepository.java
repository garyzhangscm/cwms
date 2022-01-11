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

package com.garyzhangscm.cwms.inventory.repository;

import com.garyzhangscm.cwms.inventory.model.Inventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InventoryRepository extends JpaRepository<Inventory, Long>, JpaSpecificationExecutor<Inventory> {
    List<Inventory> findByWarehouseIdAndLpn(Long warehouseId, String lpn);

    List<Inventory> findByLocationId(Long locationId);

    @Query("select inv from Inventory inv inner join inv.item i where i.id = :itemId and inv.inventoryStatus.id = :inventoryStatusId")
    List<Inventory> findByItemIdAndInventoryStatusId(Long itemId, Long inventoryStatusId);
    @Query("select inv from Inventory inv inner join inv.item i where inv.warehouseId = :warehouseId" +
            "    and inv.inventoryStatus.id = :inventoryStatusId " +
            "  and i.name = :itemName")
    List<Inventory> findByItemNameAndInventoryStatusId(Long warehouseId, String itemName, Long inventoryStatusId);

    @Query("select inv from Inventory inv where inv.warehouseId = :warehouseId and inv.virtual = :virtual")
    List<Inventory> findByVirtual(Long warehouseId, Boolean virtual);

    @Query("select inv from Inventory inv inner join inv.inventoryMovements im where im.locationId = :locationId")
    List<Inventory> findPendingInventoryByLocationId(Long locationId);
}
