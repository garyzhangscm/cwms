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
import com.garyzhangscm.cwms.outbound.repository.ListPickConfigurationRepository;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.util.Strings;
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
public class ListPickConfigurationService implements TestDataInitiableService {
    private static final Logger logger = LoggerFactory.getLogger(ListPickConfigurationService.class);

    @Autowired
    private ListPickConfigurationRepository listPickConfigurationRepository;


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

    public ListPickConfiguration findById(Long id, boolean loadDetails) {
        ListPickConfiguration listPickConfiguration
                = listPickConfigurationRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.raiseException("list picking configuration not found by id: " + id));
        if (loadDetails) {
            loadAttribute(listPickConfiguration);
        }
        return listPickConfiguration;
    }

    public ListPickConfiguration findBySequence(int sequence) {
        return findBySequence(sequence, true);
    }


    public ListPickConfiguration findBySequence(int sequence, boolean loadDetails) {
        ListPickConfiguration listPickConfiguration = listPickConfigurationRepository.findBySequence(sequence);
        if (listPickConfiguration != null && loadDetails) {
            loadAttribute(listPickConfiguration);
        }
        return listPickConfiguration;
    }

    public ListPickConfiguration findById(Long id) {
        return findById(id, true);
    }


    public List<ListPickConfiguration> findAll(boolean loadDetails) {
        List<ListPickConfiguration> listPickConfigurations = listPickConfigurationRepository.findAll();

        if (listPickConfigurations.size() > 0 && loadDetails) {
            loadAttribute(listPickConfigurations);
        }
        return listPickConfigurations;
    }

    public List<ListPickConfiguration> findAll() {
        return findAll(true);
    }

    public List<ListPickConfiguration> findAll(Long warehouseId,
                                               Long clientId,
                                               String clientName,
                                               Long customerId,
                                               String customerName) {
        return findAll(warehouseId, clientId, clientName,
                customerId, customerName, true);
    }

    public List<ListPickConfiguration> findAll(Long warehouseId,
                                               Long clientId,
                                               String clientName,
                                               Long customerId,
                                               String customerName,
                                               boolean loadDetails) {
        List<ListPickConfiguration> listPickConfigurations =  listPickConfigurationRepository.findAll(
                (Root<ListPickConfiguration> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) -> {
                    List<Predicate> predicates = new ArrayList<Predicate>();


                    predicates.add(criteriaBuilder.equal(root.get("warehouseId"), warehouseId));

                    if (Objects.nonNull(clientId)) {

                        predicates.add(criteriaBuilder.equal(root.get("clientId"), clientId));
                    }
                    if (Strings.isNotBlank(clientName)) {
                        Client client = commonServiceRestemplateClient.getClientByName(
                                warehouseId, clientName
                        );

                        if (Objects.nonNull(client)) {
                            predicates.add(criteriaBuilder.equal(root.get("clientId"), client.getId()));
                        }
                        else {
                            predicates.add(criteriaBuilder.equal(root.get("clientId"), -1));
                        }
                    }

                    if (Objects.nonNull(customerId)) {

                        predicates.add(criteriaBuilder.equal(root.get("customerId"), customerId));
                    }
                    if (Strings.isNotBlank(customerName)) {
                        Customer customer = commonServiceRestemplateClient.getCustomerByName(
                                null, warehouseId, customerName
                        );

                        if (Objects.nonNull(customer)) {
                            predicates.add(criteriaBuilder.equal(root.get("customerId"), customer.getId()));
                        }
                        else {
                            predicates.add(criteriaBuilder.equal(root.get("customerId"), -1));
                        }
                    }
                    Predicate[] p = new Predicate[predicates.size()];
                    return criteriaBuilder.and(predicates.toArray(p));
                }
        );
        logger.debug("## Find {} list picking configuration by warehouse Id {}",
                listPickConfigurations.size(), warehouseId);

        if (listPickConfigurations.size() > 0 && loadDetails) {
            loadAttribute(listPickConfigurations);
        }
        return listPickConfigurations;
    }


    private void loadAttribute(ListPickConfiguration listPickConfiguration) {

        if (Objects.nonNull(listPickConfiguration.getWarehouseId()) &&
                Objects.isNull(listPickConfiguration.getWarehouse())) {
            listPickConfiguration.setWarehouse(warehouseLayoutServiceRestemplateClient.getWarehouseById(listPickConfiguration.getWarehouseId()));
        }
        try {
            if (Objects.nonNull(listPickConfiguration.getCustomerId()) &&
                    Objects.isNull(listPickConfiguration.getCustomer())) {
                listPickConfiguration.setCustomer(commonServiceRestemplateClient.getCustomerById(listPickConfiguration.getCustomerId()));
            }
        }
        catch (Exception ex) {}
    }

    private void loadAttribute(List<ListPickConfiguration> listPickConfigurations) {
        listPickConfigurations.forEach(listPickingConfiguration -> loadAttribute(listPickingConfiguration));
    }

    public ListPickConfiguration save(ListPickConfiguration listPickConfiguration) {
        return listPickConfigurationRepository.save(listPickConfiguration);
    }

    public ListPickConfiguration saveOrUpdate(ListPickConfiguration listPickConfiguration) {
        if (listPickConfiguration.getId() == null && findBySequence(listPickConfiguration.getSequence()) != null) {
            listPickConfiguration.setId(findBySequence(listPickConfiguration.getSequence()).getId());
        }
        return save(listPickConfiguration);
    }


    public void delete(ListPickConfiguration listPickConfiguration) {
        listPickConfigurationRepository.delete(listPickConfiguration);
    }

    public void delete(Long id) {
        listPickConfigurationRepository.deleteById(id);
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
                addColumn("company").
                addColumn("sequence").
                addColumn("warehouse").
                addColumn("client").
                addColumn("pickType").
                addColumn("groupRule").
                addColumn("enabled").
                build().withHeader();

        return fileService.loadData(inputStream, schema, ListPickingConfigurationCSVWrapper.class);
    }

    public void initTestData(Long companyId, String warehouseName) {
        try {

            String companyCode = warehouseLayoutServiceRestemplateClient.getCompanyById(companyId).getCode();

            String testDataFileName = StringUtils.isBlank(warehouseName) ?
                    testDataFile + ".csv" :
                    testDataFile + "-" + companyCode + "-" + warehouseName + ".csv";
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

    private ListPickConfiguration convertFromWrapper(ListPickingConfigurationCSVWrapper listPickingConfigurationCSVWrapper) {

        ListPickConfiguration listPickConfiguration = new ListPickConfiguration();
        listPickConfiguration.setSequence(listPickingConfigurationCSVWrapper.getSequence());
        listPickConfiguration.setPickType(PickType.valueOf(listPickingConfigurationCSVWrapper.getPickType()));

            Warehouse warehouse = warehouseLayoutServiceRestemplateClient.getWarehouseByName(
                    listPickingConfigurationCSVWrapper.getCompany(),
                    listPickingConfigurationCSVWrapper.getWarehouse()
            );

/**
        listPickingConfiguration.setGroupRule(
                ListPickingGroupRuleType.valueOf(listPickingConfigurationCSVWrapper.getGroupRule())
        );
**/
        listPickConfiguration.setEnabled(listPickingConfigurationCSVWrapper.isEnabled());

        return listPickConfiguration;

    }

    public List<ListPickConfiguration> findMatchedListPickConfiguration(Pick pick) {
        List<ListPickConfiguration> listPickConfigurations = findAll(pick.getWarehouseId(),
                null, null, null, null);
        logger.debug("Start to find matched list picking configuration for pick: {} / from {} configuration",
                pick.getNumber(), listPickConfigurations.size());

        return listPickConfigurations.stream()
                .filter(listPickingConfiguration -> match(listPickingConfiguration,pick))
                .collect(Collectors.toList());
    }

    public List<ListPickConfiguration> findMatchedListPickConfiguration(String waveNumber, Pick pick) {
        List<ListPickConfiguration> listPickConfigurations = findAll(pick.getWarehouseId(),
                null, null, null, null);
        logger.debug("Start to find matched list picking configuration for pick: {} / from {} configuration",
                pick.getNumber(), listPickConfigurations.size());

        return listPickConfigurations.stream()
                .filter(listPickingConfiguration -> match(listPickingConfiguration,pick))
                .collect(Collectors.toList());
    }

    private boolean match(ListPickConfiguration listPickConfiguration, Pick pick) {
        logger.debug("Start to match list picking configuration {} with pick {}",
                listPickConfiguration.getId(), pick.getNumber());
        if (!listPickConfiguration.getEnabled()) {
            logger.debug("{} is not enabled! Non match", listPickConfiguration);
            return false;
        }

        if (Objects.nonNull(listPickConfiguration.getWarehouseId())&&
                !listPickConfiguration.getWarehouseId().equals(pick.getWarehouseId())) {
            logger.debug("The warehouse ID doesn't match!warehouse ID in configuration {} " +
                    "doesnt match with pick's warehouse id {}",
                    listPickConfiguration.getWarehouseId(), pick.getWarehouseId());
            return false;
        }

        if (!listPickConfiguration.getPickType().equals(pick.getPickType())) {

            logger.debug("The type doesn't match! type in configuration {} " +
                            "doesnt match with pick's type {}",
                    listPickConfiguration.getPickType(), pick.getPickType());
            return false;
        }
        logger.debug(">> list picking configuraiton matches with the pick!");
        return true;

    }


    public ListPickConfiguration addListPickConfiguration(ListPickConfiguration listPickConfiguration) {
        listPickConfiguration.getGroupRules().forEach(
                groupRule -> groupRule.setListPickConfiguration(
                        listPickConfiguration
                )
        );
        return saveOrUpdate(listPickConfiguration);
    }

    public ListPickConfiguration changeListPickConfiguration(Long id, ListPickConfiguration listPickConfiguration) {
        listPickConfiguration.setId(id);
        listPickConfiguration.getGroupRules().forEach(
                groupRule -> groupRule.setListPickConfiguration(
                        listPickConfiguration
                )
        );
        return saveOrUpdate(listPickConfiguration);

    }
}
