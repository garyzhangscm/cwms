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
import com.garyzhangscm.cwms.outbound.exception.ResourceNotFoundException;
import com.garyzhangscm.cwms.outbound.model.*;
import com.garyzhangscm.cwms.outbound.repository.CartonizationConfigurationRepository;
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
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;


@Service
public class CartonizationConfigurationService implements TestDataInitiableService {
    private static final Logger logger = LoggerFactory.getLogger(CartonizationConfigurationService.class);

    @Autowired
    private CartonizationConfigurationRepository cartonizationConfigurationRepository;


    @Autowired
    private CommonServiceRestemplateClient commonServiceRestemplateClient;
    @Autowired
    private WarehouseLayoutServiceRestemplateClient warehouseLayoutServiceRestemplateClient;
    @Autowired
    private InventoryServiceRestemplateClient inventoryServiceRestemplateClient;
    @Autowired
    private FileService fileService;

    @Value("${fileupload.test-data.cartonization-configuration:cartonization-configuration}")
    String testDataFile;

    public CartonizationConfiguration findById(Long id, boolean loadDetails) {
        CartonizationConfiguration cartonizationConfiguration
                = cartonizationConfigurationRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.raiseException("cartonization configuration not found by id: " + id));
        if (loadDetails) {
            loadAttribute(cartonizationConfiguration);
        }
        return cartonizationConfiguration;
    }

    public CartonizationConfiguration findBySequence(int sequence) {
        return findBySequence(sequence, true);
    }


    public CartonizationConfiguration findBySequence(int sequence, boolean loadDetails) {
        CartonizationConfiguration cartonizationConfiguration = cartonizationConfigurationRepository.findBySequence(sequence);
        if (cartonizationConfiguration != null && loadDetails) {
            loadAttribute(cartonizationConfiguration);
        }
        return cartonizationConfiguration;
    }

    public CartonizationConfiguration findById(Long id) {
        return findById(id, true);
    }


    public List<CartonizationConfiguration> findAll(boolean loadDetails) {
        List<CartonizationConfiguration> cartonizationConfigurations = cartonizationConfigurationRepository.findAll();

        if (cartonizationConfigurations.size() > 0 && loadDetails) {
            loadAttribute(cartonizationConfigurations);
        }
        return cartonizationConfigurations;
    }

    public List<CartonizationConfiguration> findAll() {
        return findAll(true);
    }

    public List<CartonizationConfiguration> findAll(Long warehouseId, String clientIds,
                                                    String pickType, Boolean enabled,
                                                    Integer sequence) {
        return findAll(warehouseId, clientIds, pickType, enabled, sequence,true);
    }

    public List<CartonizationConfiguration> findAll(Long warehouseId,  String clientIds,
                                                    String pickType, Boolean enabled,
                                                    Integer sequence,
                                                    boolean loadDetails) {
        List<CartonizationConfiguration> cartonizationConfigurations =  cartonizationConfigurationRepository.findAll(
                (Root<CartonizationConfiguration> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) -> {
                    List<Predicate> predicates = new ArrayList<Predicate>();

                    predicates.add(criteriaBuilder.equal(root.get("warehouseId"), warehouseId));

                    if (StringUtils.isNotBlank(clientIds)) {
                        CriteriaBuilder.In<Long> inClientIds = criteriaBuilder.in(root.get("clientId"));
                        for(String id : clientIds.split(",")) {
                            inClientIds.value(Long.parseLong(id));
                        }
                        predicates.add(criteriaBuilder.and(inClientIds));
                    }

                    if (StringUtils.isNotBlank(pickType)) {
                        predicates.add(criteriaBuilder.equal(root.get("pickType"),
                                PickType.valueOf(pickType)));

                    }
                    if (Objects.nonNull(enabled)) {
                        predicates.add(criteriaBuilder.equal(root.get("enabled"),enabled));

                    }
                    if (Objects.nonNull(sequence)) {
                        predicates.add(criteriaBuilder.equal(root.get("sequence"),sequence));

                    }

                    Predicate[] p = new Predicate[predicates.size()];
                    return criteriaBuilder.and(predicates.toArray(p));
                }
        );
        logger.debug("## Find {} cartonization configuration by warehouse Id {}",
                cartonizationConfigurations.size(), warehouseId);

        if (cartonizationConfigurations.size() > 0 && loadDetails) {
            loadAttribute(cartonizationConfigurations);
        }
        return cartonizationConfigurations;
    }


    private void loadAttribute(CartonizationConfiguration cartonizationConfiguration) {

        if (Objects.nonNull(cartonizationConfiguration.getWarehouseId()) &&
                Objects.isNull(cartonizationConfiguration.getWarehouse())) {

            cartonizationConfiguration.setWarehouse(warehouseLayoutServiceRestemplateClient.getWarehouseById(cartonizationConfiguration.getWarehouseId()));
        }
        if (Objects.nonNull(cartonizationConfiguration.getClientId()) &&
                Objects.isNull(cartonizationConfiguration.getClient())) {
            try {
                cartonizationConfiguration.setClient(commonServiceRestemplateClient.getClientById(cartonizationConfiguration.getClientId()));
            }
            catch (Exception ex) {}
        }
    }

    private void loadAttribute(List<CartonizationConfiguration> cartonizationConfigurations) {
        cartonizationConfigurations.forEach(cartonizationConfiguration -> loadAttribute(cartonizationConfiguration));
    }

    public CartonizationConfiguration save(CartonizationConfiguration cartonizationConfiguration) {
        return cartonizationConfigurationRepository.save(cartonizationConfiguration);
    }

    public CartonizationConfiguration saveOrUpdate(CartonizationConfiguration cartonizationConfiguration) {
        if (Objects.isNull(cartonizationConfiguration.getId()) &&
                Objects.nonNull(findBySequence(cartonizationConfiguration.getSequence()))) {
            cartonizationConfiguration.setId(findBySequence(cartonizationConfiguration.getSequence()).getId());
        }
        return save(cartonizationConfiguration);
    }


    public void delete(CartonizationConfiguration cartonizationConfiguration) {
        cartonizationConfigurationRepository.delete(cartonizationConfiguration);
    }

    public void delete(Long id) {
        cartonizationConfigurationRepository.deleteById(id);
    }


    public List<CartonizationConfigurationCSVWrapper> loadData(InputStream inputStream) throws IOException {

        CsvSchema schema = CsvSchema.builder().
                addColumn("company").
                addColumn("sequence").
                addColumn("warehouse").
                addColumn("client").
                addColumn("pickType").
                addColumn("groupRule").
                addColumn("enabled").
                build().withHeader();

        return fileService.loadData(inputStream, schema, CartonizationConfigurationCSVWrapper.class);
    }

    public void initTestData(Long companyId, String warehouseName) {
        try {

            String companyCode = warehouseLayoutServiceRestemplateClient.getCompanyById(companyId).getCode();

            String testDataFileName = StringUtils.isBlank(warehouseName) ?
                    testDataFile + ".csv" :
                    testDataFile + "-" + companyCode + "-" + warehouseName + ".csv";

            InputStream inputStream = new ClassPathResource(testDataFileName).getInputStream();
            List<CartonizationConfigurationCSVWrapper> cartonizationConfigurationCSVWrappers = loadData(inputStream);
            cartonizationConfigurationCSVWrappers
                    .stream()
                    .forEach(
                            cartonizationConfigurationCSVWrapper
                                -> saveOrUpdate(convertFromWrapper(cartonizationConfigurationCSVWrapper)));
        } catch (IOException ex) {
            logger.debug("Exception while load test data: {}", ex.getMessage());
        }
    }

    private CartonizationConfiguration convertFromWrapper(CartonizationConfigurationCSVWrapper cartonizationConfigurationCSVWrapper) {

        CartonizationConfiguration cartonizationConfiguration = new CartonizationConfiguration();
        cartonizationConfiguration.setSequence(cartonizationConfigurationCSVWrapper.getSequence());
        cartonizationConfiguration.setPickType(PickType.valueOf(cartonizationConfigurationCSVWrapper.getPickType()));


        Warehouse warehouse = warehouseLayoutServiceRestemplateClient.getWarehouseByName(
                    cartonizationConfigurationCSVWrapper.getCompany(),
                    cartonizationConfigurationCSVWrapper.getWarehouse()
        );

        if (!StringUtils.isBlank(cartonizationConfigurationCSVWrapper.getClient())) {
            Client client = commonServiceRestemplateClient.getClientByName(
                    warehouse.getId(),
                    cartonizationConfigurationCSVWrapper.getClient()
            );
            if (client != null) {
                cartonizationConfiguration.setClientId(client.getId());
            }
        }


        logger.debug("Start to process group rule: {}", cartonizationConfigurationCSVWrapper.getGroupRule());
        List<CartonizationGroupRule> groupRules =
                    Arrays.stream(cartonizationConfigurationCSVWrapper.getGroupRule().split("\\|")).
                            map(groupRule -> {
                                logger.debug("Get Group Rule: {}", groupRule);
                                return CartonizationGroupRule.valueOf(groupRule);
                            }).collect(Collectors.toList());
        logger.debug("Get {} group rules: {}", groupRules.size(), groupRules);
        cartonizationConfiguration.setGroupRules(groupRules);

        cartonizationConfiguration.setEnabled(cartonizationConfigurationCSVWrapper.isEnabled());

        return cartonizationConfiguration;

    }

    public List<CartonizationConfiguration> findMatchedCartonizationConfiguration(Pick pick) {
        List<CartonizationConfiguration> cartonizationConfigurations = findAll(pick.getWarehouseId(), null, null, null, null);
        logger.debug("Start to find matched cartonization configuration for pick: {} / from {} configuration",
                pick, cartonizationConfigurations.size());

        return cartonizationConfigurations.stream()
                .filter(cartonizationConfiguration -> match(cartonizationConfiguration,pick))
                .collect(Collectors.toList());
    }

    private boolean match(CartonizationConfiguration cartonizationConfiguration, Pick pick) {
        logger.debug("Start to match cartonization configuration {} with pick {}",
                cartonizationConfiguration, pick);
        if (!cartonizationConfiguration.getEnabled()) {
            logger.debug("{} is not enabled! Non match", cartonizationConfiguration);
            return false;
        }

        if (Objects.nonNull(cartonizationConfiguration.getWarehouseId())&&
                !cartonizationConfiguration.getWarehouseId().equals(pick.getWarehouseId())) {
            logger.debug("The warehouse ID doesn't match!warehouse ID in configuration {} " +
                    "doesnt match with pick's warehouse id {}",
                    cartonizationConfiguration.getWarehouseId(), pick.getWarehouseId());
            return false;
        }

        if (!cartonizationConfiguration.getPickType().equals(pick.getPickType())) {

            logger.debug("The type doesn't match! type in configuration {} " +
                            "doesnt match with pick's type {}",
                    cartonizationConfiguration.getPickType(), pick.getPickType());
            return false;
        }
        // If the configuraiton has client id defined, we will need to make sure
        // the pick's client id not null
        //
        if (Objects.nonNull(cartonizationConfiguration.getClientId()) &&
                (Objects.isNull(pick.getClient())
                        || !pick.getClient().getId().equals(cartonizationConfiguration.getClientId()))) {

            logger.debug("The client doesn't match! client id in configuration {} " +
                            "doesnt match with pick's client id {}",
                    cartonizationConfiguration.getClientId(), pick.getClient());
            return false;
        }

        logger.debug(">> list picking configuraiton matches with the pick!");
        return true;

    }


}
