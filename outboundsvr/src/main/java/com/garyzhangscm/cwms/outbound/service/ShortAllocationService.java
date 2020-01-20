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
import com.garyzhangscm.cwms.outbound.clients.WarehouseLayoutServiceRestemplateClient;
import com.garyzhangscm.cwms.outbound.exception.GenericException;
import com.garyzhangscm.cwms.outbound.model.Pick;
import com.garyzhangscm.cwms.outbound.model.PickStatus;
import com.garyzhangscm.cwms.outbound.model.ShortAllocation;
import com.garyzhangscm.cwms.outbound.model.ShortAllocationStatus;
import com.garyzhangscm.cwms.outbound.repository.PickRepository;
import com.garyzhangscm.cwms.outbound.repository.ShortAllocationRepository;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;


@Service
public class ShortAllocationService {
    private static final Logger logger = LoggerFactory.getLogger(ShortAllocationService.class);

    @Autowired
    private ShortAllocationRepository shortAllocationRepository;

    @Autowired
    private CommonServiceRestemplateClient commonServiceRestemplateClient;
    @Autowired
    private WarehouseLayoutServiceRestemplateClient warehouseLayoutServiceRestemplateClient;
    @Autowired
    private InventoryServiceRestemplateClient inventoryServiceRestemplateClient;

    public ShortAllocation findById(Long id, boolean loadDetails) {
        ShortAllocation shortAllocation = shortAllocationRepository.findById(id).orElse(null);
        if (shortAllocation != null && loadDetails) {
            loadOrderAttribute(shortAllocation);
        }
        return shortAllocation;
    }

    public ShortAllocation findById(Long id) {
        return findById(id, true);
    }


    public List<ShortAllocation> findAll(boolean loadDetails) {
        List<ShortAllocation> shortAllocations = shortAllocationRepository.findAll();

        if (shortAllocations.size() > 0 && loadDetails) {
            loadOrderAttribute(shortAllocations);
        }
        return shortAllocations;
    }

    public List<ShortAllocation> findAll() {
        return findAll(true);
    }

    public void loadOrderAttribute(List<ShortAllocation> shortAllocations) {
        for (ShortAllocation shortAllocation : shortAllocations) {
            loadOrderAttribute(shortAllocation);
        }
    }

    public void loadOrderAttribute(ShortAllocation shortAllocation) {

        // Load the item and inventory status information for each lines
        if (shortAllocation.getItemId() != null && shortAllocation.getItem() == null) {
            shortAllocation.setItem(inventoryServiceRestemplateClient.getItemById(shortAllocation.getItemId()));
        }


    }
    public ShortAllocation save(ShortAllocation shortAllocation) {
        ShortAllocation newShortAllocation = shortAllocationRepository.save(shortAllocation);
        loadOrderAttribute(newShortAllocation);
        return newShortAllocation;
    }




    public void delete(ShortAllocation shortAllocation) {
        shortAllocationRepository.delete(shortAllocation);
    }

    public void delete(Long id) {
        shortAllocationRepository.deleteById(id);
    }


    public List<ShortAllocation> cancelShortAllocations(String ShortAllocationIds) {

        return Arrays.stream(ShortAllocationIds.split(",")).mapToLong(Long::parseLong).mapToObj(this::cancelShortAllocation).collect(Collectors.toList());
    }

    public ShortAllocation cancelShortAllocation(Long id) {
        return cancelShortAllocation(findById(id));
    }
    public ShortAllocation cancelShortAllocation(ShortAllocation shortAllocation) {
        if (shortAllocation.getStatus().equals(ShortAllocationStatus.COMPLETED)) {
            throw new GenericException(10000, "Can't cancel short allocation that is already cancelled");
        }
        shortAllocation.setStatus(ShortAllocationStatus.CANCELLED);
        return save(shortAllocation);

    }

}
