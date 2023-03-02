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

package com.garyzhangscm.cwms.common.repository;


import com.garyzhangscm.cwms.common.model.Trailer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;


@Repository
public interface TrailerRepository extends JpaRepository<Trailer, Long>, JpaSpecificationExecutor<Trailer> {
    @Query(
            value = "SELECT * FROM  trailer WHERE company_id = :companyId and warehouse_id = :warehouseId " +
                    // not attached to any tractor
                    " and not exists (select 'x' from tractor_attached_trailer where trailer.trailer_id = tractor_attached_trailer.trailer_id)" +
                    // not attached to any tractor appointment yet
                    " and not exists (select 'x' from tractor_appointment join tractor_appointment_trailer " +
                    "                    on tractor_appointment.tractor_appointment_id = tractor_appointment_trailer.tractor_appointment_id " +
                    "                 where tractor_appointment.status not in ('CANCELLED','COMPLETED') and trailer.trailer_id = tractor_appointment_trailer.trailer_id)" ,
            nativeQuery = true )
    List<Trailer> findTrailersOpenForTractor(Long companyId, Long warehouseId);

    Trailer findByWarehouseIdAndNumber(Long warehouseId, String number);
}
