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
import com.garyzhangscm.cwms.outbound.clients.InventoryServiceRestemplateClient;
import com.garyzhangscm.cwms.outbound.clients.ResourceServiceRestemplateClient;
import com.garyzhangscm.cwms.outbound.clients.WarehouseLayoutServiceRestemplateClient;
import com.garyzhangscm.cwms.outbound.exception.OrderOperationException;
import com.garyzhangscm.cwms.outbound.exception.ResourceNotFoundException;
import com.garyzhangscm.cwms.outbound.model.*;
import com.garyzhangscm.cwms.outbound.repository.BulkPickRepository;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.util.*;
import java.util.stream.Collectors;


@Service
public class WorkTaskService {
    private static final Logger logger = LoggerFactory.getLogger(WorkTaskService.class);

    @Autowired
    private ResourceServiceRestemplateClient resourceServiceRestemplateClient;

    public WorkTask releaseBulkPick(BulkPick bulkPick) {
        Pick pick = bulkPick.getNextPick();
        if (Objects.isNull(pick)) {
            logger.debug("Can't release the bulk pick" +
                    " as there's no open picks in the bulk");
            return null;
        }
        WorkTask workTask = new WorkTask(
                bulkPick.getWarehouseId(),
                "",
                WorkTaskType.BULK_PICK,
                WorkTaskStatus.PENDING,
                pick.getSourceLocationId(),
                null,
                bulkPick.getNumber());

        return resourceServiceRestemplateClient.addWorkTask(workTask.getWarehouseId(), workTask);
    }

    public WorkTask releasePick(Pick pick) {

        WorkTask workTask = new WorkTask(
                pick.getWarehouseId(),
                "",
                WorkTaskType.PICK,
                WorkTaskStatus.PENDING,
                pick.getSourceLocationId(),
                pick.getDestinationLocationId(),
                pick.getNumber());

        return resourceServiceRestemplateClient.addWorkTask(workTask.getWarehouseId(), workTask);
    }

}
