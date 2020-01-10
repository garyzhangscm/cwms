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
import com.garyzhangscm.cwms.outbound.model.*;
import com.garyzhangscm.cwms.outbound.repository.PickRepository;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


@Service
public class PickService {
    private static final Logger logger = LoggerFactory.getLogger(PickService.class);

    @Autowired
    private PickRepository pickRepository;

    @Autowired
    private CommonServiceRestemplateClient commonServiceRestemplateClient;
    @Autowired
    private WarehouseLayoutServiceRestemplateClient warehouseLayoutServiceRestemplateClient;
    @Autowired
    private InventoryServiceRestemplateClient inventoryServiceRestemplateClient;

    public Pick findById(Long id, boolean loadDetails) {
        Pick pick = pickRepository.findById(id).orElse(null);
        if (pick != null && loadDetails) {
            loadOrderAttribute(pick);
        }
        return pick;
    }

    public Pick findById(Long id) {
        return findById(id, true);
    }


    public List<Pick> findAll(String number, boolean loadDetails) {
        List<Pick> picks;

        if (StringUtils.isBlank(number)) {
            picks = pickRepository.findAll();
        } else {
            Pick pick = pickRepository.findByNumber(number);
            if (pick != null) {
                picks = Arrays.asList(new Pick[]{pick});
            } else {
                picks = new ArrayList<>();
            }
        }
        if (picks.size() > 0 && loadDetails) {
            loadOrderAttribute(picks);
        }
        return picks;
    }

    public List<Pick> findAll(String number) {
        return findAll(number, true);
    }

    public Pick findByNumber(String number, boolean loadDetails) {
        Pick pick = pickRepository.findByNumber(number);
        if (pick != null && loadDetails) {
            loadOrderAttribute(pick);
        }
        return pick;
    }

    public Pick findByNumber(String number) {
        return findByNumber(number, true);
    }


    public void loadOrderAttribute(List<Pick> picks) {
        for (Pick pick : picks) {
            loadOrderAttribute(pick);
        }
    }

    public void loadOrderAttribute(Pick pick) {
        // Load the details for client and supplier informaiton
        if (pick.getSourcelLocationId() != null && pick.getSourcelLocation() == null) {
            pick.setSourcelLocation(warehouseLayoutServiceRestemplateClient.getLocationById(pick.getSourcelLocationId()));
        }
        if (pick.getDestinationLocationId() != null && pick.getDestinationLocation() == null) {
            pick.setDestinationLocation(warehouseLayoutServiceRestemplateClient.getLocationById(pick.getDestinationLocationId()));
        }

        // Load the item and inventory status information for each lines
        if (pick.getItemId() != null && pick.getItem() == null) {
            pick.setItem(inventoryServiceRestemplateClient.getItemById(pick.getItemId()));
        }


    }



    public Pick save(Pick pick) {
        Pick newPick = pickRepository.save(pick);
        loadOrderAttribute(newPick);
        return newPick;
    }

    public Pick saveOrUpdate(Pick pick) {
        if (pick.getId() == null && findByNumber(pick.getNumber()) != null) {
            pick.setId(findByNumber(pick.getNumber()).getId());
        }
        return save(pick);
    }


    public void delete(Pick pick) {
        pickRepository.delete(pick);
    }

    public void delete(Long id) {
        pickRepository.deleteById(id);
    }




}
