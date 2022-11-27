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
import com.garyzhangscm.cwms.inventory.model.InventorySnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import javax.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface InventorySnapshotRepository extends JpaRepository<InventorySnapshot, Long>, JpaSpecificationExecutor<InventorySnapshot> {

    /**
     * Override a item in the warehouse level. We will change the item id to the new warehouse level
     * item. We will only change in the specific warehouse
     * @param oldItemId
     * @param newItemId
     */
    @Transactional
    @Modifying
    @Query(value = "update inventory_snapshot_detail set item_id = :newItemId  " +
            " where item_id = :oldItemId " +
            "  and inventory_snapshot_id in (select inventory_snapshot_id from inventory_snapshot where " +
            "       warehouse_id = :warehouseId)",
            nativeQuery = true)
    void processItemOverrideForLine(Long warehouseId, Long oldItemId, Long newItemId);

    @Query(value = "select inventory_snapshot.batch_number, inventory_snapshot.complete_time, " +
            "   item.velocity_id, sum(inventory_snapshot_detail.quantity) total_quantity" +
            "  from inventory_snapshot join inventory_snapshot_detail " +
            "  on inventory_snapshot.inventory_snapshot_id = inventory_snapshot_detail.inventory_snapshot_id" +
            "  join item  on inventory_snapshot_detail.item_id = item.item_id " +
            " where inventory_snapshot.warehouse_id = :warehouseId " +
            "  and inventory_snapshot.complete_time between :startTime and :endTime " +
            "group by inventory_snapshot.batch_number, inventory_snapshot.complete_time, " +
            "    item.velocity_id, inventory_snapshot.inventory_snapshot_id",
            nativeQuery = true)
    // List<Object[]> getInventorySnapshotSummaryByVelocity(Long warehouseId);
    List<Object[]> getInventorySnapshotSummaryByVelocity(Long warehouseId, String startTime, String endTime);

    @Query(value = "select inventory_snapshot.batch_number, inventory_snapshot.complete_time, " +
            "   item.abc_velocity_id, sum(inventory_snapshot_detail.quantity) total_quantity" +
            "  from inventory_snapshot join inventory_snapshot_detail " +
            "  on inventory_snapshot.inventory_snapshot_id = inventory_snapshot_detail.inventory_snapshot_id" +
            "  join item  on inventory_snapshot_detail.item_id = item.item_id " +
            " where inventory_snapshot.warehouse_id = :warehouseId " +
            "  and inventory_snapshot.complete_time between :startTime and :endTime " +
            "group by inventory_snapshot.batch_number, inventory_snapshot.complete_time, " +
            "    inventory_snapshot.inventory_snapshot_id, " +
            "   item.abc_velocity_id",
            nativeQuery = true)
    // List<Object[]> getInventorySnapshotSummaryByABCCategory(Long warehouseId);
    List<Object[]> getInventorySnapshotSummaryByABCCategory(Long warehouseId, String startTime, String endTime);

}
