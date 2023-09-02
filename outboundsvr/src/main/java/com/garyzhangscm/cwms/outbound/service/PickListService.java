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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.garyzhangscm.cwms.outbound.clients.CommonServiceRestemplateClient;
import com.garyzhangscm.cwms.outbound.clients.InventoryServiceRestemplateClient;
import com.garyzhangscm.cwms.outbound.clients.ResourceServiceRestemplateClient;
import com.garyzhangscm.cwms.outbound.clients.WarehouseLayoutServiceRestemplateClient;
import com.garyzhangscm.cwms.outbound.exception.GenericException;
import com.garyzhangscm.cwms.outbound.exception.OrderOperationException;
import com.garyzhangscm.cwms.outbound.exception.PickingException;
import com.garyzhangscm.cwms.outbound.exception.ResourceNotFoundException;
import com.garyzhangscm.cwms.outbound.model.*;
import com.garyzhangscm.cwms.outbound.model.Order;
import com.garyzhangscm.cwms.outbound.repository.PickListRepository;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


import javax.persistence.criteria.*;
import javax.transaction.Transactional;

import java.util.*;
import java.util.stream.Collectors;


@Service
public class PickListService {
    private static final Logger logger = LoggerFactory.getLogger(PickListService.class);

    @Autowired
    private PickListRepository pickListRepository;
    @Autowired
    private ListPickConfigurationService listPickConfigurationService;
    @Autowired
    private PickService pickService;
    @Autowired
    private UserService userService;

    @Autowired
    private CommonServiceRestemplateClient commonServiceRestemplateClient;
    @Autowired
    private WarehouseLayoutServiceRestemplateClient warehouseLayoutServiceRestemplateClient;
    @Autowired
    private InventoryServiceRestemplateClient inventoryServiceRestemplateClient;
    @Autowired
    private ResourceServiceRestemplateClient resourceServiceRestemplateClient;

    @Autowired
    private PickReleaseService pickReleaseService;

    public PickList findById(Long id) {
        return findById(id, true);
    }
    public PickList findById(Long id, boolean loadDetails) {

        PickList pickList = pickListRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.raiseException(" pick list not found by id: " + id));
        if (Objects.nonNull(pickList) && loadDetails) {
            loadAttribute(pickList);
        }
        return pickList;
    }

    public PickList save(PickList pickList) {
        return pickListRepository.save(pickList);

    }

    public PickList saveOrUpdate(PickList pickList) {
        if (Objects.isNull(pickList.getId()) &&
                Objects.nonNull(findByNumber(pickList.getWarehouseId(), pickList.getNumber(), false))) {
            pickList.setId(findByNumber(pickList.getWarehouseId(), pickList.getNumber(), false).getId());
        }
        return save(pickList);
    }

    public List<PickList> findAll(Long warehouseId, String number, String numberList) {
        return findAll(warehouseId, number, numberList, true);
    }
    public List<PickList> findAll(Long warehouseId, String number, String numberList, boolean loadDetails) {

        List<PickList> pickLists =  pickListRepository.findAll(
                (Root<PickList> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) -> {
                    List<Predicate> predicates = new ArrayList<Predicate>();

                    predicates.add(criteriaBuilder.equal(root.get("warehouseId"), warehouseId));

                    if (StringUtils.isNotBlank(number)) {
                        predicates.add(criteriaBuilder.equal(root.get("number"), number));

                    }
                    if (StringUtils.isNotBlank(numberList)) {
                        CriteriaBuilder.In<String> inNumbers = criteriaBuilder.in(root.get("number"));
                        for(String bulkPickNumber : numberList.split(",")) {
                            inNumbers.value(bulkPickNumber);
                        }
                        predicates.add(criteriaBuilder.and(inNumbers));

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
        if (Objects.nonNull(pickList.getWorkTaskId()) &&
                Objects.isNull(pickList.getWorkTask())) {
            try {
                pickList.setWorkTask(resourceServiceRestemplateClient.getWorkTaskById(
                        pickList.getWarehouseId(),
                        pickList.getWorkTaskId()
                ));
            }
            catch (Exception ex) {}
        }
        pickService.loadAttribute(pickList.getPicks());

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
    /**
    @Transactional
    public PickList processPickList(Pick pick) {
        return processPickList(pick, new ArrayList<>());
    }
    **/

    /**
     * Group the pick into a list and return the list
     * if we don't have the list picking policy turned on, or
     * we don't have the matched policy, return null. the function accept
     * a list of pick list that created in the same transaction. It is useful
     * when
     * 1. those will be the first priority to group the pick into
     * 2. if the list configuration doesn't allow new pick to be added into
     *    existing list, then those are the only option to add the pick
     * @param pick
     * @param pickLists
     * @return
     */
    @Transactional
    public PickList processPickList(Pick pick, List<PickList> pickLists) {
        logger.debug("Start to process pick list potential for pick {} ",
                pick.getNumber());
        // first, let's check if the pick is enabled
        // 1. globally
        // 2. enabled for the customer
        // 3. etc(see the method for the details

        if(!listPickEnabled(pick)) {
            logger.debug("list pick is not enabled for warehouse {}, skip list for pick {}",
                    pick.getWarehouseId(),
                    pick.getNumber());

            return null;
        }
        // Step 1. Find the matched configuration
        List<ListPickConfiguration> listPickConfigurations = findMatchedListPickingConfiguration(pick);

        logger.debug("We find {} list picking configuration that match with current pick, start to process list for pick {}",
                listPickConfigurations.size(),
                pick.getNumber());
        if (listPickConfigurations.size() == 0) {
            // throw PickingException.raiseException("No list picking configuration defined for the current pick " + pick);
            return null;
        }
        try {
            logger.debug("start to find matched pick list for pick {}, with {} existing list in the current session",
                    pick.getNumber(), pickLists.size());
            pickLists.forEach(
                    pickList -> logger.debug(">> existing list: {}, number of picks: {}", pickList.getNumber(),
                            pickList.getPicks().size())
            );
            PickList pickList = findMatchedPickList(listPickConfigurations, pick, pickLists);

            if (Objects.nonNull(pickList)) {
                // if we found an existing list, let's add the pick to the list
                logger.debug("add pick {} to the list {}",
                        pick.getNumber(),
                        pickList.getNumber());
                pickService.assignPickToList(pick, pickList);
            }

            return pickList;
        }
        catch (GenericException ex) {
            // OK we can't find any existing pick list for the pick. Let's
            // create a new list based upon the first available pick list
            logger.debug("We can't find any existing picking list, let's try to create one based on the configuration");
            PickList pickList = createPickList(listPickConfigurations, pick);
            // let's release the list pick
            pickList = pickReleaseService.releasePickList(pickList);
            // add the new pick list to the temporary list so that
            // we can consider the list as first priority for grouping
            // new picks from the same session
            pickLists.add(pickList);

            return pickList;

        }

    }


    private boolean listPickEnabled(Pick pick) {

        // see if the list pick is enabled for the warehouse
        WarehouseConfiguration warehouseConfiguration =
                warehouseLayoutServiceRestemplateClient.getWarehouseConfiguration(
                        pick.getWarehouseId()
                );
        if (Objects.isNull(warehouseConfiguration) ||
                !Boolean.TRUE.equals(warehouseConfiguration.getListPickEnabledFlag())) {
            // warehouse configuration is not setup
            // or list pick is not enabled
            logger.debug("Pick list is not enabled for the warehouse ");
            return false;
        }

        // see if the list pick is enabled for the client
        Client client = pick.getShipmentLine().getOrderLine().getOrder().getClient();
        if (Objects.isNull(client)) {
            Long clientId = pick.getShipmentLine().getOrderLine().getOrder().getClientId();
            if (Objects.nonNull(clientId)) {
                client = commonServiceRestemplateClient.getClientById(clientId);
            }
        }

        if (Objects.nonNull(client) && !Boolean.TRUE.equals(client.getListPickEnabledFlag())) {

            // ok the order belongs to certain client but list pick is not enabled
            // for the client
            logger.debug("pick list function is not enabled for client {}",
                    client.getName());

            return false;
        }

        // see if list pick is enabled for the customer
        Customer shipToCustomer = pick.getShipmentLine().getOrderLine().getOrder().getShipToCustomer();
        if (Objects.isNull(shipToCustomer)) {
            Long customerId = pick.getShipmentLine().getOrderLine().getOrder().getShipToCustomerId();
            if (Objects.nonNull(customerId)) {
                shipToCustomer = commonServiceRestemplateClient.getCustomerById(customerId);
            }
        }

        if (Objects.nonNull(shipToCustomer) && !Boolean.TRUE.equals(shipToCustomer.getListPickEnabledFlag())) {

            // ok the order belongs to certain customer but list pick is not enabled
            // for the client
            logger.debug("pick list function is not enabled for ship to customer {}",
                    shipToCustomer.getName());
            return false;
        }


        return true;

    }

    private List<ListPickConfiguration> findMatchedListPickingConfiguration(Pick pick) {
        return listPickConfigurationService.findMatchedListPickConfiguration(pick);

    }

    private PickList findMatchedPickList(List<ListPickConfiguration> listPickConfigurations, Pick pick) {
        return findMatchedPickList(listPickConfigurations, pick, new ArrayList<>());
    }
    private PickList findMatchedPickList(List<ListPickConfiguration> listPickConfigurations, Pick pick,
                                         List<PickList> pickLists) {

        for(ListPickConfiguration listPickConfiguration : listPickConfigurations) {
            try {
                logger.debug("Start to find existing PENDING picking list based on the configuraiton {}",
                        listPickConfiguration);
                PickList pickList = findMatchedPickList(listPickConfiguration, pick, pickLists);
                return pickList;
            }
            catch (GenericException ex) {
                // if we can't find a list, let's just ignore and continue with the next configuration
                logger.debug("Fail when try the configuration {}, exception: \n{}",
                        listPickConfiguration.getId(), ex.getMessage());
            }
        }
        throw PickingException.raiseException( "Can't find matched open list while trying all the list pick configurations");

    }

    private PickList findMatchedPickList(ListPickConfiguration listPickConfiguration, Pick pick) {
        return findMatchedPickList(listPickConfiguration, pick, new ArrayList<>());
    }
    private PickList findMatchedPickList(ListPickConfiguration listPickConfiguration, Pick pick,
                                         List<PickList> pickListsFromSameSession) {

        String groupKey = getGroupKey(listPickConfiguration, pick);

        // see if we have pick list with same group key that is created
        // from the same session
        for (PickList pickList : pickListsFromSameSession) {
            logger.debug("start to check if save session pick list {} with status {} " +
                            " has the same group key {} as the pick's group key {}" +
                    pickList.getNumber(),
                    pickList.getStatus(),
                    pickList.getGroupKey(),
                    groupKey);
            if (pickList.getGroupKey().equalsIgnoreCase(groupKey) &&
                    (pickList.getStatus().equals(PickListStatus.PENDING) || pickList.getStatus().equals(PickListStatus.RELEASED))) {
                return pickList;
            }
        }

        // ok, we don't have list with same group key, we will need to find from
        // existing but pending list(list that no one started yet)
        // ONLY if the configuration allows to add new pick into existing list
        if (Boolean.TRUE.equals(listPickConfiguration.getAllowAddToExistingList())) {

            logger.debug("Will try to find existing list picking based on groupKey: {}",
                    groupKey);
            // Only return the open list with same group key
            List<PickList> pickLists = findByGroupKeyAndStatus(groupKey, PickListStatus.PENDING);
            if (pickLists.size() == 0) {
                throw PickingException.raiseException( "Can't find matched open list with the configuration");
            }

            return pickLists.get(0);
        }
        else {

            throw PickingException.raiseException( "Configuration doesn't allow adding new pick into existing list");
        }
    }

    private String getGroupKey(ListPickConfiguration listPickConfiguration, Pick pick) {

        if (Objects.nonNull(pick.getCartonization()) &&
                !validategroupKeyByCartonization(listPickConfiguration, pick)) {
            String errorMessage = "The picks in the cartonization: " +
                                   pick.getCartonization() +
                                    ", have different group key";

            logger.debug(errorMessage);
            throw PickingException.raiseException(errorMessage);
        }
        List<String> groupKeyList = new ArrayList<>();
        listPickConfiguration.getGroupRules().forEach(
            groupRule -> {
                switch (groupRule.getGroupRuleType()) {
                    case BY_ORDER:
                        groupKeyList.add(pick.getOrderNumber());
                        break;
                    case BY_SHIPMENT:
                        groupKeyList.add(pick.getShipmentLine().getShipmentNumber());
                        break;
                    case BY_CUSTOMER:
                        groupKeyList.add(
                                Objects.nonNull(pick.getShipmentLine().getOrderLine().getOrder().getShipToCustomerId()) ?
                                        pick.getShipmentLine().getOrderLine().getOrder().getShipToCustomerId().toString() :
                                        "****"
                                );
                        break;
                    case BY_ITEM:
                        groupKeyList.add(
                                Objects.nonNull(pick.getShipmentLine().getOrderLine().getItemId()) ?
                                        pick.getShipmentLine().getOrderLine().getItemId().toString() :
                                        "****"
                        );
                        break;
                    case BY_TRAILER_APPOINTMENT:
                        if (Objects.nonNull(pick.getShipmentLine()) &&
                               Objects.nonNull(pick.getShipmentLine().getShipment()) &&
                                Objects.nonNull(pick.getShipmentLine().getShipment().getStop()) &&
                                Objects.nonNull(pick.getShipmentLine().getShipment().getStop().getTrailerAppointmentId())) {

                            groupKeyList.add(pick.getShipmentLine().getShipment().getStop().getTrailerAppointmentId().toString());
                        }
                        else {

                            groupKeyList.add("****");
                        }

                        break;
                    case BY_WAVE:
                        if (Objects.nonNull(pick.getShipmentLine()) &&
                            Objects.nonNull(pick.getShipmentLine().getWave())) {
                            groupKeyList.add(pick.getShipmentLine().getWave().getNumber());
                        }
                        else {

                            groupKeyList.add("****");
                        }
                        break;
                }

            }
        );

        return groupKeyList.stream().collect(Collectors.joining("#"));
    }

    private boolean validategroupKeyByCartonization(ListPickConfiguration listPickConfiguration, Pick pick) {

        /*
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
        */
        return true;
    }

    @Transactional
    protected PickList createPickList(List<ListPickConfiguration> listPickConfigurations, Pick pick) {

        // Create the pick list based upon the first configuration
        ListPickConfiguration listPickConfiguration = listPickConfigurations.get(0);
        logger.debug("try to create picking list based on the configuration {}", listPickConfiguration);
        String groupKey = getGroupKey(listPickConfiguration, pick);
        logger.debug("create a new list with group key {}",
                groupKey);

        PickList pickList = new PickList();
        pickList.setGroupKey(groupKey);
        pickList.setStatus(PickListStatus.PENDING);
        pickList.setWarehouseId(pick.getWarehouseId());
        pickList.setNumber(getNextPickListNumber(pick.getWarehouseId()));
        pickList = save(pickList);


        pick = pickService.assignPickToList(pick, pickList);
        pickList.getPicks().add(pick);

        return pickList;
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


    @Transactional
    public void cancelPickList(Long id) {
        PickList pickList = findById(id);
        // let's cancel all the picks in this list
        for (Pick pick : pickList.getPicks()) {
            pickService.cancelPick(pick, false, false);
        }

        // if there's already a work task released for this pick list, then
        // remove the work task as well
        if (Objects.nonNull(pickList.getWorkTaskId())) {
            resourceServiceRestemplateClient.cancelWorkTaskById(
                    pickList.getWarehouseId(),
                    pickList.getWorkTaskId()
            );
        }

        delete(pickList);
    }


    public PickList confirmPickList(Long pickId,
                                    Long sourceLocationId,
                                    Long quantity, Long nextLocationId,
                            String nextLocationName,
                            boolean pickToContainer, String containerId,
                            String lpn, String destinationLPN)  {
        PickList pickList = findById(pickId);
        List<Pick> confirmablePicks = pickList.getPicks().stream().filter(
                pick -> pick.getSourceLocationId().equals(sourceLocationId) &&
                        pick.getQuantity() > pick.getPickedQuantity()
        ).collect(Collectors.toList());

        // make sure all the picks in the list are pickable
        for (Pick pick : confirmablePicks) {

            if (Objects.nonNull(pick.getShipmentLine())) {
                Order order = pick.getShipmentLine().getOrderLine().getOrder();

                if (order.getStatus().equals(OrderStatus.COMPLETE)) {
                    throw OrderOperationException.raiseException(
                            "Can't confirm the pick " + pick.getNumber() + " as its order " +
                                    order.getNumber() + " already completed");
                }
                if (order.getStatus().equals(OrderStatus.CANCELLED)) {
                    throw OrderOperationException.raiseException(
                            "Can't confirm the pick " + pick.getNumber() + " as its order " +
                                    order.getNumber() + " already cancelled");
                }
                if (Boolean.TRUE.equals(order.getCancelRequested())) {
                    throw OrderOperationException.raiseException("There's a cancel request on the pick " +
                            pick.getNumber() + "'s Order " + order.getNumber() + ", " +
                            "please cancel it before you want to continue");
                }
            }
        }

        // confirm each pick
        Long totalPickQuantity = quantity;
        Iterator<Pick> pickIterator = confirmablePicks.listIterator();
        while(pickIterator.hasNext() && totalPickQuantity > 0) {
            Pick pick = pickIterator.next();
            // get the pick quantity for the current pick
            long pickQuantity = Math.min(totalPickQuantity, pick.getQuantity() - pick.getPickedQuantity());

            logger.debug("start to confirm pick {} from list {}, with quantity {}",
                    pick.getNumber(), pickList.getNumber(), pickQuantity);
            pickService.confirmPick(pick.getId(), pickQuantity, nextLocationId, nextLocationName,
                    pickToContainer, containerId, lpn, destinationLPN);
            // remove the picked quantity from the total quantity
            totalPickQuantity -= pickQuantity;
        }
        pickList = findById(pickList.getId());

        logger.debug("pick list {}'s work task id {}",
                pickList.getNumber(), pickList.getWorkTaskId());
        pickList.getPicks().forEach(
                pick -> logger.debug("pick {}, pick quantity {}, picked quantity {}",
                        pick.getNumber(),
                        pick.getQuantity(),
                        pick.getPickedQuantity())
        );
        if (Objects.nonNull(pickList.getWorkTaskId()) &&
            pickList.getPicks().stream().allMatch(pick -> pick.getQuantity() <= pick.getPickedQuantity())) {
            logger.debug("All picks in the list {} are completely picked, let's complete the work task",
                    pickList.getNumber());
            resourceServiceRestemplateClient.completeWorkTask(
                    pickList.getWarehouseId(), pickList.getWorkTaskId()
            );
        }

        return pickList;

    }

    public ReportHistory generatePickReportByPickList(Long warehouseId, Long id, String locale) throws JsonProcessingException {
        return generatePickReportByPickList(warehouseId, findById(id), locale);
    }

    public ReportHistory generatePickReportByPickList(Long warehouseId, PickList pickList, String locale)
            throws JsonProcessingException {

        Report reportData = new Report();
        setupBulkPickReportParameters(
                reportData, pickList
        );
        setupBulkPickReportData(
                reportData, pickList
        );

        logger.debug("will call resource service to print the report with locale: {}",
                locale);
        logger.debug("####   Report   Data  ######");
        logger.debug(reportData.toString());
        ReportHistory reportHistory =
                resourceServiceRestemplateClient.generateReport(
                        warehouseId, ReportType.PICK_LIST_SHEET, reportData, locale
                );


        logger.debug("####   Report   printed: {}", reportHistory.getFileName());
        return reportHistory;

    }

    private String getQuantityByUOM(Long quantity, List<Inventory> pickableInventory) {

        logger.debug("getQuantityByUOM by quantity {}", quantity);
        StringBuilder pickQuantityByUOM = new StringBuilder();
        pickQuantityByUOM.append(quantity);

        if (pickableInventory != null && !pickableInventory.isEmpty() &&
                Objects.nonNull(pickableInventory.get(0).getItemPackageType())) {
            // get the information from the first inventory of the list
            // we will assume all the pickable inventory in the same location
            // has the same item UOM information. If the location is mixed with
            // different package type, the warehouse may have some difficulty for picking
            ItemUnitOfMeasure stockItemUnitOfMeasure =
                    pickableInventory.get(0).getItemPackageType().getStockItemUnitOfMeasures();
            ItemUnitOfMeasure caseItemUnitOfMeasure =
                    pickableInventory.get(0).getItemPackageType().getCaseItemUnitOfMeasure();

            if (Objects.nonNull(stockItemUnitOfMeasure) &&
                    Objects.nonNull(stockItemUnitOfMeasure.getUnitOfMeasure())) {

                logger.debug("stockItemUnitOfMeasure: {}", stockItemUnitOfMeasure.getUnitOfMeasure().getName());
                pickQuantityByUOM.append(" ")
                        .append(stockItemUnitOfMeasure.getUnitOfMeasure().getName());
            }
            // if the item package type has case UOM defined, show the quantity in case UOM as well.
            if (Objects.nonNull(caseItemUnitOfMeasure) &&
                    Objects.nonNull(caseItemUnitOfMeasure.getUnitOfMeasure())) {

                Long caseQuantity = quantity / caseItemUnitOfMeasure.getQuantity();
                Long leftOverQuantity = quantity % caseItemUnitOfMeasure.getQuantity();
                if (caseQuantity > 0) {


                    pickQuantityByUOM.append(" (").append(caseQuantity).append(" ")
                            .append(caseItemUnitOfMeasure.getUnitOfMeasure().getName());
                    if (leftOverQuantity > 0) {
                        pickQuantityByUOM.append(", ").append(leftOverQuantity);
                        if (Objects.nonNull(stockItemUnitOfMeasure) &&
                                Objects.nonNull(stockItemUnitOfMeasure.getUnitOfMeasure())) {
                            pickQuantityByUOM.append(" ")
                                    .append(stockItemUnitOfMeasure.getUnitOfMeasure().getName());
                        }
                    }
                    logger.debug("caseItemUnitOfMeasure: {}", caseItemUnitOfMeasure.getUnitOfMeasure().getName());
                    pickQuantityByUOM.append(")");
                }
            }
        }

        return pickQuantityByUOM.toString();
    }
    private void setupBulkPickReportParameters(
            Report report, PickList pickList) {

        report.addParameter("number", pickList.getNumber());

    }

    private void setupBulkPickReportData(Report report, PickList pickList) {

        // set data to be all picks

        // key: item id - inventory status id - source location id -color - product size - style
        Map<String, List<Inventory>> pickableInventoryMap = new HashMap<>();

        // Setup display field
        pickList.getPicks().forEach(
                pick -> {
                    String key = pick.getItemId() + "-" + pick.getInventoryStatusId() + "-" +
                            pick.getSourceLocationId() + "-" + pick.getColor() + "-" +
                            pick.getProductSize() + "-" + pick.getStyle();

                    List<Inventory> pickableInventory =  pickableInventoryMap.getOrDefault(key,
                            inventoryServiceRestemplateClient.getPickableInventory(
                                    pick.getItemId(), pick.getInventoryStatusId(), pick.getSourceLocationId(),
                                    pick.getColor(), pick.getProductSize(), pick.getStyle(), null)
                    );
                    pickableInventoryMap.putIfAbsent(key, pickableInventory);

                    // set the inventory attribute in one string
                    StringBuilder inventoryAttribute = new StringBuilder()
                            .append(Strings.isBlank(pick.getColor()) ? "" : pick.getColor()).append("    ")
                            .append(Strings.isBlank(pick.getProductSize()) ? "" : pick.getProductSize()).append("    ")
                            .append(Strings.isBlank(pick.getStyle()) ? "" : pick.getStyle())
                            .append(Strings.isBlank(pick.getAllocateByReceiptNumber()) ? "" : pick.getAllocateByReceiptNumber());
                    pick.setInventoryAttribute(inventoryAttribute.toString());

                    pick.setQuantityByUOM(getQuantityByUOM(pick.getQuantity(), pickableInventory));
                }
        );


        report.setData(pickList.getPicks());
    }

    public List<ReportHistory> generatePickListReportInBatch(Long warehouseId, String ids, String locale) throws JsonProcessingException {
        List<ReportHistory> reportHistories = new ArrayList<>();

        for (String id : (ids.split(","))) {
            reportHistories.add(
                    generatePickReportByPickList(warehouseId, Long.parseLong(id), locale)
            );
        }
        return reportHistories;
    }

    public PickList acknowledgePickList(Long warehouseId, Long id) {
        PickList pickList = findById(id);
        String currentUserName = userService.getCurrentUserName();
        if (Strings.isNotBlank(pickList.getAcknowledgedUsername()) &&
            !pickList.getAcknowledgedUsername().equalsIgnoreCase(currentUserName)) {
            throw PickingException.raiseException("pick list " + pickList.getNumber() +
                    " is already acknowledged by " + pickList.getAcknowledgedUsername());
        }
        pickList.setAcknowledgedUsername(currentUserName);

        return saveOrUpdate(pickList);

    }
    public PickList unacknowledgePickList(Long warehouseId, Long id) {
        PickList pickList = findById(id);
        pickList.setAcknowledgedUsername(null);

        return saveOrUpdate(pickList);

    }
}
