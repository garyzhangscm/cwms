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
import com.garyzhangscm.cwms.outbound.model.Order;
import com.garyzhangscm.cwms.outbound.repository.ShortAllocationConfigurationRepository;

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
import java.util.ArrayList;

import java.util.List;
import java.util.Objects;


@Service
public class ShortAllocationConfigurationService implements TestDataInitiableService {
    private static final Logger logger = LoggerFactory.getLogger(ShortAllocationConfigurationService.class);

    @Autowired
    private ShortAllocationConfigurationRepository shortAllocationConfigurationRepository;
    @Autowired
    private FileService fileService;
    @Autowired
    private WarehouseLayoutServiceRestemplateClient warehouseLayoutServiceRestemplateClient;


    @Value("${fileupload.test-data.short-allocation-configuration:short-allocation-configuration}")
    String testDataFile;


    public ShortAllocationConfiguration findById(Long id ) {
        return shortAllocationConfigurationRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.raiseException("short allocation not found by id: " + id));
    }


    public List<ShortAllocationConfiguration> findAll(Long warehouseId) {


        return  shortAllocationConfigurationRepository.findAll(
                (Root<ShortAllocationConfiguration> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) -> {
                    List<Predicate> predicates = new ArrayList<Predicate>();

                    predicates.add(criteriaBuilder.equal(root.get("warehouseId"), warehouseId));

                    Predicate[] p = new Predicate[predicates.size()];
                    return criteriaBuilder.and(predicates.toArray(p));
                }
        );
    }
    public ShortAllocationConfiguration save(ShortAllocationConfiguration shortAllocationConfiguration) {
        return shortAllocationConfigurationRepository.save(shortAllocationConfiguration);
    }


    public ShortAllocationConfiguration saveOrUpdate(ShortAllocationConfiguration shortAllocationConfiguration) {
        if (Objects.isNull(shortAllocationConfiguration.getId()) &&
                findAll(shortAllocationConfiguration.getWarehouseId()).size() > 0) {
            // Keep in mind that up till now(4/17/2020), we should only have one
            // configuration per warehouse
            shortAllocationConfiguration.setId(
                    findAll(shortAllocationConfiguration.getWarehouseId()).get(0).getId());
        }
        return save(shortAllocationConfiguration);
    }


    public List<ShortAllocationConfigurationCSVWrapper> loadData(InputStream inputStream) throws IOException {

        CsvSchema schema = CsvSchema.builder().
                addColumn("company").
                addColumn("warehouse").
                addColumn("enabled").
                addColumn("retryInterval").
                build().withHeader();

        return fileService.loadData(inputStream, schema, ShortAllocationConfigurationCSVWrapper.class);
    }

    public void initTestData(String warehouseName) {
        try {
            String testDataFileName = StringUtils.isBlank(warehouseName) ?
                    testDataFile + ".csv" :
                    testDataFile + "-" + warehouseName + ".csv";
            InputStream inputStream = new ClassPathResource(testDataFileName).getInputStream();
            List<ShortAllocationConfigurationCSVWrapper> shortAllocationConfigurationCSVWrappers = loadData(inputStream);
            shortAllocationConfigurationCSVWrappers.stream().
                    forEach(shortAllocationConfigurationCSVWrapper ->
                            saveOrUpdate(convertFromWrapper(shortAllocationConfigurationCSVWrapper)));
        } catch (IOException ex) {
            logger.debug("Exception while load test data: {}", ex.getMessage());
        }
    }


    private ShortAllocationConfiguration convertFromWrapper(ShortAllocationConfigurationCSVWrapper shortAllocationConfigurationCSVWrapper) {

        ShortAllocationConfiguration shortAllocationConfiguration = new ShortAllocationConfiguration();

        Warehouse warehouse =
                warehouseLayoutServiceRestemplateClient.getWarehouseByName(
                        shortAllocationConfigurationCSVWrapper.getCompany(),
                        shortAllocationConfigurationCSVWrapper.getWarehouse());

        shortAllocationConfiguration.setWarehouseId(warehouse.getId());
        shortAllocationConfiguration.setEnabled(shortAllocationConfigurationCSVWrapper.getEnabled());
        shortAllocationConfiguration.setRetryInterval(shortAllocationConfigurationCSVWrapper.getRetryInterval());
        return shortAllocationConfiguration;
    }

    public ShortAllocationConfiguration getShortAllocationConfiguration(Long warehouseId) {
        List<ShortAllocationConfiguration> shortAllocationConfigurations = findAll(warehouseId);
        if (shortAllocationConfigurations.size() == 0) {
            return null;
        }
        return shortAllocationConfigurations.get(0);
    }

}
