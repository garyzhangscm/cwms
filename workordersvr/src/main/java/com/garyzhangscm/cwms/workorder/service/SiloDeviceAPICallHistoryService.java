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

package com.garyzhangscm.cwms.workorder.service;

import com.garyzhangscm.cwms.workorder.model.SiloDeviceAPICallHistory;
import com.garyzhangscm.cwms.workorder.repository.SiloDeviceAPICallHistoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


@Service
public class SiloDeviceAPICallHistoryService {
    private static final Logger logger = LoggerFactory.getLogger(SiloDeviceAPICallHistoryService.class);

    @Autowired
    private SiloDeviceAPICallHistoryRepository siloDeviceAPICallHistoryRepository;

    public SiloDeviceAPICallHistory save(SiloDeviceAPICallHistory siloDeviceAPICallHistory) {
        return siloDeviceAPICallHistoryRepository.save(siloDeviceAPICallHistory);
    }


    public SiloDeviceAPICallHistory addSiloDeviceAPICallHistory(SiloDeviceAPICallHistory siloDeviceAPICallHistory) {
        return save(siloDeviceAPICallHistory);
    }

    public List<SiloDeviceAPICallHistory> getLatestBatchOfSiloDeviceAPICallHistory(Long warehouseId) {

        Long latestBatchTimeStamp = siloDeviceAPICallHistoryRepository.getLatestBatchTimeStamp(warehouseId);
        if (Objects.isNull(latestBatchTimeStamp)) {
            return new ArrayList<>();
        }
        return siloDeviceAPICallHistoryRepository.getLatestBatch(latestBatchTimeStamp);

    }
}
