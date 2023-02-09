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

import com.garyzhangscm.cwms.inventory.model.Item;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import javax.transaction.Transactional;
import java.util.List;

@Repository
public interface ItemRepository extends JpaRepository<Item, Long>, JpaSpecificationExecutor<Item> {
    Item findByWarehouseIdAndName(Long warehouseId, String name);


    @Query( "select i from Item i " +
            "  where i.warehouseId is null and i.name = :name ")
    Item findGlobalItemByName(String name);

    /**
     * Override a item family in the warehouse level. We will change the item's item family id to the new warehouse level
     * item family. We will only change the item in the specific warehouse
     * @param oldItemFamilyId
     * @param newItemFamilyId
     */
    @Transactional
    @Modifying
    @Query(value = "update item set item_family_id = :newItemFamilyId where item_family_id = :oldItemFamilyId  and warehouse_id = :warehouseId",
            nativeQuery = true)
    void processItemFamilyOverride(Long oldItemFamilyId, Long newItemFamilyId, Long warehouseId);

    List<Item> findByWarehouseId(Long warehouseId);
}
