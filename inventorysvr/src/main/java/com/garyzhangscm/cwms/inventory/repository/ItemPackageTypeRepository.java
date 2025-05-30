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

import com.garyzhangscm.cwms.inventory.model.ItemPackageType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface ItemPackageTypeRepository extends JpaRepository<ItemPackageType, Long>, JpaSpecificationExecutor<ItemPackageType> {
    ItemPackageType findByName(String name);

    @Query("select itemPackageType from ItemPackageType itemPackageType " +
           " where itemPackageType.name = :name and itemPackageType.item.id = :itemId and itemPackageType.warehouseId = :warehouseId")
    ItemPackageType findByNaturalKeys(Long warehouseId, Long itemId, String name);

    @Query("select itemPackageType from ItemPackageType itemPackageType " +
            " where itemPackageType.name = :name and itemPackageType.item.name = :itemName " +
            "   and itemPackageType.warehouseId = :warehouseId" +
            "   and ((:clientId is not null and itemPackageType.clientId = :clientId)" +
            "         or (:clientId is null and itemPackageType.clientId is null))")
    ItemPackageType findByNaturalKeys(Long warehouseId, Long clientId, String itemName, String name);
}
