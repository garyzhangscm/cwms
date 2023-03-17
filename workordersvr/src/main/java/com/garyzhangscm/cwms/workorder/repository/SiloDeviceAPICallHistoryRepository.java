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


import com.garyzhangscm.cwms.workorder.model.SiloDeviceAPICallHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SiloDeviceAPICallHistoryRepository extends JpaRepository<SiloDeviceAPICallHistory, Long>, JpaSpecificationExecutor<SiloDeviceAPICallHistory> {


    @Query(value = "select max(web_api_call_timestamp) web_api_call_timestamp from silo_device_api_call_history " +
            "  where warehouse_id = :warehouseId",
            nativeQuery = true)
    Long getLatestBatchTimeStamp(Long warehouseId);

    @Query("select  sdHistory from SiloDeviceAPICallHistory  sdHistory " +
            "  where webAPICallTimeStamp = :webAPICallTimeStamp")
    List<SiloDeviceAPICallHistory> getLatestBatch(Long webAPICallTimeStamp);
}
