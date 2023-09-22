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

import com.garyzhangscm.cwms.workorder.model.ProductionLineAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.ZonedDateTime;
import java.util.List;

@Repository
public interface ProductionLineAssignmentRepository extends JpaRepository<ProductionLineAssignment, Long>, JpaSpecificationExecutor<ProductionLineAssignment> {

    @Query("select a from ProductionLineAssignment a where a.workOrder.id = :workOrderId and a.productionLine.id = :productionLineId")
    ProductionLineAssignment findByWorkOrderAndProductionLine(Long workOrderId, Long productionLineId);

    @Query("select a from ProductionLineAssignment a where a.warehouseId = :warehouseId " +
            " and (" +
            "        (a.assignedTime <= :startTime and (a.deassigned = false or a.deassignedTime > :startTime))" + // assigned before start time and still assigned at start time
            "        or " +
            "        (a.assignedTime >= :startTime and a.assignedTime < :endTime)   " + // assigned between the start and end time
            "     ) ")
    List<ProductionLineAssignment> getProductionAssignmentByTimeRange(Long warehouseId, ZonedDateTime startTime, ZonedDateTime endTime);
}
