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

package com.garyzhangscm.cwms.outbound.service;

import com.garyzhangscm.cwms.outbound.clients.CommonServiceRestemplateClient;
import com.garyzhangscm.cwms.outbound.exception.OrderOperationException;
import com.garyzhangscm.cwms.outbound.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Objects;


@Service
public class PickReleaseService {
    private static final Logger logger = LoggerFactory.getLogger(PickReleaseService.class);

    @Autowired
    private WorkTaskService workTaskService;

    @Autowired
    private BulkPickConfigurationService bulkPickConfigurationService;

    @Autowired
    private PickConfigurationService pickConfigurationService;

    public BulkPick releaseBulkPick(BulkPick bulkPick) {
        BulkPickConfiguration bulkPickConfiguration =
                bulkPickConfigurationService.findByWarehouse(bulkPick.getWarehouseId());
        if (Objects.nonNull(bulkPickConfiguration) &&
                Boolean.TRUE.equals(bulkPickConfiguration.getReleaseToWorkTask())) {
            logger.debug("bulk pick configuration is setup and the pick will be released into work task");
            WorkTask workTask = workTaskService.releaseBulkPick(bulkPick);
            if (Objects.nonNull(workTask)) {
                bulkPick.setStatus(PickStatus.RELEASED);
                bulkPick.setWorkTaskId(workTask.getId());
            }
        }
        else {
            // if we don't need to release to work task, then set the bulk pick's status
            // to released
            logger.debug("bulk pick configuration is not setup for work task, let's just mark the bulk pick as released");
            bulkPick.setStatus(PickStatus.RELEASED);
        }
        return bulkPick;
    }

    public Pick releasePick(Pick pick) {
        logger.debug("start to release pick {}", pick.getNumber());

        PickConfiguration pickConfiguration =
                pickConfigurationService.findByWarehouse(pick.getWarehouseId());
        if (Objects.nonNull(pickConfiguration) &&
                Boolean.TRUE.equals(pickConfiguration.getReleaseToWorkTask())) {
            WorkTask workTask = workTaskService.releasePick(pick);
            if (Objects.nonNull(workTask)) {
                pick.setStatus(PickStatus.RELEASED);
                pick.setWorkTaskId(workTask.getId());
            }
        }
        else {
            // if we don't need to release to work task, then set the bulk pick's status
            // to released
            pick.setStatus(PickStatus.RELEASED);
        }
        return pick;
    }

    public PickList releasePickList(PickList pickList) {
        logger.debug("start to release pick list {}", pickList.getNumber());

        PickConfiguration pickConfiguration =
                pickConfigurationService.findByWarehouse(pickList.getWarehouseId());
        if (Objects.nonNull(pickConfiguration) &&
                Boolean.TRUE.equals(pickConfiguration.getReleasePickListToWorkTask())) {
            WorkTask workTask = workTaskService.releasePickList(pickList);
            if (Objects.nonNull(workTask)) {
                pickList.setStatus(PickListStatus.RELEASED);
                pickList.setWorkTaskId(workTask.getId());
            }
        }
        else {
            // if we don't need to release to work task, then set the bulk pick's status
            // to released
            pickList.setStatus(PickListStatus.RELEASED);
        }
        return pickList;
    }
}
