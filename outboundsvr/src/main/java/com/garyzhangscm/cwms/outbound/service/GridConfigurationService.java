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
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


@Service
public class GridConfigurationService implements TestDataInitiableService {
    private static final Logger logger = LoggerFactory.getLogger(GridConfigurationService.class);

    @Autowired
    private GridConfigurationRepository gridConfigurationRepository;


    @Autowired
    private WarehouseLayoutServiceRestemplateClient warehouseLayoutServiceRestemplateClient;

    @Autowired
    private FileService fileService;

    @Value("${fileupload.test-data.grid-configuration:grid-configuration}")
    String testDataFile;

    public GridConfiguration findById(Long id) {
        return findById(id, true);
    }
    public GridConfiguration findById(Long id, boolean loadDetails) {
        GridConfiguration gridConfiguration = gridConfigurationRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.raiseException("grid configuration not found by id: " + id));

        if (Objects.nonNull(gridConfiguration) && loadDetails) {
            loadAttribute(gridConfiguration);
        }
        return gridConfiguration;
    }

    public GridConfiguration save(GridConfiguration gridConfiguration) {
        return save(gridConfiguration, true);
    }
    public GridConfiguration save(GridConfiguration gridConfiguration, boolean loadDetails) {
        GridConfiguration newGridConfiguration =  gridConfigurationRepository.save(gridConfiguration);
        if (loadDetails) {
            loadAttribute(newGridConfiguration);
        }
        return newGridConfiguration;
    }

    public GridConfiguration saveOrUpdate(GridConfiguration gridConfiguration) {
        if (Objects.isNull(gridConfiguration.getId()) &&
                Objects.nonNull(findByWarehouseIdAndLocationGroupId(
                        gridConfiguration.getWarehouseId(),gridConfiguration.getLocationGroupId()))) {
            gridConfiguration.setId(
                    findByWarehouseIdAndLocationGroupId(
                            gridConfiguration.getWarehouseId(),gridConfiguration.getLocationGroupId()).getId()
            );
        }
        return gridConfigurationRepository.save(gridConfiguration);
    }
    public GridConfiguration findByWarehouseIdAndLocationGroupId(Long warehouseId, Long locationGroupId) {
        return findByWarehouseIdAndLocationGroupId(warehouseId, locationGroupId, true);
    }
    public GridConfiguration findByWarehouseIdAndLocationGroupId(Long warehouseId, Long locationGroupId,
                                                                 boolean loadDetails) {
        GridConfiguration gridConfiguration =
                gridConfigurationRepository.findByWarehouseIdAndLocationGroupId(warehouseId, locationGroupId);

        if (Objects.nonNull(gridConfiguration) && loadDetails) {
            loadAttribute(gridConfiguration);
        }

        return gridConfiguration;
    }
    public GridConfiguration addGridConfiguration(Long warehouseId,
                                       GridConfiguration gridConfiguration) {
        if (Objects.isNull(gridConfiguration.getWarehouseId())) {
            gridConfiguration.setWarehouseId(warehouseId);
        }
        return save(gridConfiguration);
    }

    public List<GridConfiguration> findAll(Long warehouseId, Long locationGroupId) {
        return findAll(warehouseId, locationGroupId, true);

    }
    public List<GridConfiguration> findAll(Long warehouseId, Long locationGroupId, boolean loadDetails) {

        List<GridConfiguration> gridConfigurations =  gridConfigurationRepository.findAll(
                (Root<GridConfiguration> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) -> {
                    List<Predicate> predicates = new ArrayList<Predicate>();
                    predicates.add(criteriaBuilder.equal(root.get("warehouseId"), warehouseId));

                    if (Objects.nonNull(locationGroupId)) {
                        predicates.add(criteriaBuilder.equal(root.get("locationGroupId"), locationGroupId));

                    }


                    Predicate[] p = new Predicate[predicates.size()];
                    return criteriaBuilder.and(predicates.toArray(p));
                }
        );
        if (gridConfigurations.size() > 0 && loadDetails) {
            loadAttribute(gridConfigurations);
        }

        return gridConfigurations;
    }

    private void loadAttribute(List<GridConfiguration> gridConfigurationList) {
        gridConfigurationList.forEach(this::loadAttribute);
    }
    private void loadAttribute(GridConfiguration gridConfiguration) {

        // Load the location group for the grid
        LocationGroup locationGroup = warehouseLayoutServiceRestemplateClient.getLocationGroupById(
                gridConfiguration.getLocationGroupId()
        );
        gridConfiguration.setLocationGroup(locationGroup);
    }

    public List<GridConfigurationCSVWrapper> loadData(InputStream inputStream) throws IOException {

        CsvSchema schema = CsvSchema.builder().
                addColumn("warehouse").
                addColumn("locationGroup").
                addColumn("preAssignedLocation").
                addColumn("allowConfirmByGroup").
                addColumn("depositOnConfirm").
                build().withHeader();

        return fileService.loadData(inputStream, schema, GridConfigurationCSVWrapper.class);
    }

    public void initTestData(String warehouseName) {
        try {
            String testDataFileName = StringUtils.isBlank(warehouseName) ?
                    testDataFile + ".csv" :
                    testDataFile + "-" + warehouseName + ".csv";
            InputStream inputStream = new ClassPathResource(testDataFileName).getInputStream();
            List<GridConfigurationCSVWrapper> gridConfigurationCSVWrappers = loadData(inputStream);
            gridConfigurationCSVWrappers.stream().forEach(gridConfigurationCSVWrapper -> saveOrUpdate(convertFromWrapper(gridConfigurationCSVWrapper)));
        } catch (IOException ex) {
            logger.debug("Exception while load test data: {}", ex.getMessage());
        }
    }

    private GridConfiguration convertFromWrapper(GridConfigurationCSVWrapper gridConfigurationCSVWrapper) {

        logger.debug("Start to load grid configuration from data: \n{}", gridConfigurationCSVWrapper);
        GridConfiguration gridConfiguration = new GridConfiguration();

        Warehouse warehouse = warehouseLayoutServiceRestemplateClient.getWarehouseByName(
                gridConfigurationCSVWrapper.getWarehouse()
        );
        gridConfiguration.setWarehouseId(warehouse.getId());

        LocationGroup locationGroup = warehouseLayoutServiceRestemplateClient.getLocationGroupByName(
                warehouse.getId(),
                gridConfigurationCSVWrapper.getLocationGroup()
        );
        gridConfiguration.setLocationGroupId(locationGroup.getId());

        gridConfiguration.setAllowConfirmByGroup(
                gridConfigurationCSVWrapper.getAllowConfirmByGroup()
        );

        gridConfiguration.setDepositOnConfirm(
                gridConfigurationCSVWrapper.getDepositOnConfirm()
        );

        gridConfiguration.setPreAssignedLocation(
                gridConfigurationCSVWrapper.getPreAssignedLocation()
        );


        return  gridConfiguration;

    }


}
