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

import com.garyzhangscm.cwms.inventory.model.CycleCountRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CycleCountRequestRepository extends JpaRepository<CycleCountRequest, Long>, JpaSpecificationExecutor<CycleCountRequest> {
    List<CycleCountRequest> findByWarehouseIdAndBatchId(Long warehouseId, String batchId);

    @Query("select r from CycleCountRequest r " +
            "  where r.status = com.garyzhangscm.cwms.inventory.model.CycleCountRequestStatus.OPEN " +
            "  and r.batchId = :batchId " +
            "  and r.warehouseId = :warehouseId")
    List<CycleCountRequest> getOpenRequests(Long warehouseId, String batchId);

    @Query("select r from CycleCountRequest r " +
            " where r.status = com.garyzhangscm.cwms.inventory.model.CycleCountRequestStatus.CANCELLED " +
            "  and r.batchId = :batchId "  +
            "  and r.warehouseId = :warehouseId")
    List<CycleCountRequest> getCancelledRequests(Long warehouseId, String batchId);

    @Query("select r from CycleCountRequest r where r.status = com.garyzhangscm.cwms.inventory.model.CycleCountRequestStatus.OPEN and r.locationId = :locationId")
    CycleCountRequest findOpenCycleCountRequestByLocationId(Long locationId);
}
