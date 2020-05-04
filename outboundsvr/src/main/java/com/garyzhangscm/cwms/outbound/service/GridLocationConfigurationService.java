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
import com.garyzhangscm.cwms.outbound.clients.WarehouseLayoutServiceRestemplateClient;
import com.garyzhangscm.cwms.outbound.exception.ResourceNotFoundException;
import com.garyzhangscm.cwms.outbound.model.*;
import com.garyzhangscm.cwms.outbound.repository.GridConfigurationRepository;
import com.garyzhangscm.cwms.outbound.repository.GridLocationConfigurationRepository;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;


@Service
public class GridLocationConfigurationService implements TestDataInitiableService {
    private static final Logger logger = LoggerFactory.getLogger(GridLocationConfigurationService.class);

    @Autowired
    private GridLocationConfigurationRepository gridLocationConfigurationRepository;


    @Autowired
    private WarehouseLayoutServiceRestemplateClient warehouseLayoutServiceRestemplateClient;

    @Autowired
    private FileService fileService;

    @Value("${fileupload.test-data.grid-location-configuration:grid-location-configuration}")
    String testDataFile;

    public GridLocationConfiguration findById(Long id) {
        return findById(id, true);
    }
    public GridLocationConfiguration findById(Long id, boolean loadDetails) {
        GridLocationConfiguration gridLocationConfiguration =  gridLocationConfigurationRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.raiseException("grid location configuration not found by id: " + id));

        if (Objects.nonNull(gridLocationConfiguration) && loadDetails) {
            loadAttribute(gridLocationConfiguration);
        }
        return gridLocationConfiguration;
    }

    public GridLocationConfiguration save(GridLocationConfiguration gridLocationConfiguration) {
        return gridLocationConfigurationRepository.save(gridLocationConfiguration);
    }

    public GridLocationConfiguration saveOrUpdate(GridLocationConfiguration gridLocationConfiguration) {
        if (Objects.isNull(gridLocationConfiguration.getId()) &&
                Objects.nonNull(findByWarehouseIdAndLocationId(
                        gridLocationConfiguration.getWarehouseId(),gridLocationConfiguration.getLocationId()))) {
            gridLocationConfiguration.setId(
                    findByWarehouseIdAndLocationId(
                            gridLocationConfiguration.getWarehouseId(),gridLocationConfiguration.getLocationId()).getId()
            );
        }
        return gridLocationConfigurationRepository.save(gridLocationConfiguration);
    }
    public GridLocationConfiguration findByWarehouseIdAndLocationId(Long warehouseId, Long locationId) {
        return gridLocationConfigurationRepository.findByWarehouseIdAndLocationId(warehouseId, locationId);
    }
    public GridLocationConfiguration addGridLocationConfiguration(Long warehouseId,
                                                          GridLocationConfiguration gridLocationConfiguration) {
        if (Objects.isNull(gridLocationConfiguration.getWarehouseId())) {
            gridLocationConfiguration.setWarehouseId(warehouseId);
        }
        return save(gridLocationConfiguration);
    }

    public List<GridLocationConfiguration> findAll(Long warehouseId, Long locationGroupId) {
        return findAll(warehouseId, locationGroupId, true);
    }
    public List<GridLocationConfiguration> findAll(Long warehouseId, Long locationGroupId, boolean loadDetails) {

        List<GridLocationConfiguration> gridLocationConfigurations =  gridLocationConfigurationRepository.findAll(
                (Root<GridLocationConfiguration> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) -> {
                    List<Predicate> predicates = new ArrayList<Predicate>();
                    predicates.add(criteriaBuilder.equal(root.get("warehouseId"), warehouseId));

                    if (Objects.nonNull(locationGroupId)) {
                        predicates.add(criteriaBuilder.equal(root.get("locationGroupId"), locationGroupId));

                    }

                    Predicate[] p = new Predicate[predicates.size()];
                    return criteriaBuilder.and(predicates.toArray(p));
                }
        );

        if (gridLocationConfigurations.size() > 0 && loadDetails) {
            loadAttribute(gridLocationConfigurations);
        }

        // Sort teh result based on the row number
        // and the sequence in each row
        Collections.sort(gridLocationConfigurations, (o1, o2) -> {
            if (o1.getRowNumber().equals(o2.getRowNumber())) {
                return o1.getSequence().compareTo(o2.getSequence());
            }
            else {
                return o1.getRowNumber().compareTo(o2.getRowNumber());
            }
        });

        return gridLocationConfigurations;
    }

    private void loadAttribute(List<GridLocationConfiguration> gridLocationConfigurations) {



        gridLocationConfigurations.forEach(this::loadAttribute);
    }
    private void loadAttribute(GridLocationConfiguration gridLocationConfiguration) {
        LocationGroup locationGroup = warehouseLayoutServiceRestemplateClient.getLocationGroupById(
                gridLocationConfiguration.getLocationGroupId()
        );
        gridLocationConfiguration.setLocationGroup(locationGroup);

        Location location = warehouseLayoutServiceRestemplateClient.getLocationById(
                gridLocationConfiguration.getLocationId()
        );
        gridLocationConfiguration.setLocation(location);
    }

    public List<GridLocationConfigurationCSVWrapper> loadData(InputStream inputStream) throws IOException {

        CsvSchema schema = CsvSchema.builder().
                addColumn("warehouse").
                addColumn("location").
                addColumn("rowNumber").
                addColumn("columnSpan").
                addColumn("sequence").
                addColumn("pendingQuantity").
                build().withHeader();

        return fileService.loadData(inputStream, schema, GridLocationConfigurationCSVWrapper.class);
    }

    public void initTestData(String warehouseName) {
        try {
            String testDataFileName = StringUtils.isBlank(warehouseName) ?
                    testDataFile + ".csv" :
                    testDataFile + "-" + warehouseName + ".csv";
            InputStream inputStream = new ClassPathResource(testDataFileName).getInputStream();
            List<GridLocationConfigurationCSVWrapper> gridLocationConfigurationCSVWrappers = loadData(inputStream);
            gridLocationConfigurationCSVWrappers.stream().forEach(gridLocationConfigurationCSVWrapper ->
                    saveOrUpdate(convertFromWrapper(gridLocationConfigurationCSVWrapper)));
        } catch (IOException ex) {
            logger.debug("Exception while load test data: {}", ex.getMessage());
        }
    }

    private GridLocationConfiguration convertFromWrapper(GridLocationConfigurationCSVWrapper gridLocationConfigurationCSVWrapper) {

        GridLocationConfiguration gridLocationConfiguration = new GridLocationConfiguration();

        Warehouse warehouse = warehouseLayoutServiceRestemplateClient.getWarehouseByName(
                gridLocationConfigurationCSVWrapper.getWarehouse()
        );
        gridLocationConfiguration.setWarehouseId(warehouse.getId());

        Location location = warehouseLayoutServiceRestemplateClient.getLocationByName(
                warehouse.getId(),
                gridLocationConfigurationCSVWrapper.getLocation()
        );
        gridLocationConfiguration.setLocationId(location.getId());

        gridLocationConfiguration.setLocationGroupId(location.getLocationGroup().getId());


        gridLocationConfiguration.setRowNumber(
                gridLocationConfigurationCSVWrapper.getRowNumber()
        );

        gridLocationConfiguration.setColumnSpan(
                gridLocationConfigurationCSVWrapper.getColumnSpan()
        );


        gridLocationConfiguration.setSequence(
                gridLocationConfigurationCSVWrapper.getSequence()
        );

        gridLocationConfiguration.setPendingQuantity(
                gridLocationConfigurationCSVWrapper.getPendingQuantity()
        );


        return  gridLocationConfiguration;

    }

    public void increasePendingQuantity(GridLocationConfiguration gridLocation, Long quantity) {
        gridLocation.increasePendingQuantity(quantity);
        saveOrUpdate(gridLocation);
    }

    public void confirmGridDistributionWork(Long warehouseId,
                                            Location location, Inventory inventory) {
        GridLocationConfiguration gridLocationConfiguration
                = findByWarehouseIdAndLocationId(warehouseId, location.getId());

        synchronized (this) {
            logger.debug("Will increase grid location {}'s quantity by {}",
                    location.getName(), inventory.getQuantity());
            gridLocationConfiguration.increaseArrivedQuantity(inventory.getQuantity());

            saveOrUpdate(gridLocationConfiguration);
        }
    }


}
