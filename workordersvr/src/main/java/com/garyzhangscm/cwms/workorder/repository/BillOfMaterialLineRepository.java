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


import com.garyzhangscm.cwms.workorder.model.BillOfMaterialLine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import jakarta.transaction.Transactional;

@Repository
public interface BillOfMaterialLineRepository extends JpaRepository<BillOfMaterialLine, Long>, JpaSpecificationExecutor<BillOfMaterialLine> {

    @Query("select line from BillOfMaterialLine line where line.billOfMaterial.number = :billOfMaterialNumber and number = :number")
    BillOfMaterialLine findByNumber(String billOfMaterialNumber, String number);

    /**
     * Override a item in the warehouse level. We will change  item id to the new warehouse level
     * item. We will only change in the specific warehouse
     * @param oldItemId
     * @param newItemId
     */
    @Transactional
    @Modifying
    @Query(value = "update bill_of_material_line set item_id = :newItemId where item_id = :oldItemId " +
            "  and bill_of_material_id in (select bill_of_material_id from bill_of_material where warehouse_id = :warehouseId) ",
            nativeQuery = true)
    void processItemOverride(Long warehouseId, Long oldItemId, Long newItemId);

}
