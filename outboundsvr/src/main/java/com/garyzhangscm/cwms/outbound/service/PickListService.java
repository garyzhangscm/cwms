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
import com.garyzhangscm.cwms.outbound.exception.PickingException;
import com.garyzhangscm.cwms.outbound.exception.ResourceNotFoundException;
import com.garyzhangscm.cwms.outbound.model.*;
import com.garyzhangscm.cwms.outbound.model.Order;
import com.garyzhangscm.cwms.outbound.repository.PickListRepository;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


import javax.persistence.criteria.*;
import javax.transaction.Transactional;

import java.util.*;


@Service
public class PickListService {
    private static final Logger logger = LoggerFactory.getLogger(PickListService.class);

    @Autowired
    private PickListRepository pickListRepository;
    @Autowired
    private ListPickingConfigurationService listPickingConfigurationService;

    @Autowired
    private CommonServiceRestemplateClient commonServiceRestemplateClient;
    @Autowired
    private WarehouseLayoutServiceRestemplateClient warehouseLayoutServiceRestemplateClient;
    @Autowired
    private InventoryServiceRestemplateClient inventoryServiceRestemplateClient;

    public PickList findById(Long id) {

        return pickListRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.raiseException(" pick list not found by id: " + id));
    }

    public PickList save(PickList pickList) {
        return pickListRepository.save(pickList);

    }

    public List<PickList> findAll(Long warehouseId, String number) {
        return findAll(warehouseId, number, true);
    }
    public List<PickList> findAll(Long warehouseId, String number, boolean loadDetails) {

        List<PickList> pickLists =  pickListRepository.findAll(
                (Root<PickList> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) -> {
                    List<Predicate> predicates = new ArrayList<Predicate>();

                    predicates.add(criteriaBuilder.equal(root.get("warehouseId"), warehouseId));

                    if (StringUtils.isNotBlank(number)) {
                        predicates.add(criteriaBuilder.equal(root.get("number"), number));

                    }

                    Predicate[] p = new Predicate[predicates.size()];
                    return criteriaBuilder.and(predicates.toArray(p));
                }
        );

        if (pickLists.size() > 0 && loadDetails) {
            loadAttribute(pickLists);
        }
        return pickLists;
    }

    public PickList findByNumber(Long warehouseId, String number) {
        return findByNumber(warehouseId, number, true);

    }
    public PickList findByNumber(Long warehouseId, String number, boolean loadDetails) {

        PickList pickList =  pickListRepository.findByWarehouseIdAndNumber(warehouseId, number);
        if (Objects.nonNull(pickList) && loadDetails) {
            loadAttribute(pickList);
        }
        return pickList;
    }

    private void loadAttribute(List<PickList> pickLists) {
        pickLists.forEach(this::loadAttribute);
    }

    private void loadAttribute(PickList pickList) {
        if (Objects.nonNull(pickList.getWarehouseId()) &&
               Objects.isNull(pickList.getWarehouse())) {
            pickList.setWarehouse(warehouseLayoutServiceRestemplateClient.getWarehouseById(
                    pickList.getWarehouseId()
            ));
        }
    }

    public List<PickList> findByGroupKey(String groupKey) {
        return pickListRepository.findByGroupKey(groupKey);
    }

    public List<PickList> findByGroupKeyAndStatus(String groupKey, PickListStatus pickListStatus) {
        return pickListRepository.findByGroupKeyAndStatus(groupKey, pickListStatus);
    }


    public void delete(PickList pickList) {
        pickListRepository.delete(pickList);
    }

    public void delete(Long id) {
        pickListRepository.deleteById(id);
    }

    // Group the pick into a list and return the list
    // if we don't have the list picking policy turned on, or
    // we don't have the matched policy, return null
    @Transactional
    public PickList processPickList(Pick pick) {
        // Step 1. Find the matched configuration
        List<ListPickingConfiguration> listPickingConfigurations = findMatchedListPickingConfiguration(pick);

        logger.debug("We find {} list picking configuration that match with current pick",
                listPickingConfigurations.size());
        if (listPickingConfigurations.size() == 0) {
            // throw PickingException.raiseException("No list picking configuration defined for the current pick " + pick);
            return null;
        }
        try {
            PickList  pickList = findMatchedPickList(listPickingConfigurations, pick);
            return pickList;
        }
        catch (GenericException ex) {
            // OK we can't find any existing pick list for the pick. Let's
            // create a new list based upon the first available pick list
            logger.debug("We can't find any existing picking list, let's try to create one based on the configuration");
            return createPickList(listPickingConfigurations, pick);

        }

    }

    private List<ListPickingConfiguration> findMatchedListPickingConfiguration(Pick pick) {
        return listPickingConfigurationService.findMatchedListPickingConfiguration(pick);

    }

    private PickList findMatchedPickList(List<ListPickingConfiguration> listPickingConfigurations, Pick pick) {

        for(ListPickingConfiguration listPickingConfiguration : listPickingConfigurations) {
            try {
                logger.debug("Start to find existing PENDING picking list based on the configuraiton {}",
                        listPickingConfiguration);
                PickList pickList = findMatchedPickList(listPickingConfiguration, pick);
                return pickList;
            }
            catch (GenericException ex) {
                // if we can't find a list, let's just ignore and continue with the next configuration
                logger.debug("Fail when try the configuration {}, exception: \n{}",
                        listPickingConfiguration.getId(), ex.getMessage());
            }
        }
        throw PickingException.raiseException( "Can't find matched open list while trying all the list pick configurations");

    }

    private PickList findMatchedPickList(ListPickingConfiguration listPickingConfiguration, Pick pick) {

        String groupKey = getGroupKey(listPickingConfiguration, pick);

        logger.debug("Will try to find existing list picking based on groupKey: {}",
                groupKey);
        // Only return the open list with same group key
        List<PickList> pickLists = findByGroupKeyAndStatus(groupKey, PickListStatus.PENDING);
        if (pickLists.size() == 0) {
            throw PickingException.raiseException( "Can't find matched open list with the configuration");
        }
        return pickLists.get(0);
    }

    private String getGroupKey(ListPickingConfiguration listPickingConfiguration, Pick pick) {

        if (Objects.nonNull(pick.getCartonization()) &&
                !validategroupKeyByCartonization(listPickingConfiguration, pick)) {
            String errorMessage = "The picks in the cartonization: " +
                                   pick.getCartonization() +
                                    ", have different group key";

            logger.debug(errorMessage);
            throw PickingException.raiseException(errorMessage);
        }
        String groupKey = "";
        switch (listPickingConfiguration.getGroupRule()) {
            case BY_ORDER:
                groupKey = pick.getOrderNumber();
                break;
            case BY_SHIPMENT:
                groupKey = pick.getShipmentLine().getShipmentNumber();
                break;
            default:
                throw PickingException.raiseException( "Can't get group key from the pick: " + pick);
        }
        return groupKey;
    }

    private boolean validategroupKeyByCartonization(ListPickingConfiguration listPickingConfiguration, Pick pick) {

        Cartonization cartonization = pick.getCartonization();
        // We will get the group key's values from each pick in the cartonization
        // then make sure the value is unique in all the
        Set<String> keyValue = new HashSet<>();
        switch (listPickingConfiguration.getGroupRule()) {
            case BY_ORDER:
                keyValue.add(pick.getOrderNumber());
                break;
            case BY_SHIPMENT:
                keyValue.add(pick.getShipmentLine().getShipmentNumber() );
                break;
            default:
                throw PickingException.raiseException( "Can't get group key from the pick: " + pick);

        }
        return keyValue.size() == 1;
    }

    @Transactional
    protected PickList createPickList(List<ListPickingConfiguration> listPickingConfigurations, Pick pick) {

        // Create the pick list based upon the first configuration
        ListPickingConfiguration listPickingConfiguration = listPickingConfigurations.get(0);
        logger.debug("try to create picking list based on the configuration {}", listPickingConfiguration);
        String groupKey = getGroupKey(listPickingConfiguration, pick);

        PickList pickList = new PickList();
        pickList.setGroupKey(groupKey);
        pickList.setStatus(PickListStatus.PENDING);
        pickList.setWarehouseId(pick.getWarehouseId());
        pickList.setNumber(getNextPickListNumber(pick.getWarehouseId()));
        return save(pickList);
    }

    public void processPickConfirmed(Pick pick) {
        PickList pickList = pick.getPickList();
        if (pickList != null) {
            updatePickListStatus(pickList);
        }
    }

    @Transactional
    protected void updatePickListStatus(PickList pickList) {
        // Load all the picks that belong to this list
        PickList updatedPickList = findById(pickList.getId());
        PickListStatus suggestedPickListStatus = PickListStatus.PENDING;
        List<Pick> picks = pickList.getPicks();
        if (picks.size() == 0) {
            suggestedPickListStatus = PickListStatus.PENDING;
        }
            // 1. If all picks are complete, the list is completed
            // 2. or if all picks are cancelled, the list is cancelled
            // 3. or if any picks are inprocess
        else if (picks.stream().filter(pick -> pick.getStatus().equals(PickStatus.COMPLETED)).count()
                   == picks.size()){
            suggestedPickListStatus = PickListStatus.COMPLETED;
        }
        else if (picks.stream().filter(pick -> pick.getStatus().equals(PickStatus.CANCELLED)).count()
                == picks.size()){
            suggestedPickListStatus = PickListStatus.CANCELLED;
        }
        else if (picks.stream().filter(pick -> pick.getStatus().equals(PickStatus.INPROCESS)).count()
                 > 0){
            // Someone is working on the pick(not neccessary picked anything)
            suggestedPickListStatus = PickListStatus.INPROCESS;
        }
        else if (picks.stream().filter(pick -> pick.getPickedQuantity() > 0).count() > 0){
            // Someone already picked something for at least one pick from the list
            suggestedPickListStatus = PickListStatus.INPROCESS;
        }

        if (!suggestedPickListStatus.equals(updatedPickList.getStatus())) {
            updatedPickList.setStatus(suggestedPickListStatus);
            save(updatedPickList);
        }
    }

    private String getNextPickListNumber(Long warehouseId) {
        return commonServiceRestemplateClient.getNextNumber(warehouseId, "list-pick");
    }





}
