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

package com.garyzhangscm.cwms.resources.service;

import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import com.garyzhangscm.cwms.resources.clients.LayoutServiceRestemplateClient;
import com.garyzhangscm.cwms.resources.exception.ResourceNotFoundException;
import com.garyzhangscm.cwms.resources.model.*;
import com.garyzhangscm.cwms.resources.repository.MenuRepository;
import com.garyzhangscm.cwms.resources.repository.SystemConfigurationRepository;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Objects;

@Service
public class SystemConfigurationService implements TestDataInitiableService{
    private static final Logger logger = LoggerFactory.getLogger(UserService.class);
    @Autowired
    private SystemConfigurationRepository systemConfigurationRepository;

    @Autowired
    private LayoutServiceRestemplateClient layoutServiceRestemplateClient;

    @Autowired
    private FileService fileService;
    @Value("${fileupload.test-data.system-configuration:system-configuration}")
    String testDataFile;

    public SystemConfiguration findById(Long id) {
        return systemConfigurationRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.raiseException("system configuration  not found by id: " + id));
    }

    public List<SystemConfiguration> findAll() {

        return systemConfigurationRepository.findAll();
    }
    public SystemConfiguration save(SystemConfiguration systemConfiguration) {
        return systemConfigurationRepository.save(systemConfiguration);
    }
    public SystemConfiguration saveOrUpdate(SystemConfiguration systemConfiguration) {
        if (Objects.nonNull(findByCompanyAndWarehouse(
                systemConfiguration.getCompanyId(), systemConfiguration.getWarehouseId()))) {
            systemConfiguration.setId(
                    findByCompanyAndWarehouse(
                            systemConfiguration.getCompanyId(), systemConfiguration.getWarehouseId()
                    ).getId()
            );
        }
        return systemConfigurationRepository.save(systemConfiguration);
    }
    public SystemConfiguration findByCompanyAndWarehouse(Long companyId, Long warehouseId) {
        return systemConfigurationRepository.findByCompanyIdAndWarehouseId(
                companyId, warehouseId);
    }
    public SystemConfiguration findByCompanyIdAndWarehouseName(Long companyId, String warehouseName) {
        Company company = layoutServiceRestemplateClient.getCompanyById(companyId);
        if (Objects.isNull(company)) {
            return null;
        }
        Warehouse warehouse = layoutServiceRestemplateClient.getWarehouseByName(
                company.getCode(), warehouseName
        );
        if (Objects.isNull(warehouse)) {
            return null;
        }
        SystemConfiguration systemConfiguration = findByCompanyAndWarehouse(
                company.getId(), warehouse.getId()
        );
        return systemConfiguration;

    }

    public boolean allowInitTestData(Long companyId, String warehouseName) {
        SystemConfiguration systemConfiguration = findByCompanyIdAndWarehouseName(
                companyId, warehouseName
        );
        if (Objects.isNull(systemConfiguration)) {
            return false;
        }
        else {
            return systemConfiguration.getAllowDataInitialFlag();
        }
    }
    public boolean allowInitTestData(Long companyId, Long warehouseId) {
        SystemConfiguration systemConfiguration = findByCompanyAndWarehouse(
                companyId, warehouseId
        );
        if (Objects.isNull(systemConfiguration)) {
            return false;
        }
        else {
            return systemConfiguration.getAllowDataInitialFlag();
        }
    }



    public List<SystemConfigurationCSVWrapper> loadData(InputStream inputStream) throws IOException {

        CsvSchema schema = CsvSchema.builder().
                addColumn("company").
                addColumn("warehouse").
                addColumn("allowDataInitialFlag").
                addColumn("serverSidePrinting").
                build().withHeader();

        return fileService.loadData(inputStream, schema, SystemConfigurationCSVWrapper.class);
    }

    public void initTestData(Long companyId, String warehouseName) {
        try {
            String companyCode = companyId == null ?
                    "" : layoutServiceRestemplateClient.getCompanyById(companyId).getCode();

            String testDataFileName = StringUtils.isBlank(warehouseName) ?
                    testDataFile + ".csv" :
                    testDataFile + "-" + companyCode + "-" + warehouseName + ".csv";
            InputStream inputStream = new ClassPathResource(testDataFileName).getInputStream();
            List<SystemConfigurationCSVWrapper> systemConfigurationCSVWrappers = loadData(inputStream);
            systemConfigurationCSVWrappers.stream().forEach(
                    systemConfigurationCSVWrapper -> saveOrUpdate(convertFromCSVWrapper(
                            systemConfigurationCSVWrapper)));
        } catch (IOException ex) {
            logger.debug("Exception while load test data: {}", ex.getMessage());
        }
    }

    private SystemConfiguration convertFromCSVWrapper(
            SystemConfigurationCSVWrapper systemConfigurationCSVWrapper) {
        SystemConfiguration systemConfiguration = new SystemConfiguration();
        systemConfiguration.setAllowDataInitialFlag(
                systemConfigurationCSVWrapper.getAllowDataInitialFlag());
        systemConfiguration.setServerSidePrinting(
                systemConfigurationCSVWrapper.getServerSidePrinting());


        if (StringUtils.isNotBlank(systemConfigurationCSVWrapper.getCompany())) {
            Company company = layoutServiceRestemplateClient.getCompanyByCode(
                    systemConfigurationCSVWrapper.getCompany()
            );
            if (Objects.nonNull(company)) {
                systemConfiguration.setCompanyId(company.getId());
                // only set the warehouse if the warehouse name and company name are
                // all present
                if (StringUtils.isNotBlank(systemConfigurationCSVWrapper.getWarehouse())) {
                    Warehouse warehouse = layoutServiceRestemplateClient.getWarehouseByName(
                            company.getCode(), systemConfigurationCSVWrapper.getWarehouse()
                    );
                    if (Objects.nonNull(warehouse)) {
                        systemConfiguration.setWarehouseId(warehouse.getId());
                    }
                }

            }
        }

        return systemConfiguration;

    }




}
