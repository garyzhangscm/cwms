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

package com.garyzhangscm.cwms.workorder.repository;


import com.garyzhangscm.cwms.workorder.model.MaterialRequirementsPlanning;
import com.garyzhangscm.cwms.workorder.model.Mould;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import javax.transaction.Transactional;

@Repository
public interface MaterialRequirementsPlanningRepository extends JpaRepository<MaterialRequirementsPlanning, Long>,
        JpaSpecificationExecutor<MaterialRequirementsPlanning> {

    MaterialRequirementsPlanning findByWarehouseIdAndNumber(Long warehouseId, String number);

    /**
     * Override a item in the warehouse level. We will change  item id to the new warehouse level
     * item. We will only change in the specific warehouse
     * @param oldItemId
     * @param newItemId
     */
    @Transactional
    @Modifying
    @Query(value = "update material_requirements_planning set item_id = :newItemId where item_id = :oldItemId " +
            "  and warehouse_id = :warehouseId",
            nativeQuery = true)
    void processItemOverride(Long warehouseId, Long oldItemId, Long newItemId);

    /**
     * Override a item in the warehouse level. We will change  item id to the new warehouse level
     * item. We will only change in the specific warehouse
     * @param oldItemId
     * @param newItemId
     */
    @Transactional
    @Modifying
    @Query(value = "update material_requirements_planning_line set item_id = :newItemId where item_id = :oldItemId " +
            "  and warehouse_id = :warehouseId",
            nativeQuery = true)
    void processItemOverrideFroLine(Long warehouseId, Long oldItemId, Long newItemId);
}
