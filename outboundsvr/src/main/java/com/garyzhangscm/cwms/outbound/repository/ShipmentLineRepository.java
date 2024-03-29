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

import com.garyzhangscm.cwms.outbound.model.ShipmentLine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ShipmentLineRepository extends JpaRepository<ShipmentLine, Long>, JpaSpecificationExecutor<ShipmentLine> {

    @Query("select sl from ShipmentLine sl inner join sl.wave w where w.id =  :waveId")
    List<ShipmentLine> findByWaveId(Long waveId);

    @Query("select sl from ShipmentLine sl inner join sl.orderLine ol " +
            " inner join ol.order o where o.number =  :orderNumber and sl.warehouseId = :warehouseId")
    List<ShipmentLine> findByOrderNumber(Long warehouseId, String orderNumber);
}
