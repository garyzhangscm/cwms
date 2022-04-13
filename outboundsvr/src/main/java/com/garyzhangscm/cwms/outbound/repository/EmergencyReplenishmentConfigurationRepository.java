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

package com.garyzhangscm.cwms.outbound.repository;


import com.garyzhangscm.cwms.outbound.model.AllocationConfiguration;
import com.garyzhangscm.cwms.outbound.model.AllocationConfigurationType;
import com.garyzhangscm.cwms.outbound.model.EmergencyReplenishmentConfiguration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import javax.transaction.Transactional;
import java.util.List;

@Repository
public interface EmergencyReplenishmentConfigurationRepository extends JpaRepository<EmergencyReplenishmentConfiguration, Long>, JpaSpecificationExecutor<EmergencyReplenishmentConfiguration> {

    EmergencyReplenishmentConfiguration findBySequence(int sequence);

    /**
     * Override a item in the warehouse level. We will change the emergency replenishment configuration's item id to the new warehouse level
     * item. We will only change the emergency replenishment configuration in the specific warehouse
     * @param oldItemId
     * @param newItemId
     */
    @Transactional
    @Modifying
    @Query(value = "update emergency_replenishment_configuration set item_id = :newItemId where item_id = :oldItemId  and warehouse_id = :warehouseId",
            nativeQuery = true)
    void processItemOverride(Long oldItemId, Long newItemId, Long warehouseId);
}
