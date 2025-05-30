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

import com.garyzhangscm.cwms.inventory.model.InventoryStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import jakarta.transaction.Transactional;

@Repository
public interface InventoryStatusRepository extends JpaRepository<InventoryStatus, Long>, JpaSpecificationExecutor<InventoryStatus> {
    InventoryStatus findByWarehouseIdAndName(Long warehouseId, String name);

    @Transactional
    @Modifying
    @Query(value = "update inventory_status set available_status_flag = false where inventory_status_id != :id and warehouse_id = :warehouseId",
            nativeQuery = true)
    void resetAvailableStatus(Long warehouseId, Long id);
}
