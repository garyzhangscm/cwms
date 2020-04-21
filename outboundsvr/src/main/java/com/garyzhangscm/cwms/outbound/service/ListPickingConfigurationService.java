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
import com.garyzhangscm.cwms.outbound.model.Order;
import com.garyzhangscm.cwms.outbound.repository.EmergencyReplenishmentConfigurationRepository;
import com.garyzhangscm.cwms.outbound.repository.ListPickingConfigurationRepository;
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
import java.util.stream.Collectors;


@Service
public class ListPickingConfigurationService implements TestDataInitiableService {
    private static final Logger logger = LoggerFactory.getLogger(ListPickingConfigurationService.class);

    @Autowired
    private ListPickingConfigurationRepository listPickingConfigurationRepository;


    @Autowired
    private CommonServiceRestemplateClient commonServiceRestemplateClient;
    @Autowired
    private WarehouseLayoutServiceRestemplateClient warehouseLayoutServiceRestemplateClient;
    @Autowired
    private InventoryServiceRestemplateClient inventoryServiceRestemplateClient;
    @Autowired
    private FileService fileService;

    @Value("${fileupload.test-data.list-picking-configuration:list-picking-configuration}")
    String testDataFile;

    public ListPickingConfiguration findById(Long id, boolean loadDetails) {
        ListPickingConfiguration listPickingConfiguration
                = listPickingConfigurationRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.raiseException("list picking configuration not found by id: " + id));
        if (loadDetails) {
            loadAttribute(listPickingConfiguration);
        }
        return listPickingConfiguration;
    }

    public ListPickingConfiguration findBySequence(int sequence) {
        return findBySequence(sequence, true);
    }


    public ListPickingConfiguration findBySequence(int sequence, boolean loadDetails) {
        ListPickingConfiguration listPickingConfiguration = listPickingConfigurationRepository.findBySequence(sequence);
        if (listPickingConfiguration != null && loadDetails) {
            loadAttribute(listPickingConfiguration);
        }
        return listPickingConfiguration;
    }

    public ListPickingConfiguration findById(Long id) {
        return findById(id, true);
    }


    public List<ListPickingConfiguration> findAll(boolean loadDetails) {
        List<ListPickingConfiguration> listPickingConfigurations = listPickingConfigurationRepository.findAll();

        if (listPickingConfigurations.size() > 0 && loadDetails) {
            loadAttribute(listPickingConfigurations);
        }
        return listPickingConfigurations;
    }

    public List<ListPickingConfiguration> findAll() {
        return findAll(true);
    }

    public List<ListPickingConfiguration> findAll(Long warehouseId) {
        return findAll(warehouseId, true);
    }

    public List<ListPickingConfiguration> findAll(Long warehouseId, boolean loadDetails) {
        List<ListPickingConfiguration> listPickingConfigurations =  listPickingConfigurationRepository.findAll(
                (Root<ListPickingConfiguration> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) -> {
                    List<Predicate> predicates = new ArrayList<Predicate>();


                    predicates.add(criteriaBuilder.equal(root.get("warehouseId"), warehouseId));

                    Predicate[] p = new Predicate[predicates.size()];
                    return criteriaBuilder.and(predicates.toArray(p));
                }
        );
        logger.debug("## Find {} list picking configuration by warehouse Id {}",
                listPickingConfigurations.size(), warehouseId);

        if (listPickingConfigurations.size() > 0 && loadDetails) {
            loadAttribute(listPickingConfigurations);
        }
        return listPickingConfigurations;
    }


    private void loadAttribute(ListPickingConfiguration listPickingConfiguration) {

        if (Objects.nonNull(listPickingConfiguration.getWarehouseId()) &&
                Objects.isNull(listPickingConfiguration.getWarehouse())) {
            listPickingConfiguration.setWarehouse(warehouseLayoutServiceRestemplateClient.getWarehouseById(listPickingConfiguration.getWarehouseId()));
        }
        if (Objects.nonNull(listPickingConfiguration.getClientId()) &&
                Objects.isNull(listPickingConfiguration.getClient())) {
            listPickingConfiguration.setClient(commonServiceRestemplateClient.getClientById(listPickingConfiguration.getClientId()));
        }
    }

    private void loadAttribute(List<ListPickingConfiguration> listPickingConfigurations) {
        listPickingConfigurations.forEach(listPickingConfiguration -> loadAttribute(listPickingConfiguration));
    }

    public ListPickingConfiguration save(ListPickingConfiguration listPickingConfiguration) {
        return listPickingConfigurationRepository.save(listPickingConfiguration);
    }

    public ListPickingConfiguration saveOrUpdate(ListPickingConfiguration listPickingConfiguration) {
        if (listPickingConfiguration.getId() == null && findBySequence(listPickingConfiguration.getSequence()) != null) {
            listPickingConfiguration.setId(findBySequence(listPickingConfiguration.getSequence()).getId());
        }
        return save(listPickingConfiguration);
    }


    public void delete(ListPickingConfiguration listPickingConfiguration) {
        listPickingConfigurationRepository.delete(listPickingConfiguration);
    }

    public void delete(Long id) {
        listPickingConfigurationRepository.deleteById(id);
    }

    public void delete(String listPickingConfigurationIds) {
        if (!listPickingConfigurationIds.isEmpty()) {
            long[] listPickingConfigurationIdArray = Arrays.asList(listPickingConfigurationIds.split(",")).stream().mapToLong(Long::parseLong).toArray();
            for (long id : listPickingConfigurationIdArray) {
                delete(id);
            }
        }
    }


    public List<ListPickingConfigurationCSVWrapper> loadData(InputStream inputStream) throws IOException {

        CsvSchema schema = CsvSchema.builder().
                addColumn("sequence").
                addColumn("warehouse").
                addColumn("client").
                addColumn("pickType").
                addColumn("groupRule").
                addColumn("enabled").
                build().withHeader();

        return fileService.loadData(inputStream, schema, ListPickingConfigurationCSVWrapper.class);
    }

    public void initTestData(String warehouseName) {
        try {
            String testDataFileName = StringUtils.isBlank(warehouseName) ?
                    testDataFile + ".csv" :
                    testDataFile + "-" + warehouseName + ".csv";
            InputStream inputStream = new ClassPathResource(testDataFileName).getInputStream();
            List<ListPickingConfigurationCSVWrapper> listPickingConfigurationCSVWrappers = loadData(inputStream);
            listPickingConfigurationCSVWrappers
                    .stream()
                    .forEach(
                        listPickingConfigurationCSVWrapper
                                -> saveOrUpdate(convertFromWrapper(listPickingConfigurationCSVWrapper)));
        } catch (IOException ex) {
            logger.debug("Exception while load test data: {}", ex.getMessage());
        }
    }

    private ListPickingConfiguration convertFromWrapper(ListPickingConfigurationCSVWrapper listPickingConfigurationCSVWrapper) {

        ListPickingConfiguration listPickingConfiguration = new ListPickingConfiguration();
        listPickingConfiguration.setSequence(listPickingConfigurationCSVWrapper.getSequence());
        listPickingConfiguration.setPickType(PickType.valueOf(listPickingConfigurationCSVWrapper.getPickType()));

        if (!StringUtils.isBlank(listPickingConfigurationCSVWrapper.getWarehouse())) {
            Warehouse warehouse = warehouseLayoutServiceRestemplateClient.getWarehouseByName(
                   listPickingConfigurationCSVWrapper.getWarehouse()
            );
            if (warehouse != null) {
                listPickingConfiguration.setWarehouseId(warehouse.getId());
            }
        }

        if (!StringUtils.isBlank(listPickingConfigurationCSVWrapper.getClient())) {
            Client client = commonServiceRestemplateClient.getClientByName(
                    listPickingConfigurationCSVWrapper.getClient()
            );
            if (client != null) {
                listPickingConfiguration.setClientId(client.getId());
            }
        }

        listPickingConfiguration.setGroupRule(
                ListPickingGroupRule.valueOf(listPickingConfigurationCSVWrapper.getGroupRule())
        );

        listPickingConfiguration.setEnabled(listPickingConfigurationCSVWrapper.isEnabled());

        return listPickingConfiguration;

    }

    public List<ListPickingConfiguration> findMatchedListPickingConfiguration(Pick pick) {
        List<ListPickingConfiguration> listPickingConfigurations = findAll(pick.getWarehouseId());
        logger.debug("Start to find matched list picking configuration for pick: {} / from {} configuration",
                pick, listPickingConfigurations.size());

        return listPickingConfigurations.stream()
                .filter(listPickingConfiguration -> match(listPickingConfiguration,pick))
                .collect(Collectors.toList());
    }

    private boolean match(ListPickingConfiguration listPickingConfiguration, Pick pick) {
        logger.debug("Start to match list picking configuration {} with pick {}",
                listPickingConfiguration, pick);
        if (!listPickingConfiguration.getEnabled()) {
            logger.debug("{} is not enabled! Non match", listPickingConfiguration);
            return false;
        }

        if (Objects.nonNull(listPickingConfiguration.getWarehouseId())&&
                !listPickingConfiguration.getWarehouseId().equals(pick.getWarehouseId())) {
            logger.debug("The warehouse ID doesn't match!warehouse ID in configuration {} " +
                    "doesnt match with pick's warehouse id {}",
                    listPickingConfiguration.getWarehouseId(), pick.getWarehouseId());
            return false;
        }

        if (!listPickingConfiguration.getPickType().equals(pick.getPickType())) {

            logger.debug("The type doesn't match! type in configuration {} " +
                            "doesnt match with pick's type {}",
                    listPickingConfiguration.getPickType(), pick.getPickType());
            return false;
        }
        // If the configuraiton has client id defined, we will need to make sure
        // the pick's client id not null
        //
        if (Objects.nonNull(listPickingConfiguration.getClientId()) &&
                (Objects.isNull(pick.getClient())
                        || !pick.getClient().getId().equals(listPickingConfiguration.getClientId()))) {

            logger.debug("The client doesn't match! client id in configuration {} " +
                            "doesnt match with pick's client id {}",
                    listPickingConfiguration.getClientId(), pick.getClient());
            return false;
        }

        logger.debug(">> list picking configuraiton matches with the pick!");
        return true;

    }


}
