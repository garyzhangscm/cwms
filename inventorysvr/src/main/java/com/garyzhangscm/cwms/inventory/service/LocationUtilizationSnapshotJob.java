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

package com.garyzhangscm.cwms.inventory.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.io.IOException;
import java.util.List;

@Component
public class LocationUtilizationSnapshotJob {

    private static final Logger logger = LoggerFactory.getLogger(LocationUtilizationSnapshotJob.class);
    @Autowired
    private LocationUtilizationSnapshotBatchService locationUtilizationSnapshotBatchService;

    @Autowired
    private InventoryConfigurationService inventoryConfigurationService;

    @Scheduled(cron = "0 0 0-23 * * ?")
    public void generateLocationUtilizationSnapshot() throws IOException {

        logger.debug("start to automatically generate the location utilization snapshot");
        List<Long> warehouseIds = inventoryConfigurationService.findLocationUtilizationEnabledWarehouses();
        warehouseIds.forEach(
                warehouseId -> {
                    logger.debug("start to generate location utilization snapshot for warehouse {}", warehouseId);

                    locationUtilizationSnapshotBatchService.generateLocationUtilizationSnapshotBatch(warehouseId);
                }
        );


    }
}
