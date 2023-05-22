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

import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import com.garyzhangscm.cwms.inventory.clients.WarehouseLayoutServiceRestemplateClient;
import com.garyzhangscm.cwms.inventory.exception.InventoryException;
import com.garyzhangscm.cwms.inventory.exception.ResourceNotFoundException;
import com.garyzhangscm.cwms.inventory.model.*;
import com.garyzhangscm.cwms.inventory.repository.InventoryStatusRepository;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import javax.persistence.criteria.*;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

@Service
public class InventoryStatusService implements TestDataInitiableService{
    private static final Logger logger = LoggerFactory.getLogger(InventoryStatusService.class);

    @Autowired
    private InventoryStatusRepository inventoryStatusRepository;
    @Autowired
    private WarehouseLayoutServiceRestemplateClient warehouseLayoutServiceRestemplateClient;
    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private FileService fileService;

    @Value("${fileupload.test-data.inventory-status:inventory_status}")
    String testDataFile;

    public InventoryStatus findById(Long id) {
        return inventoryStatusRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.raiseException("inventory status not found by id: " + id));
    }


    public List<InventoryStatus> findAll(Long warehouseId, String name,
                                         Boolean availableStatusFlag) {

        return inventoryStatusRepository.findAll(
                (Root<InventoryStatus> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) -> {
                    List<Predicate> predicates = new ArrayList<Predicate>();

                    predicates.add(criteriaBuilder.equal(root.get("warehouseId"), warehouseId));

                    if (StringUtils.isNotBlank(name)) {
                        if (name.contains("*")) {

                            predicates.add(criteriaBuilder.like(root.get("name"), name.replaceAll("\\*", "%")));
                        }
                        else {

                            predicates.add(criteriaBuilder.equal(root.get("name"), name));
                        }

                    }
                    if (Objects.nonNull(availableStatusFlag)) {

                        predicates.add(criteriaBuilder.equal(root.get("availableStatusFlag"), availableStatusFlag));
                    }


                    Predicate[] p = new Predicate[predicates.size()];
                    return criteriaBuilder.and(predicates.toArray(p));
                }
        );

    }


    public InventoryStatus findByName(Long warehouseId, String name){
        return inventoryStatusRepository.findByWarehouseIdAndName(warehouseId, name);
    }

    public InventoryStatus save(InventoryStatus inventoryStatus) {
        return inventoryStatusRepository.save(inventoryStatus);
    }

    public InventoryStatus saveOrUpdate(InventoryStatus inventoryStatus) {
        if (Objects.isNull(inventoryStatus.getId()) &&
                Objects.nonNull(findByName(inventoryStatus.getWarehouseId(), inventoryStatus.getName()))) {
            logger.debug("InventoryStatusService.saveOrUpdate: find existing inventory status with id : {}, from warehouse id {}, name {}",
                    findByName(inventoryStatus.getWarehouseId(), inventoryStatus.getName()).getId(),
                    inventoryStatus.getWarehouseId(),
                    inventoryStatus.getName());
            inventoryStatus.setId(findByName(inventoryStatus.getWarehouseId(), inventoryStatus.getName()).getId());
        }
        InventoryStatus newInventoryStatus = save(inventoryStatus);
        if (Boolean.TRUE.equals(newInventoryStatus.getAvailableStatusFlag())) {
            resetAvailableStatus(newInventoryStatus);
        }
        return newInventoryStatus;
    }

    /**
     * Every time we add a new inventory status or change the inventory status and mark it
     * as the defaual available status, we will make sure it is the only one available status
     *  defined for the warehouse
     * @param newInventoryStatus
     */
    private void resetAvailableStatus(InventoryStatus newInventoryStatus) {
        if (Boolean.TRUE.equals(newInventoryStatus.getAvailableStatusFlag())) {
            inventoryStatusRepository.resetAvailableStatus(
                    newInventoryStatus.getWarehouseId(), newInventoryStatus.getId());
        }
    }

    public void delete(InventoryStatus inventoryStatus) {
        inventoryStatusRepository.delete(inventoryStatus);
    }
    public void delete(Long id) {
        inventoryStatusRepository.deleteById(id);
    }
    public void delete(String inventoryStatusIds) {
        if (!inventoryStatusIds.isEmpty()) {
            long[] inventoryStatusIdArray = Arrays.asList(inventoryStatusIds.split(",")).stream().mapToLong(Long::parseLong).toArray();
            for(long id : inventoryStatusIdArray) {
                delete(id);
            }
        }

    }

    public List<InventoryStatusCSVWrapper> loadData(InputStream inputStream) throws IOException {

        CsvSchema schema = CsvSchema.builder().
                addColumn("company").
                addColumn("warehouse").
                addColumn("name").
                addColumn("description").
                build().withHeader();

        return fileService.loadData(inputStream, schema, InventoryStatusCSVWrapper.class);
    }

    public void initTestData(Long companyId, String warehouseName) {
        try {
            String companyCode = warehouseLayoutServiceRestemplateClient.getCompanyById(companyId).getCode();

            String testDataFileName = StringUtils.isBlank(warehouseName) ?
                    testDataFile + ".csv" :
                    testDataFile + "-" + companyCode + "-" + warehouseName + ".csv";

            InputStream inputStream = new ClassPathResource(testDataFileName).getInputStream();

            List<InventoryStatusCSVWrapper> inventoryStatusCSVWrappers = loadData(inputStream);
            inventoryStatusCSVWrappers.stream().forEach(inventoryStatusCSVWrapper -> saveOrUpdate(convertFromWrapper(inventoryStatusCSVWrapper)));
        } catch (IOException ex) {
            logger.debug("Exception while load test data: {}", ex.getMessage());
        }
    }

    private InventoryStatus convertFromWrapper(InventoryStatusCSVWrapper inventoryStatusCSVWrapper) {
        InventoryStatus inventoryStatus = new InventoryStatus();
        inventoryStatus.setName(inventoryStatusCSVWrapper.getName());
        inventoryStatus.setDescription(inventoryStatusCSVWrapper.getDescription());



        // warehouse
        if (StringUtils.isNotBlank(inventoryStatusCSVWrapper.getWarehouse())) {
            Warehouse warehouse =
                    warehouseLayoutServiceRestemplateClient.getWarehouseByName(
                            inventoryStatusCSVWrapper.getCompany(),
                            inventoryStatusCSVWrapper.getWarehouse());
            if (warehouse != null) {
                inventoryStatus.setWarehouseId(warehouse.getId());
            }
        }
        logger.debug("Start to save invenotry status, warehouse id {}, name {}",
                inventoryStatus.getWarehouseId(), inventoryStatus.getName());
        return inventoryStatus;
    }

    public Optional<InventoryStatus> getAvailableInventoryStatus(Long warehouseId) {
        return findAll(warehouseId, null, true).stream().findFirst();
    }

    public InventoryStatus removeInventoryStatus(Long warehouseId, Long id) {
        InventoryStatus inventoryStatus = findById(id);
        validateInventoryStatusForRemove(warehouseId, inventoryStatus);
        delete(id);
        return inventoryStatus;
    }

    private void validateInventoryStatusForRemove(Long warehouseId, InventoryStatus inventoryStatus) {
        // we are allowed to remove the inventory status only when there's no one is using the status
        if (Boolean.TRUE.equals(inventoryStatus.getAvailableStatusFlag()) ) {
            throw InventoryException.raiseException("Can't remove the inventory status " + inventoryStatus.getName() +
                    " as it is the default available status");
        }
        if (!inventoryService.findAll(warehouseId,
                null, null, null, null,
                null, null, inventoryStatus.getId(), null,
                null, null, null, null, null,
                null, null, null, null,null,null,null,
                null, null, null, null,false, null).isEmpty()) {
            throw InventoryException.raiseException("Can't remove the inventory status " + inventoryStatus.getName() +
                    " as there's existing inventory");
        }

    }

    public InventoryStatus createInventoryStatus(InventoryStatus inventoryStatus) {
        return saveOrUpdate(inventoryStatus);
    }

    public InventoryStatus changeInventoryStatus(InventoryStatus inventoryStatus) {
        return saveOrUpdate(inventoryStatus);
    }
}
