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

import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import com.garyzhangscm.cwms.outbound.clients.CommonServiceRestemplateClient;
import com.garyzhangscm.cwms.outbound.clients.InventoryServiceRestemplateClient;
import com.garyzhangscm.cwms.outbound.clients.WarehouseLayoutServiceRestemplateClient;
import com.garyzhangscm.cwms.outbound.exception.GenericException;
import com.garyzhangscm.cwms.outbound.exception.ResourceNotFoundException;
import com.garyzhangscm.cwms.outbound.model.*;
import com.garyzhangscm.cwms.outbound.repository.AllocationConfigurationRepository;
import com.garyzhangscm.cwms.outbound.repository.PickConfirmStrategyRepository;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.transaction.Transactional;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;


@Service
public class PickConfirmStrategyService implements TestDataInitiableService {
    private static final Logger logger = LoggerFactory.getLogger(PickConfirmStrategyService.class);

    @Autowired
    private PickConfirmStrategyRepository pickConfirmStrategyRepository;

    @Autowired
    private CommonServiceRestemplateClient commonServiceRestemplateClient;
    @Autowired
    private WarehouseLayoutServiceRestemplateClient warehouseLayoutServiceRestemplateClient;
    @Autowired
    private InventoryServiceRestemplateClient inventoryServiceRestemplateClient;
    @Autowired
    private FileService fileService;

    @Value("${fileupload.test-data.pick-confirm-strategy:pick-confirm-strategy}")
    String testDataFile;

    public PickConfirmStrategy findById(Long id, boolean loadDetails) {
        PickConfirmStrategy pickConfirmStrategy
                = pickConfirmStrategyRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.raiseException("Pick confirm strategy not found by id: " + id));

        if (loadDetails) {
            loadAttribute(pickConfirmStrategy);
        }
        return pickConfirmStrategy;
    }

    public PickConfirmStrategy findById(Long id) {
        return findById(id, true);
    }


    public List<PickConfirmStrategy> findAll(Long warehouseId,
                                                 Long itemId,
                                                 Long itemFamilyId,
                                                 Long locationId,
                                                 Long locationGroupId,
                                                 Long locationGroupTypeId,
                                                 boolean loadDetails) {

        List<PickConfirmStrategy> pickConfirmStrategies =  pickConfirmStrategyRepository.findAll(
                (Root<PickConfirmStrategy> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) -> {
                    List<Predicate> predicates = new ArrayList<Predicate>();

                    predicates.add(criteriaBuilder.equal(root.get("warehouseId"), warehouseId));
                    logger.debug("Add criteria warehouseId: {}", warehouseId);

                    if (Objects.nonNull(itemId)) {

                        predicates.add(criteriaBuilder.equal(root.get("itemId"), itemId));
                        logger.debug("Add criteria itemId: {}", itemId);
                    }
                    if (Objects.nonNull(itemFamilyId)) {

                        predicates.add(criteriaBuilder.equal(root.get("itemFamilyId"), itemFamilyId));
                        logger.debug("Add criteria itemFamilyId: {}", itemFamilyId);
                    }
                    if (Objects.nonNull(locationId)) {

                        predicates.add(criteriaBuilder.equal(root.get("locationId"), locationId));
                        logger.debug("Add criteria locationId: {}", locationId);
                    }
                    if (Objects.nonNull(locationGroupId)) {

                        predicates.add(criteriaBuilder.equal(root.get("locationGroupId"), locationGroupId));
                        logger.debug("Add criteria locationGroupId: {}", locationGroupId);
                    }
                    if (Objects.nonNull(locationGroupTypeId)) {

                        predicates.add(criteriaBuilder.equal(root.get("locationGroupTypeId"), locationGroupTypeId));
                        logger.debug("Add criteria locationGroupTypeId: {}", locationGroupTypeId);
                    }

                    Predicate[] p = new Predicate[predicates.size()];
                    return criteriaBuilder.and(predicates.toArray(p));
                }
        );

        logger.debug("We got {} result of pickConfirmStrategies", pickConfirmStrategies.size());

        if (pickConfirmStrategies.size() > 0 && loadDetails) {
            loadAttribute(pickConfirmStrategies);
        }
        return pickConfirmStrategies;
    }

    public List<PickConfirmStrategy> findAll(Long warehouseId,
                                                 Long itemId,
                                                 Long itemFamilyId,
                                                 Long locationId,
                                                 Long locationGroupId,
                                                 Long locationGroupTypeId) {
        return findAll(warehouseId,  itemId,
                  itemFamilyId,  locationId,
                  locationGroupId, locationGroupTypeId,  true);
    }


    public PickConfirmStrategy findBySequence(Long warehouseId, int sequence) {
        return findBySequence(warehouseId, sequence, true);
    }


    public PickConfirmStrategy findBySequence(
            Long warehouseId,int sequence, boolean loadDetails) {
        PickConfirmStrategy pickConfirmStrategy
                = pickConfirmStrategyRepository.findByWarehouseIdAndSequence(
                        warehouseId, sequence);
        if (pickConfirmStrategy != null && loadDetails) {
            loadAttribute(pickConfirmStrategy);
        }
        return pickConfirmStrategy;
    }


    private void loadAttribute(PickConfirmStrategy pickConfirmStrategy) {


        if (pickConfirmStrategy.getItemId() != null &&
                pickConfirmStrategy.getItem() == null) {
            try {
                pickConfirmStrategy.setItem(
                        inventoryServiceRestemplateClient.getItemById(
                                pickConfirmStrategy.getItemId()));
            }
            catch (Exception ex) {}
        }
        if (pickConfirmStrategy.getItemFamilyId() != null &&
                pickConfirmStrategy.getItemFamily() == null) {
            try {
                pickConfirmStrategy.setItemFamily(
                        inventoryServiceRestemplateClient.getItemFamilyById(
                                pickConfirmStrategy.getItemFamilyId()));
            }
            catch (Exception ex) {}
        }
        if (pickConfirmStrategy.getLocationId() != null
                && pickConfirmStrategy.getLocation() == null) {
            try {
                pickConfirmStrategy.setLocation(
                        warehouseLayoutServiceRestemplateClient.getLocationById(
                                pickConfirmStrategy.getLocationId()));
            }
            catch (Exception ex) {}
        }
        if (pickConfirmStrategy.getLocationGroupId() != null &&
                pickConfirmStrategy.getLocationGroup() == null) {
            try {
                pickConfirmStrategy.setLocationGroup(
                        warehouseLayoutServiceRestemplateClient.getLocationGroupById(
                                pickConfirmStrategy.getLocationGroupId()));
            }
            catch (Exception ex) {}
        }
        if (pickConfirmStrategy.getLocationGroupTypeId() != null &&
                pickConfirmStrategy.getLocationGroupType() == null) {
            try {
                pickConfirmStrategy.setLocationGroupType(
                        warehouseLayoutServiceRestemplateClient.getLocationGroupTypeById(
                                pickConfirmStrategy.getLocationGroupTypeId()));
            }
            catch (Exception ex) {}
        }


    }

    private void loadAttribute(List<PickConfirmStrategy> pickConfirmStrategies) {
        pickConfirmStrategies.forEach(
                pickConfirmStrategie -> loadAttribute(pickConfirmStrategie));
    }

    public PickConfirmStrategy save(PickConfirmStrategy pickConfirmStrategy) {
        PickConfirmStrategy newPickConfirmStrategy =
                pickConfirmStrategyRepository.save(pickConfirmStrategy);
        loadAttribute(newPickConfirmStrategy);
        return newPickConfirmStrategy;
    }

    public PickConfirmStrategy saveOrUpdate(PickConfirmStrategy pickConfirmStrategy) {
        if (pickConfirmStrategy.getId() == null &&
                findBySequence(
                        pickConfirmStrategy.getWarehouseId(),
                        pickConfirmStrategy.getSequence()) != null) {
            pickConfirmStrategy.setId(
                    findBySequence(
                            pickConfirmStrategy.getWarehouseId(),
                            pickConfirmStrategy.getSequence()).getId());
        }
        return save(pickConfirmStrategy);
    }


    public void delete(PickConfirmStrategy pickConfirmStrategy) {
        pickConfirmStrategyRepository.delete(pickConfirmStrategy);
    }

    public void delete(Long id) {

        pickConfirmStrategyRepository.deleteById(id);
    }

    public void delete(String pickConfirmStrategyIds) {
        if (!pickConfirmStrategyIds.isEmpty()) {
            long[] pickConfirmStrategyIdArray = Arrays.asList(
                    pickConfirmStrategyIds.split(","))
                    .stream()
                    .mapToLong(Long::parseLong)
                    .toArray();
            for (long id : pickConfirmStrategyIdArray) {
                delete(id);
            }
        }
    }

    public List<PickConfirmStrategyCSVWrapper> loadData(InputStream inputStream)
            throws IOException {

        CsvSchema schema = CsvSchema.builder().
                addColumn("company").
                addColumn("warehouse").
                addColumn("sequence").
                addColumn("item").
                addColumn("itemFamily").
                addColumn("location").
                addColumn("locationGroup").
                addColumn("locationGroupType").
                addColumn("unitOfMeasure").
                addColumn("confirmItemFlag").
                addColumn("confirmLocationFlag").
                addColumn("confirmLocationCodeFlag").
                build().withHeader();

        return fileService.loadData(
                inputStream, schema, PickConfirmStrategyCSVWrapper.class);
    }

    public void initTestData(Long companyId, String warehouseName) {
        try {

            String companyCode =
                    warehouseLayoutServiceRestemplateClient.getCompanyById(
                            companyId).getCode();

            String testDataFileName = StringUtils.isBlank(warehouseName) ?
                    testDataFile + ".csv" :
                    testDataFile + "-" + companyCode + "-" + warehouseName + ".csv";

            InputStream inputStream = new ClassPathResource(testDataFileName).getInputStream();
            List<PickConfirmStrategyCSVWrapper> pickConfirmStrategyCSVWrappers
                    = loadData(inputStream);
            pickConfirmStrategyCSVWrappers.stream()
                    .forEach(pickConfirmStrategyCSVWrapper
                            -> saveFromWrapper(pickConfirmStrategyCSVWrapper));
        } catch (IOException ex) {
            logger.debug("Exception while load test data: {}", ex.getMessage());
        }
    }

    private void saveFromWrapper(
            PickConfirmStrategyCSVWrapper pickConfirmStrategyCSVWrapper) {

        logger.debug("Start to save pick confirm strategy with meta value:\n {}",
                pickConfirmStrategyCSVWrapper);
        PickConfirmStrategy pickConfirmStrategy = new PickConfirmStrategy();
        BeanUtils.copyProperties(pickConfirmStrategyCSVWrapper, pickConfirmStrategy);


        Warehouse warehouse =
                warehouseLayoutServiceRestemplateClient.getWarehouseByName(
                        pickConfirmStrategyCSVWrapper.getCompany(),
                        pickConfirmStrategyCSVWrapper.getWarehouse());

        pickConfirmStrategy.setWarehouseId(warehouse.getId());

        Client client = null;
        if (Strings.isNotBlank(pickConfirmStrategyCSVWrapper.getClient())) {
            client = commonServiceRestemplateClient.getClientByName(warehouse.getId(),
                    pickConfirmStrategyCSVWrapper.getClient());
        }
        if (!StringUtils.isBlank(pickConfirmStrategyCSVWrapper.getItem())) {
            Item item = inventoryServiceRestemplateClient.getItemByName(
                    warehouse.getId(),
                    Objects.isNull(client) ? null : client.getId(),
                    pickConfirmStrategyCSVWrapper.getItem());
            if (item != null) {
                pickConfirmStrategy.setItemId(item.getId());
            }
        }
        if (!StringUtils.isBlank(pickConfirmStrategyCSVWrapper.getItemFamily())) {
            ItemFamily itemFamily = inventoryServiceRestemplateClient.getItemFamilyByName(
                    warehouse.getId(), pickConfirmStrategyCSVWrapper.getItemFamily());
            if (itemFamily != null) {
                pickConfirmStrategy.setItemFamilyId(itemFamily.getId());
            }
        }


        if (!StringUtils.isBlank(pickConfirmStrategyCSVWrapper.getLocation())) {
            Location location =
                    warehouseLayoutServiceRestemplateClient.getLocationByName(
                            pickConfirmStrategyCSVWrapper.getCompany(),
                            pickConfirmStrategyCSVWrapper.getWarehouse(),
                            pickConfirmStrategyCSVWrapper.getLocation());
            if (location != null) {
                pickConfirmStrategy.setLocationId(location.getId());
            }
        }

        if (!StringUtils.isBlank(pickConfirmStrategyCSVWrapper.getLocationGroup())) {
            LocationGroup locationGroup =
                    warehouseLayoutServiceRestemplateClient.getLocationGroupByName(
                            pickConfirmStrategyCSVWrapper.getCompany(),
                            pickConfirmStrategyCSVWrapper.getWarehouse(),
                            pickConfirmStrategyCSVWrapper.getLocationGroup());
            if (locationGroup != null) {
                pickConfirmStrategy.setLocationGroupId(locationGroup.getId());
            }
        }

        if (!StringUtils.isBlank(pickConfirmStrategyCSVWrapper.getLocationGroupType())) {
            LocationGroupType locationGroupType =
                    warehouseLayoutServiceRestemplateClient.getLocationGroupTypeByName(
                            pickConfirmStrategyCSVWrapper.getLocationGroupType());
            if (locationGroupType != null) {
                pickConfirmStrategy.setLocationGroupTypeId(locationGroupType.getId());
            }
        }

        if (!StringUtils.isBlank(pickConfirmStrategyCSVWrapper.getUnitOfMeasure())) {
            UnitOfMeasure unitOfMeasure =
                    commonServiceRestemplateClient.getUnitOfMeasureByName(
                        warehouse.getId(),
                            pickConfirmStrategyCSVWrapper.getUnitOfMeasure());

            if (unitOfMeasure != null) {
                pickConfirmStrategy.setUnitOfMeasureId(unitOfMeasure.getId());
            }
        }

        // Save the configuration first, then save all the pickable unit of measure
        saveOrUpdate(pickConfirmStrategy);
    }


    public List<PickConfirmStrategy> getMatchedPickConfirmStrategies(
            Pick pick) {
        logger.debug("Start to find matched pick confirm strategy for pick {}",
                pick.getNumber());
        List<PickConfirmStrategy> pickConfirmStrategies =
                findAll(pick.getWarehouseId(), null, null, null, null,
                        null, false);
        logger.debug("we have {} strategies configured, for warehouse {}",
                pickConfirmStrategies.size(), pick.getWarehouseId());

        return pickConfirmStrategies.stream().filter(pickConfirmStrategy ->
                isMatch(pickConfirmStrategy, pick)).collect(Collectors.toList());
    }
    public PickConfirmStrategy getMatchedPickConfirmStrategy(
            Pick pick) {
        List<PickConfirmStrategy> matchedPickConfirmStrategies
                = getMatchedPickConfirmStrategies(pick);
        if (matchedPickConfirmStrategies.size() == 0) {
            // no matched pick confirm strategy
            return null;
        }
        else {
            Collections.sort(
                    matchedPickConfirmStrategies,
                    Comparator.comparing(PickConfirmStrategy::getSequence)
            );
            // return the first matched strategy based on the sequence
            return matchedPickConfirmStrategies.get(0);
        }
    }

    /**
     * Check if the pick confirm strategy matches with the pick
     * @param pickConfirmStrategy  pick confirm strategy
     * @param pick pick
     * @return true for match. false for not match
     */
    public boolean isMatch(PickConfirmStrategy pickConfirmStrategy, Pick pick) {
        // we will compare each field that setup in the pick confirm strategy
        // against the pick.
        // If the field is not setup in the strategy, then it matches with
        // any pick
        logger.debug("Start to compare picks {} against pick confirm strategy {}",
                pick.getNumber(), pickConfirmStrategy.getId());
        if (Objects.nonNull(pickConfirmStrategy.getItemId()) &&
                !pickConfirmStrategy.getItemId().equals(pick.getItemId())) {
            logger.debug("> strategy's item id is setup to {}, doesn't match with pick's item id {}",
                    pickConfirmStrategy.getItemId(),
                    pick.getItemId());
            return false;
        }


        if (Objects.nonNull(pickConfirmStrategy.getWarehouseId()) &&
                !pickConfirmStrategy.getWarehouseId().equals(pick.getWarehouseId())) {
            logger.debug("> strategy's warehouse  id is setup to {}, doesn't match with pick's warehouse id {}",
                    pickConfirmStrategy.getWarehouseId(),
                    pick.getWarehouseId());
            return false;
        }


        if (Objects.nonNull(pickConfirmStrategy.getItemFamilyId()) &&
                !pickConfirmStrategy.getItemFamilyId().equals(
                        pick.getItem().getItemFamily().getId())) {
            logger.debug("> strategy's item family  id is setup to {}, doesn't match with pick's item family id {}",
                    pickConfirmStrategy.getItemFamilyId(),
                    pick.getItem().getItemFamily().getId());
            return false;
        }


        if (Objects.nonNull(pickConfirmStrategy.getLocationId()) &&
                !pickConfirmStrategy.getLocationId().equals(
                        pick.getSourceLocationId())) {
            logger.debug("> strategy's location  id is setup to {}, doesn't match with pick's location id {}",
                    pickConfirmStrategy.getLocationId(),
                    pick.getSourceLocationId());
            return false;
        }
        if (Objects.nonNull(pickConfirmStrategy.getLocationGroupId()) &&
                !pickConfirmStrategy.getLocationGroupId().equals(
                        pick.getSourceLocation().getLocationGroup().getId())) {
            logger.debug("> strategy's location group id is setup to {}, doesn't match with pick's location group id {}",
                    pickConfirmStrategy.getLocationGroupId(),
                    pick.getSourceLocation().getLocationGroup().getId());
            return false;
        }

        if (Objects.nonNull(pickConfirmStrategy.getLocationGroupTypeId()) &&
                !pickConfirmStrategy.getLocationGroupTypeId().equals(
                        pick.getSourceLocation().getLocationGroup().getLocationGroupType().getId())) {
            logger.debug("> strategy's location group type id is setup to {}, doesn't match with pick's location group type id {}",
                    pickConfirmStrategy.getLocationGroupTypeId(),
                    pick.getSourceLocation().getLocationGroup().getLocationGroupType().getId());
            return false;
        }
        if (Objects.nonNull(pickConfirmStrategy.getUnitOfMeasureId()) &&
                !pickConfirmStrategy.getUnitOfMeasureId().equals(
                        pick.getUnitOfMeasureId())) {
            logger.debug("> strategy's location unit of measure id is setup to {}, doesn't match with pick's unit of measure id {}",
                    pickConfirmStrategy.getUnitOfMeasureId(),
                    pick.getUnitOfMeasureId());
            return false;
        }

        logger.debug(">>> We found match!");
        // we passed all the validation and for all the fields that
        // setup in the strategy, the pick matches all of them
        return  true;
    }


    public void handleItemOverride(Long warehouseId, Long oldItemId, Long newItemId) {
        logger.debug("start to process item override for order line, current warehouse {}, from item id {} to item id {}",
                warehouseId, oldItemId, newItemId);
        pickConfirmStrategyRepository.processItemOverride(oldItemId, newItemId, warehouseId);
    }
}
