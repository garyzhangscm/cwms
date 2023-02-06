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
import com.garyzhangscm.cwms.inventory.model.QuickbookDesktopInventorySummary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import javax.transaction.Transactional;
import java.util.List;

@Repository
public interface InventoryRepository extends JpaRepository<Inventory, Long>, JpaSpecificationExecutor<Inventory> {
    List<Inventory> findByWarehouseIdAndLpn(Long warehouseId, String lpn);

    List<Inventory> findByLocationId(Long locationId);

    @Query("select inv from Inventory inv inner join inv.item i where i.id = :itemId and inv.inventoryStatus.id = :inventoryStatusId")
    List<Inventory> findByItemIdAndInventoryStatusId(Long itemId, Long inventoryStatusId);

    @Query("select inv from Inventory inv inner join inv.item i where i.id = :itemId " +
            " and inv.inventoryStatus.id = :inventoryStatusId and inv.locationId = :locationId")
    List<Inventory> findByItemIdAndInventoryStatusIdAndLocationId(Long itemId, Long inventoryStatusId, Long locationId);

    @Query("select inv from Inventory inv inner join inv.item i where inv.warehouseId = :warehouseId" +
            "    and inv.inventoryStatus.id = :inventoryStatusId " +
            "  and i.name = :itemName")
    List<Inventory> findByItemNameAndInventoryStatusId(Long warehouseId, String itemName, Long inventoryStatusId);

    @Query("select inv from Inventory inv where inv.warehouseId = :warehouseId and inv.virtual = :virtual")
    List<Inventory> findByVirtual(Long warehouseId, Boolean virtual);

    @Query("select inv from Inventory inv inner join inv.inventoryMovements im where im.locationId = :locationId")
    List<Inventory> findPendingInventoryByLocationId(Long locationId);


    @Query("select count(distinct inv.locationId) from Inventory inv join inv.item i where i.id = :itemId")
    Integer getLocationCount(Long itemId);

    /**
     * Override a item in the warehouse level. We will change the inventory's item id to the new warehouse level
     * item. We will only change the inventory in the specific warehouse
     * @param oldItemId
     * @param newItemId
     */
    @Transactional
    @Modifying
    @Query(value = "update inventory set item_id = :newItemId, item_package_type_id = :newItemPackageTypeId " +
            " where item_id = :oldItemId and warehouse_id = :warehouseId",
            nativeQuery = true)
    void processItemOverride(Long oldItemId, Long newItemId, Long newItemPackageTypeId, Long warehouseId);


    @Query(value =" select item.name itemName, item.quickbook_listid listId, " +
            " inventory_status.name inventoryStatus, sum(inventory.quantity) quantity" +
            " from inventory join item on inventory.item_id = item.item_id " +
            " join inventory_status on inventory.inventory_status_id = inventory_status.inventory_status_id " +
            "  where inventory.warehouse_id = :warehouseId " +
            "    and item.quickbook_listid is not null " +
            "    and item.quickbook_listid != '' " +
            "    and inventory.virtual_inventory = false " +
            "group by item.name, item.quickbook_listid, inventory_status.name ",
            nativeQuery = true)
    List<Object[]> getQuickbookDesktopInventorySummary(Long warehouseId);

    @Query(value =" select item.name itemName, item.quickbook_listid listId, " +
            " inventory_status.name inventoryStatus, sum(inventory.quantity) quantity" +
            " from inventory join item on inventory.item_id = item.item_id " +
            " join inventory_status on inventory.inventory_status_id = inventory_status.inventory_status_id " +
            "  where inventory.warehouse_id = :warehouseId " +
            "    and item.quickbook_listid is not null " +
            "    and item.quickbook_listid != '' " +
            "    and inventory.virtual_inventory = false " +
            "    and item.name = :itemName " +
            "group by item.name, item.quickbook_listid, inventory_status.name ",
            nativeQuery = true)
    List<Object[]> getQuickbookDesktopInventorySummary(Long warehouseId, String itemName);
}
