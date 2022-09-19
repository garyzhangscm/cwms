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

import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import com.garyzhangscm.cwms.workorder.clients.CommonServiceRestemplateClient;
import com.garyzhangscm.cwms.workorder.clients.InventoryServiceRestemplateClient;
import com.garyzhangscm.cwms.workorder.clients.WarehouseLayoutServiceRestemplateClient;
import com.garyzhangscm.cwms.workorder.exception.ProductionLineException;
import com.garyzhangscm.cwms.workorder.exception.ResourceNotFoundException;
import com.garyzhangscm.cwms.workorder.exception.WorkOrderException;
import com.garyzhangscm.cwms.workorder.model.*;
import com.garyzhangscm.cwms.workorder.repository.ProductionLineRepository;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;


@Service
public class ProductionLineService implements TestDataInitiableService {
    private static final Logger logger = LoggerFactory.getLogger(ProductionLineService.class);

    @Autowired
    private ProductionLineRepository productionLineRepository;
    @Autowired
    private WorkOrderService workOrderService;
    @Autowired
    private ProductionLineActivityService productionLineActivityService;

    @Autowired
    private ProductionLineMonitorTransactionService productionLineMonitorTransactionService;

    @Autowired
    private WarehouseLayoutServiceRestemplateClient warehouseLayoutServiceRestemplateClient;
    @Autowired
    private InventoryServiceRestemplateClient inventoryServiceRestemplateClient;
    @Autowired
    private CommonServiceRestemplateClient commonServiceRestemplateClient;
    @Autowired
    private FileService fileService;

    @Value("${fileupload.test-data.production-lines:production-lines}")
    String testDataFile;

    // the default max cycle time for all production is 120 second.
    // if the last cycle happens 120 seconds ago, then the production will
    // be defined as inactive
    private Double DEFAULT_MAX_CYCLE_TIME = 120.0;
    public ProductionLine findById(Long id, boolean loadDetails) {
        ProductionLine productionLine = productionLineRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.raiseException("production line not found by id: " + id));
        if (loadDetails) {
            loadAttribute(productionLine);
        }
        return productionLine;
    }

    public ProductionLine findById(Long id) {
        return findById(id, true);
    }


    public List<ProductionLine> findAll(Long warehouseId, String name, String productionLineIds, boolean genericMatch,
                                        boolean loadDetails) {
        List<ProductionLine> productionLines
                =  productionLineRepository.findAll(
                (Root<ProductionLine> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) -> {
                    List<Predicate> predicates = new ArrayList<Predicate>();

                    predicates.add(criteriaBuilder.equal(root.get("warehouseId"), warehouseId));

                    if (!StringUtils.isBlank(name)) {
                        if (genericMatch) {

                            predicates.add(criteriaBuilder.like(root.get("name"), name));
                        }
                        else {

                            predicates.add(criteriaBuilder.equal(root.get("name"), name));
                        }

                    }
                    if (!StringUtils.isBlank(productionLineIds)) {
                        CriteriaBuilder.In<Long> inProductionLineIds = criteriaBuilder.in(root.get("id"));
                        for(String id : productionLineIds.split(",")) {
                            inProductionLineIds.value(Long.parseLong(id));
                        }
                        predicates.add(criteriaBuilder.and(inProductionLineIds));
                    }
                    Predicate[] p = new Predicate[predicates.size()];
                    return criteriaBuilder.and(predicates.toArray(p));
                }
                ,
                Sort.by(Sort.Direction.ASC, "name")
        );


        if (productionLines.size() > 0 && loadDetails) {
            loadAttribute(productionLines);
        }
        return productionLines;
    }

    public List<ProductionLine> findAll(Long warehouseId,String name, String productionLineIds, boolean genericMatch) {
        return findAll(warehouseId, name, productionLineIds, genericMatch, true);
    }

    public List<ProductionLine> findAllAvailableProductionLines(
            Long warehouseId, Long itemId) {
        return findAllAvailableProductionLines(warehouseId, itemId,true);
    }

    public List<ProductionLine> findAllAvailableProductionLinesForMPS(
            Long warehouseId, Long itemId) {
        return findAllAvailableProductionLinesForMPS(warehouseId, itemId,true);
    }

    public List<ProductionLine> findAllAvailableProductionLines(
            Long warehouseId, Long itemId, boolean loadDetails) {
        List<ProductionLine> productionLines
                = productionLineRepository.findByWarehouseId(warehouseId);
        productionLines =
                productionLines.stream()
                        .filter(productionLine ->
                                isAvailableForNewWorkOrder(productionLine, itemId))
                            .collect(Collectors.toList());
        if (productionLines.size() > 0 && loadDetails) {
            loadAttribute(productionLines);
        }
        return productionLines;

    }


    public List<ProductionLine> findAllAvailableProductionLinesForMPS(
            Long warehouseId, Long itemId, boolean loadDetails) {
        List<ProductionLine> productionLines
                = productionLineRepository.findByWarehouseId(warehouseId);
        productionLines =
                productionLines.stream()
                        .filter(productionLine ->
                                isAvailableForFutureWorkOrder(productionLine, itemId))
                        .collect(Collectors.toList());
        if (productionLines.size() > 0 && loadDetails) {
            loadAttribute(productionLines);
        }
        return productionLines;

    }

    /**
     * Check if the production is valid to be assigned to the work order with the
     * specific item
     * @param productionLine
     * @param itemId
     * @return
     */
    private boolean isAvailableForNewWorkOrder(
            ProductionLine productionLine, Long itemId) {
        logger.debug("Start to check if production line {} can be used by itemId: {}",
                productionLine.getName(),
                itemId);
        // The production line is available for new work order only if
        // both of the following conditions are met
        // 1. production line is enabled
        // 2. production line is not exclusive
        //    or is exclusive but no work order on it yet
        if (!productionLine.getEnabled()) {
            logger.debug("> production line is disabled");
            return false;
        }
        if (productionLine.getWorkOrderExclusiveFlag() &&
            productionLine.getProductionLineAssignments().size() > 0) {

            logger.debug("> production line is assigned and setup as exclusive");
            return false;
        }
        // if this production line is not for generic purpose, then we
        // know it is for certain item only , we will need to make sure
        // the item id is in the list
        if (!productionLine.getGenericPurpose()) {
            if (Objects.isNull(itemId)) {
                // the production line is for certain item only but
                // we didn't pass in the item as qualifier, let's return false

                logger.debug("> production line is not generic purpose but item id is not passed in");
                return false;
            }
            if (productionLine.getProductionLineCapacities().stream()
                    .noneMatch(productionLineCapacity
                            -> itemId.equals(productionLineCapacity.getItemId()))) {
                logger.debug("> production line is not generic purpose but item id is not in the list");

                return false;
            }
        }
        logger.debug("GOOD, we found production line {} can be used by itemId: {}",
                productionLine.getName(),
                itemId);

        return true;
    }

    /**
     * Check if the production line is valid for future work order, normally
     * used when we setup the MPS for the production line
     * @param productionLine
     * @param itemId
     * @return
     */
    private boolean isAvailableForFutureWorkOrder(
            ProductionLine productionLine, Long itemId) {
        logger.debug("Start to check if production line {} can be used by itemId: {} for future MPS",
                productionLine.getName(),
                itemId);
        // The production line is available for new work order only if
        // both of the following conditions are met
        // 1. production line is enabled
        // 2. production line is not exclusive
        //    or is exclusive but no work order on it yet
        if (!productionLine.getEnabled()) {
            logger.debug("> production line is disabled");
            return false;
        }

        // if this production line is not for generic purpose, then we
        // know it is for certain item only , we will need to make sure
        // the item id is in the list
        if (!productionLine.getGenericPurpose()) {
            if (Objects.isNull(itemId)) {
                // the production line is for certain item only but
                // we didn't pass in the item as qualifier, let's return false

                logger.debug("> production line is not generic purpose but item id is not passed in");
                return false;
            }
            if (productionLine.getProductionLineCapacities().stream()
                    .noneMatch(productionLineCapacity
                            -> itemId.equals(productionLineCapacity.getItemId()))) {
                logger.debug("> production line is not generic purpose but item id is not in the list");

                return false;
            }
        }
        return true;
    }

    public ProductionLine findByName(Long warehouseId, String name, boolean loadDetails) {
        ProductionLine productionLine = productionLineRepository.findByWarehouseIdAndName(warehouseId, name);
        if (productionLine != null && loadDetails) {
            loadAttribute(productionLine);
        }
        return productionLine;
    }

    public ProductionLine findByName(Long warehouseId, String name) {
        return findByName(warehouseId, name, true);
    }



    public List<ProductionLine> findByIds(Long warehouseId, String productionLineIds, boolean loadDetails) {

        List<Long> productionLineIdList = Arrays.stream(productionLineIds.split(","))
                .mapToLong(Long::parseLong).boxed().collect(Collectors.toList());

        List<ProductionLine> productionLines = productionLineRepository.findByIds(warehouseId, productionLineIdList);
        if (productionLines.size() > 0 && loadDetails) {
            loadAttribute(productionLines);
        }
        return productionLines;
    }

    public List<ProductionLine> findByIds(Long warehouseId, String productionLineIds) {
        return findByIds(warehouseId, productionLineIds, true);
    }

    public void loadAttribute(List<ProductionLine> productionLines) {
        for (ProductionLine productionLine : productionLines) {
            loadAttribute(productionLine);
        }
    }

    public void loadAttribute(ProductionLine productionLine) {

        if (productionLine.getWarehouseId() != null && productionLine.getWarehouse() == null) {
            productionLine.setWarehouse(
                    warehouseLayoutServiceRestemplateClient.getWarehouseById(productionLine.getWarehouseId()));
        }

        if (productionLine.getInboundStageLocationId() != null && productionLine.getInboundStageLocation() == null) {
            productionLine.setInboundStageLocation(
                    warehouseLayoutServiceRestemplateClient.getLocationById(productionLine.getInboundStageLocationId()));
        }
        if (productionLine.getOutboundStageLocationId() != null && productionLine.getOutboundStageLocation() == null) {
            productionLine.setOutboundStageLocation(
                    warehouseLayoutServiceRestemplateClient.getLocationById(productionLine.getOutboundStageLocationId()));
        }
        if (productionLine.getProductionLineLocationId() != null && productionLine.getProductionLineLocation() == null) {
            productionLine.setProductionLineLocation(
                    warehouseLayoutServiceRestemplateClient.getLocationById(productionLine.getProductionLineLocationId()));
        }

        productionLine.getProductionLineCapacities().forEach(
                productionLineCapacity -> {
                    // setup the item for the production line capacity
                    productionLineCapacity.setItem(
                            inventoryServiceRestemplateClient.getItemById(
                                    productionLineCapacity.getItemId()
                            )
                    );
                    logger.debug("Will set the UOM of production to {} by id {}",

                            commonServiceRestemplateClient.getUnitOfMeasureById(
                                    productionLineCapacity.getUnitOfMeasureId()
                            ).getName(),
                            productionLineCapacity.getUnitOfMeasureId());
                    productionLineCapacity.setUnitOfMeasure(
                            commonServiceRestemplateClient.getUnitOfMeasureById(
                                    productionLineCapacity.getUnitOfMeasureId()
                            )

                    );
                }
        );


        // productionLine.getWorkOrders().forEach(workOrder -> workOrderService.loadAttribute(workOrder));

    }



    public ProductionLine save(ProductionLine productionLine) {
        ProductionLine newProductionLine = productionLineRepository.save(productionLine);
        loadAttribute(newProductionLine);
        return newProductionLine;
    }

    public ProductionLine saveOrUpdate(ProductionLine productionLine) {
        Long warehouseId = productionLine.getWarehouseId();
        String name = productionLine.getName();

        if (productionLine.getId() == null &&
                findByName(warehouseId, name) != null) {
            productionLine.setId(findByName(warehouseId, name).getId());
        }
        return save(productionLine);
    }


    public void delete(ProductionLine productionLine) {
        productionLineRepository.delete(productionLine);
    }

    public void delete(Long id) {
        productionLineRepository.deleteById(id);
    }

    public void delete(String productionLineIds) {
        if (!productionLineIds.isEmpty()) {
            long[] productionLineIdArray = Arrays.asList(productionLineIds.split(",")).stream().mapToLong(Long::parseLong).toArray();
            for (long id : productionLineIdArray) {
                delete(id);
            }
        }
    }

    public List<ProductionLineCSVWrapper> loadData(InputStream inputStream) throws IOException {

        CsvSchema schema = CsvSchema.builder().
                addColumn("company").
                addColumn("warehouse").
                addColumn("name").
                addColumn("inboundStageLocation").
                addColumn("outboundStageLocation").
                addColumn("productionLineLocation").
                addColumn("workOrderExclusiveFlag").
                addColumn("enabled").
                addColumn("genericPurpose").
                build().withHeader();

        return fileService.loadData(inputStream, schema, ProductionLineCSVWrapper.class);
    }

    public void initTestData(Long companyId, String warehouseName) {
        try {
            logger.debug("####### Start loading production line data");
            String companyCode = warehouseLayoutServiceRestemplateClient.getCompanyById(companyId).getCode();

            String testDataFileName = StringUtils.isBlank(warehouseName) ?
                    testDataFile + ".csv" :
                    testDataFile + "-" + companyCode + "-" + warehouseName + ".csv";
            logger.debug("testDataFileName: {}", testDataFileName);

            InputStream inputStream = new ClassPathResource(testDataFileName).getInputStream();
            List<ProductionLineCSVWrapper> productionLineCSVWrappers = loadData(inputStream);
            productionLineCSVWrappers.stream().forEach(productionLineCSVWrapper -> saveOrUpdate(convertFromWrapper(productionLineCSVWrapper)));
        } catch (IOException ex) {
            logger.debug("Exception while load test data: {}", ex.getMessage());
        }
    }

    private ProductionLine convertFromWrapper(ProductionLineCSVWrapper productionLineCSVWrapper) {

        ProductionLine productionLine = new ProductionLine();
        productionLine.setName(productionLineCSVWrapper.getName());
        productionLine.setWorkOrderExclusiveFlag(productionLineCSVWrapper.getWorkOrderExclusiveFlag());
        productionLine.setEnabled(productionLineCSVWrapper.getEnabled());
        productionLine.setGenericPurpose(productionLineCSVWrapper.getGenericPurpose());

        Warehouse warehouse = warehouseLayoutServiceRestemplateClient.getWarehouseByName(
                productionLineCSVWrapper.getCompany(),
                productionLineCSVWrapper.getWarehouse()
        );
        productionLine.setWarehouseId(warehouse.getId());

        productionLine.setInboundStageLocationId(
                warehouseLayoutServiceRestemplateClient.getLocationByName(
                        warehouse.getId(), productionLineCSVWrapper.getInboundStageLocation()
                ).getId()
        );
        productionLine.setOutboundStageLocationId(
                warehouseLayoutServiceRestemplateClient.getLocationByName(
                        warehouse.getId(), productionLineCSVWrapper.getOutboundStageLocation()
                ).getId()
        );
        productionLine.setProductionLineLocationId(
                warehouseLayoutServiceRestemplateClient.getLocationByName(
                        warehouse.getId(), productionLineCSVWrapper.getProductionLineLocation()
                ).getId()
        );

        logger.debug(">> will load production line: {}", productionLine);
        return productionLine;
    }


    public ProductionLine disableProductionLine(@PathVariable Long id,
                                                @RequestParam boolean disabled) {
        ProductionLine productionLine = findById(id);
        productionLine.setEnabled(!disabled);
        return saveOrUpdate(productionLine);
    }

    public ProductionLine addProductionLine(ProductionLine productionLine) {
        productionLine.getProductionLineCapacities().forEach(
                productionLineCapacity ->
                        productionLineCapacity.setProductionLine(
                                productionLine
                        )
        );
        return saveOrUpdate(productionLine);
    }

    public ProductionLine changeProductionLine(ProductionLine productionLine) {
        productionLine.getProductionLineCapacities().forEach(
                productionLineCapacity ->
                        productionLineCapacity.setProductionLine(
                                productionLine
                        )
        );
        return saveOrUpdate(productionLine);
    }

    public ProductionLineActivity getCheckedInUser(ProductionLine productionLine) {

        // get the latest check in and check out activity.
        // if we have someone checked in but not checked out yet, then
        // return this user
        ProductionLineActivity lastCheckInActivity =
                productionLineActivityService.getLastCheckInActivity(productionLine);
        logger.debug("the last check IN activity on production line: {}",
                productionLine.getName());

        if (Objects.isNull(lastCheckInActivity)) {
            logger.debug(">> No check IN activity found on this production line");
        }
        else {

            logger.debug("user: {}, transaction time: {}",
                    lastCheckInActivity.getUsername(), lastCheckInActivity.getTransactionTime());
        }
        ProductionLineActivity lastCheckOutActivity =
                productionLineActivityService.getLastCheckOutActivity(productionLine);
        logger.debug("the last check OUT activity on production line: {}",
                productionLine.getName());

        if (Objects.isNull(lastCheckOutActivity)) {
            logger.debug(">> No check OUT activity found on this production line");
        }
        else {

            logger.debug("user: {}, transaction time: {}",
                    lastCheckOutActivity.getUsername(), lastCheckOutActivity.getTransactionTime());
        }


        if (Objects.isNull(lastCheckInActivity)) {
            // OK, no one ever checked in
            logger.debug(">> No one is checked in yet");
            return null;
        }
        else if (Objects.isNull(lastCheckOutActivity)){
            // OK, someone checked in but no one ever checked out
            logger.debug(">> Someone checked in but no one is check out");

            return lastCheckInActivity;
        }
        else if (lastCheckInActivity.getUsername().equals(lastCheckOutActivity.getUsername()) ||
                lastCheckInActivity.getTransactionTime().isBefore(
                        lastCheckOutActivity.getTransactionTime()
                )){
            // the last user who checked in is already checked out, there's no one working
            // on the production line right now
            logger.debug(">> Someone checked in and already checked out");
            return null;
        }
        else {
            // the last user who checked in hasn't checked out yet, let's assume the user is
            // still working on the production line
            logger.debug(">> Someone checked in and haven't checked out");
            return lastCheckInActivity;

        }
    }

    public List<ProductionLine> findAllAssignedProductionLines(Long warehouseId) {
        return  findAllAssignedProductionLines(warehouseId, true);
    }
    public List<ProductionLine> findAllAssignedProductionLines(Long warehouseId, boolean loadDetails) {

        List<ProductionLine> productionLines
                = productionLineRepository.findByWarehouseId(warehouseId);
        logger.debug("We have {} production lines for warehouse id {}",
                productionLines.size(), warehouseId);
        productionLines =
                productionLines.stream()
                        .filter(productionLine ->  productionLine.getProductionLineAssignments().size() > 0
                        )
                        .collect(Collectors.toList());

        logger.debug("We have {} ASSIGNED production lines for warehouse id {}",
                productionLines.size(), warehouseId);
        if (productionLines.size() > 0 && loadDetails) {
            loadAttribute(productionLines);
        }
        return productionLines;
    }

    public ProductionLine removeProductionLine(Long id) {
        ProductionLine productionLine = findById(id);

        // make sure it doesn't have any work order assigned
        if (!productionLine.getProductionLineAssignments().isEmpty()) {
            throw WorkOrderException.raiseException("Can't remove the production line as there's " +
                    "work order on it");
        }
        delete(id);
        return productionLine;
    }

    public List<ProductionLineStatus> getProductionLineStatus(
            Long warehouseId,
            String name,
            LocalDateTime startTime,
            LocalDateTime endTime) {

        if (Objects.isNull(startTime)) {
            startTime = LocalDateTime.now().minusDays(1);
        }
        logger.debug("start to get production line's status for name: {}\n" +
                "start time: {}, end time: {}",
                Strings.isBlank(name) ? "N/A" : name,
                Objects.isNull(startTime) ? "N/A" : startTime,
                Objects.isNull(endTime) ? "N/A" : endTime);
        // we will calculate the production line's status
        // based on its monitor's transaction
        List<ProductionLineMonitorTransaction> productionLineMonitorTransactions =
                productionLineMonitorTransactionService.findAll(
                        warehouseId, null,
                        name, null,
                        startTime, endTime, null
                );

        if (Strings.isBlank(name)) {
            // we don't have the production line name passed in, then
            // return the status for all production line
            List<ProductionLine> productionLines = findAll(warehouseId,
                    null, null, false, false);
            return getProductionLineStatus(warehouseId, productionLines,
                    productionLineMonitorTransactions, startTime, endTime);

        }
        else {
            ProductionLine productionLine = findByName(warehouseId, name);
            if (Objects.isNull(productionLine)) {
                throw ProductionLineException.raiseException("Can't find production line by name " + name);
            }
            return getProductionLineStatus(warehouseId,
                    Collections.singletonList(productionLine),
                    productionLineMonitorTransactions, startTime, endTime);
        }
    }
    public List<ProductionLineStatus> getProductionLineStatus(
            Long warehouseId,
            List<ProductionLine> productionLines,
            List<ProductionLineMonitorTransaction> productionLineMonitorTransactions,
            LocalDateTime startTime,
            LocalDateTime endTime) {

        // group the result by production line
        // key: prouduction line id
        // value: list of transactions of this production line
        Map<Long, List<ProductionLineMonitorTransaction>> productionLineMonitorTransactionMap =
                new HashMap<>();

        productionLineMonitorTransactions.stream().filter(
                // the transaction may not belong to any production, in case the monitor is not setup
                // to any production line(which should not be a right scenario). In this case, we will
                // ignore the record
                productionLineMonitorTransaction -> Objects.nonNull(productionLineMonitorTransaction.getProductionLine())
        ).forEach(
                productionLineMonitorTransaction -> {
                    List<ProductionLineMonitorTransaction> specificProductionLineMonitorTransactions =
                            productionLineMonitorTransactionMap.getOrDefault(
                                    productionLineMonitorTransaction.getProductionLine().getId(),
                                    new ArrayList<>());
                    specificProductionLineMonitorTransactions.add(productionLineMonitorTransaction);
                    productionLineMonitorTransactionMap.put(
                            productionLineMonitorTransaction.getProductionLine().getId(),
                            specificProductionLineMonitorTransactions
                    );
                }
        );

        return productionLines.stream().map(
                    productionLine -> {
                        // for each production line, get the monitor transactions of this production line only

                        return getProductionLineStatus(warehouseId, productionLine,
                                productionLineMonitorTransactionMap.get(productionLine.getId()),
                                startTime, endTime);
                    }
               ).collect(Collectors.toList());

    }
    public ProductionLineStatus getProductionLineStatus(
            Long warehouseId,
            ProductionLine productionLine,
            List<ProductionLineMonitorTransaction> productionLineMonitorTransactions,
            LocalDateTime startTime,
            LocalDateTime endTime) {

        if (Objects.isNull(productionLineMonitorTransactions) ||
                productionLineMonitorTransactions.isEmpty()) {
            // there's no monitor transaction for this production line during the
            // time period, let's assume the production is inactive
            return new ProductionLineStatus(
                    productionLine, startTime, endTime,
                    false, 0.0, 0.0, null
            );
        }
        // there's monitor transaction for this production line, we will assume
        // the production line is active and calculate the cycle time
        Collections.sort(productionLineMonitorTransactions,
                Comparator.comparing(ProductionLineMonitorTransaction::getCreatedTime).reversed());

        ProductionLineMonitorTransaction lastMonitorTransaction = productionLineMonitorTransactions.get(0);
        LocalDateTime lastCycleHappensTiming = lastMonitorTransaction.getCreatedTime();
        double lastCycleTime = lastMonitorTransaction.getCycleTime();
        double averageCycleTime = productionLineMonitorTransactions.stream()
                .map(ProductionLineMonitorTransaction::getCycleTime).mapToDouble(Double::doubleValue)
                .average().orElse(0.0);

        logger.debug("production line: {}, lastCycleHappensTiming: {}, lastMonitorTransaction id {}",
                productionLine.getName(),
                lastCycleHappensTiming,
                lastMonitorTransaction.getId());
        // see if the production line is active
        // it is inactive if the last cycle time is too far away from the end time
        // If the end time is not passed in or is a future time, then in order to calculate the
        // status, we will use the current date time
        boolean active;
        if (Objects.isNull(endTime) || endTime.isAfter(LocalDateTime.now())) {
            active = LocalDateTime.now().minusSeconds((int)getMaxCycleTime(productionLine))
                    .isBefore(lastCycleHappensTiming);
        }
        else {

            active = endTime.minusSeconds((int)getMaxCycleTime(productionLine))
                    .isBefore(lastCycleHappensTiming);
        }
        return new ProductionLineStatus(
                productionLine, startTime, endTime,
                active, lastCycleTime, averageCycleTime,
                lastCycleHappensTiming
        );




    }


    private double getMaxCycleTime(ProductionLine productionLine) {
        return DEFAULT_MAX_CYCLE_TIME;
    }
}
