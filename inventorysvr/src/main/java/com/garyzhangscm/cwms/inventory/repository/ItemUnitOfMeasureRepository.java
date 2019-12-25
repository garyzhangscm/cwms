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

import com.garyzhangscm.cwms.inventory.model.ItemUnitOfMeasure;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface ItemUnitOfMeasureRepository extends JpaRepository<ItemUnitOfMeasure, Long>, JpaSpecificationExecutor<ItemUnitOfMeasure> {

    // Natrual Keys: item name, item package name, unit of measure id
    @Query("select uom from ItemUnitOfMeasure uom " +
           " where uom.unitOfMeasureId = :unitOfMeasureId and uom.itemPackageType.name = :itemPackageTypeName " +
           "   and uom.itemPackageType.item.name = :itemName")
    ItemUnitOfMeasure findByNaturalKeys(String itemName, String itemPackageTypeName, Long unitOfMeasureId);

    // Natrual Keys: item id, item package name, unit of measure id
    @Query("select uom from ItemUnitOfMeasure uom " +
            " where uom.unitOfMeasureId = :unitOfMeasureId and uom.itemPackageType.name = :itemPackageTypeName " +
            "   and uom.itemPackageType.item.id = :itemId")
    ItemUnitOfMeasure findByNaturalKeys(Long itemId, String itemPackageTypeName, Long unitOfMeasureId);

    // Natrual Keys: item package id, unit of measure id
    @Query("select uom from ItemUnitOfMeasure uom " +
            " where uom.unitOfMeasureId = :unitOfMeasureId and uom.itemPackageType.id = :itemPackageTypeId ")
    ItemUnitOfMeasure findByNaturalKeys(Long itemPackageTypeId, Long unitOfMeasureId);
}
