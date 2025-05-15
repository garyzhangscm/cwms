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

import com.garyzhangscm.cwms.inventory.model.QCInspectionRequest;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import jakarta.transaction.Transactional;
import java.util.List;

@Repository
public interface QCInspectionRequestRepository extends JpaRepository<QCInspectionRequest, Long>, JpaSpecificationExecutor<QCInspectionRequest> {

    QCInspectionRequest findByWarehouseIdAndNumber(Long warehouseId, String number);

    @Query("select r from QCInspectionRequest r inner join r.inventories i where i.id = :inventoryId")
    List<QCInspectionRequest> findByQCCompletedInventory(Long inventoryId);
    @Query("select r from QCInspectionRequest r inner join r.inventories i where i.lpn = :lpn")
    List<QCInspectionRequest> findByQCCompletedInventory(String lpn);


    /**
     * Override a item in the warehouse level. We will change the item id to the new warehouse level
     * item. We will only change in the specific warehouse
     * @param oldItemId
     * @param newItemId
     */
    @Transactional
    @Modifying
    @Query(value = "update qc_inspection_request set item_id = :newItemId  " +
            " where item_id = :oldItemId and warehouse_id = :warehouseId",
            nativeQuery = true)
    void processItemOverride(Long warehouseId, Long oldItemId, Long newItemId);

}
