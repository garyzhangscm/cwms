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


import com.garyzhangscm.cwms.outbound.model.Order;
import com.garyzhangscm.cwms.outbound.model.Shipment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long>, JpaSpecificationExecutor<Order> {

    @Query( "select o from Order o " +
            "  where o.warehouseId = :warehouseId " +
            "    and o.number = :number " +
            "    and o.clientId is null")
    Order findByWarehouseIdAndNumber(Long warehouseId, String number);

    Order findByWarehouseIdAndClientIdAndNumber(Long warehouseId, Long clientId, String number);

    @Query("select o from Order o where o.warehouseId = :warehouseId " +
            " and not exists (select 'x' from ShipmentLine sl inner join sl.orderLine ol " +
            "    where ol.order.number = o.number and ol.order.warehouseId = o.warehouseId)")
    List<Order> findOpenOrdersForStop(Long warehouseId);

    @Query("select o from Order o where o.warehouseId = :warehouseId " +
            " and o.number = :number " +
            " and not exists (select 'x' from ShipmentLine sl inner join sl.orderLine ol " +
            "    where ol.order.number = o.number and ol.order.warehouseId = o.warehouseId)")
    List<Order> findOpenOrdersForStopWithNumber(Long warehouseId, String number);

}
