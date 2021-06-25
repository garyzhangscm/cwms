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
import com.garyzhangscm.cwms.resources.repository.WebClientTabDisplayConfigurationRepository;
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
public class WebClientTabDisplayConfigurationService implements TestDataInitiableService{
    private static final Logger logger = LoggerFactory.getLogger(WebClientTabDisplayConfigurationService.class);
    @Autowired
    private WebClientTabDisplayConfigurationRepository webClientTabDisplayConfigurationRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private LayoutServiceRestemplateClient layoutServiceRestemplateClient;
    @Autowired
    private FileService fileService;
    @Value("${fileupload.test-data.web-client-tab-display-configuration:web-client-tab-display-configuration}")
    String testDataFile;


    public WebClientTabDisplayConfiguration findById(Long id) {
        return webClientTabDisplayConfigurationRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.raiseException("web client tab display configuration not found by id: " + id));
    }

    public List<WebClientTabDisplayConfiguration> findAll(String name,
                                                          String description,
                                                          Long companyId,
                                                          Long warehouseId,
                                                          Long userId) {

        return webClientTabDisplayConfigurationRepository.findAll(
                (Root<WebClientTabDisplayConfiguration> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) -> {
                    List<Predicate> predicates = new ArrayList<Predicate>();



                    if (StringUtils.isNotBlank(name)) {
                        predicates.add(criteriaBuilder.equal(root.get("name"), name));
                    }
                    if (StringUtils.isNotBlank(description)) {
                        predicates.add(criteriaBuilder.like(root.get("description"), description));
                    }
                    if (Objects.nonNull(companyId)) {
                        predicates.add(criteriaBuilder.equal(root.get("companyId"), companyId));

                    }
                    if (Objects.nonNull(warehouseId)) {
                        predicates.add(criteriaBuilder.equal(root.get("warehouseId"), warehouseId));

                    }
                    if (Objects.nonNull(userId)) {
                        predicates.add(criteriaBuilder.equal(root.get("userId"), userId));

                    }
                    Predicate[] p = new Predicate[predicates.size()];
                    return criteriaBuilder.and(predicates.toArray(p));
                }
        );
    }
    public WebClientTabDisplayConfiguration save(WebClientTabDisplayConfiguration webClientTabDisplayConfiguration) {
        return webClientTabDisplayConfigurationRepository.save(webClientTabDisplayConfiguration);
    }
    public WebClientTabDisplayConfiguration findByName(String name) {
        return webClientTabDisplayConfigurationRepository.findByName(name);
    }



    public List<WebClientTabDisplayConfigurationCSVWrapper> loadData(InputStream inputStream) throws IOException {

        CsvSchema schema = CsvSchema.builder().
                addColumn("name").
                addColumn("description").
                addColumn("company").
                addColumn("warehouse").
                addColumn("username").
                addColumn("displayFlag").
                build().withHeader();

        return fileService.loadData(inputStream, schema, WebClientTabDisplayConfigurationCSVWrapper.class);
    }

    public void initTestData(Long companyId, String warehouseName) {
        try {
            String companyCode = companyId == null ?
                    "" : layoutServiceRestemplateClient.getCompanyById(companyId).getCode();

            String testDataFileName = StringUtils.isBlank(warehouseName) ?
                    testDataFile + ".csv" :
                    testDataFile + "-" + companyCode + "-" + warehouseName + ".csv";

            logger.debug("Start to init web client tab display configuration from {}",
                    testDataFileName);
            InputStream inputStream = new ClassPathResource(testDataFileName).getInputStream();
            List<WebClientTabDisplayConfigurationCSVWrapper> webClientTabDisplayConfigurationCSVWrappers = loadData(inputStream);

            logger.debug(">> get {} lines from the CSV file",
                    webClientTabDisplayConfigurationCSVWrappers.size());
            webClientTabDisplayConfigurationCSVWrappers.stream().forEach(
                    webClientTabDisplayConfigurationCSVWrapper ->
                            save(convertFromCSVWrapper(webClientTabDisplayConfigurationCSVWrapper)));
        } catch (IOException ex) {
            logger.debug("Exception while load test data: {}", ex.getMessage());
        }
    }

    private WebClientTabDisplayConfiguration convertFromCSVWrapper(
            WebClientTabDisplayConfigurationCSVWrapper webClientTabDisplayConfigurationCSVWrapper) {
        WebClientTabDisplayConfiguration webClientTabDisplayConfiguration = new WebClientTabDisplayConfiguration();
        webClientTabDisplayConfiguration.setName(
                webClientTabDisplayConfigurationCSVWrapper.getName());
        webClientTabDisplayConfiguration.setDescription(
                webClientTabDisplayConfigurationCSVWrapper.getDescription());
        webClientTabDisplayConfiguration.setDisplayFlag(
                webClientTabDisplayConfigurationCSVWrapper.getDisplayFlag());

        if (StringUtils.isNotBlank(webClientTabDisplayConfigurationCSVWrapper.getCompany())) {
            Company company = layoutServiceRestemplateClient.getCompanyByCode(
                    webClientTabDisplayConfigurationCSVWrapper.getCompany()
            );
            if (Objects.nonNull(company)) {
                webClientTabDisplayConfiguration.setCompanyId(company.getId());
                // only set the warehouse if the warehouse name and company name are
                // all present
                if (StringUtils.isNotBlank(webClientTabDisplayConfigurationCSVWrapper.getWarehouse())) {
                    Warehouse warehouse = layoutServiceRestemplateClient.getWarehouseByName(
                            company.getCode(), webClientTabDisplayConfigurationCSVWrapper.getWarehouse()
                    );
                    if (Objects.nonNull(warehouse)) {
                        webClientTabDisplayConfiguration.setWarehouseId(warehouse.getId());
                    }
                }
                // only set the user if both user name and company name are present
                if (StringUtils.isNotBlank(webClientTabDisplayConfigurationCSVWrapper.getUsername())) {
                    User user = userService.findByUsername(
                            company.getId(), webClientTabDisplayConfigurationCSVWrapper.getUsername()
                    );
                    if (Objects.nonNull(user)) {
                        webClientTabDisplayConfiguration.setUserId(user.getId());
                    }
                }
            }
        }

        logger.debug("Will save webClientTabDisplayConfiguration: \n{}",
                webClientTabDisplayConfiguration);
        return webClientTabDisplayConfiguration;


    }


    public Collection<WebClientTabDisplayConfiguration> getWebClientTabDisplayConfigurationService(Long companyId, Long warehouseId, User user) {
        // we will start with most specific configuration until we get the most generic config
        // most specific -> most generic
        // 1. company + warehouse + user
        // 2. company + user
        // 3. company + warehouse
        // 4. company
        // 5. default
        Map<String, WebClientTabDisplayConfiguration>  webClientTabDisplayConfigurationMap =
                new HashMap<>();

        List<WebClientTabDisplayConfiguration> webClientTabDisplayConfigurations = new ArrayList<>();
        // 1. company + warehouse + user
        webClientTabDisplayConfigurations = findAll(null, null, companyId, warehouseId, user.getId());
        addWebClientTabDisplayConfigurationToTheResult(
                webClientTabDisplayConfigurationMap, webClientTabDisplayConfigurations
        );
        // 2. company + user
        webClientTabDisplayConfigurations = findAll(null, null, companyId, null, user.getId());
        addWebClientTabDisplayConfigurationToTheResult(
                webClientTabDisplayConfigurationMap, webClientTabDisplayConfigurations
        );

        // 3. company + warehouse
        // 2. company + user
        webClientTabDisplayConfigurations = findAll(null, null, companyId, warehouseId, null);
        addWebClientTabDisplayConfigurationToTheResult(
                webClientTabDisplayConfigurationMap, webClientTabDisplayConfigurations
        );
        // 4. company
        webClientTabDisplayConfigurations = findAll(null, null, companyId, null, null);
        addWebClientTabDisplayConfigurationToTheResult(
                webClientTabDisplayConfigurationMap, webClientTabDisplayConfigurations
        );
        // 5. default
        webClientTabDisplayConfigurations = findAll(null, null, null, null, null);
        addWebClientTabDisplayConfigurationToTheResult(
                webClientTabDisplayConfigurationMap, webClientTabDisplayConfigurations
        );
        return webClientTabDisplayConfigurationMap.values();
    }

    private void addWebClientTabDisplayConfigurationToTheResult(
            Map<String, WebClientTabDisplayConfiguration> webClientTabDisplayConfigurationMap,
            List<WebClientTabDisplayConfiguration> webClientTabDisplayConfigurations) {
        // add the list of configuration to the map only if it is not exists yet
        webClientTabDisplayConfigurations.forEach(
                webClientTabDisplayConfiguration -> {
                    webClientTabDisplayConfigurationMap.putIfAbsent(
                            webClientTabDisplayConfiguration.getName(),
                            webClientTabDisplayConfiguration
                    );
                }
        );
    }

}
