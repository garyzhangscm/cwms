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

import com.garyzhangscm.cwms.workorder.model.WorkOrder;
import com.garyzhangscm.cwms.workorder.model.WorkOrderStatus;
import org.hibernate.jdbc.Work;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import javax.transaction.Transactional;
import java.util.List;

@Repository
public interface WorkOrderRepository extends JpaRepository<WorkOrder, Long>, JpaSpecificationExecutor<WorkOrder> {
    WorkOrder findByWarehouseIdAndNumber(Long warehouseId, String number);

    @Query("select wo from WorkOrder wo where wo.status  = com.garyzhangscm.cwms.workorder.model.WorkOrderStatus.INPROCESS" +
            " and wo.warehouseId = :warehouseId")
    List<WorkOrder> findInprocessWorkOrder(Long warehouseId);


    @Query("select wo from WorkOrder wo where wo.itemId = :itemId " +
            " and wo.status != com.garyzhangscm.cwms.workorder.model.WorkOrderStatus.CANCELLED " +
            " and wo.status != com.garyzhangscm.cwms.workorder.model.WorkOrderStatus.COMPLETED " +
            " and wo.status != com.garyzhangscm.cwms.workorder.model.WorkOrderStatus.CLOSED ")
    List<WorkOrder> findOpenWorkOrderByItem(Long itemId);

    /**
     * Override a item in the warehouse level. We will change the work order line's item id to the new warehouse level
     * item. We will only change the work order line in the specific warehouse
     * @param oldItemId
     * @param newItemId
     */
    @Transactional
    @Modifying
    @Query(value = "update work_order set item_id = :newItemId where item_id = :oldItemId  and warehouse_id = :warehouseId",
            nativeQuery = true)
    void processItemOverride(Long oldItemId, Long newItemId, Long warehouseId);
}
