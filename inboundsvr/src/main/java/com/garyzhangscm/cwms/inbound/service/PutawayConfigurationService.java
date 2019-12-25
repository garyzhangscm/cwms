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

package com.garyzhangscm.cwms.inbound.service;

import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import com.garyzhangscm.cwms.inbound.clients.InventoryServiceRestemplateClient;
import com.garyzhangscm.cwms.inbound.clients.WarehouseLayoutServiceRestemplateClient;
import com.garyzhangscm.cwms.inbound.model.*;
import com.garyzhangscm.cwms.inbound.repository.PutawayConfigurationRepository;
import net.bytebuddy.asm.Advice;
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

@Service
public class PutawayConfigurationService implements TestDataInitiableService{
    private static final Logger logger = LoggerFactory.getLogger(PutawayConfigurationService.class);

    @Autowired
    private PutawayConfigurationRepository putawayConfigurationRepository;
    @Autowired
    private InventoryServiceRestemplateClient inventoryServiceRestemplateClient;
    @Autowired
    private WarehouseLayoutServiceRestemplateClient warehouseLayoutServiceRestemplateClient;

    @Autowired
    private FileService fileService;

    @Value("${fileupload.test-data.putaway_configuration:putaway_configuration.csv}")
    String testDataFile;

    public PutawayConfiguration findById(Long id) {
        return putawayConfigurationRepository.findById(id).orElse(null);
    }


    public List<PutawayConfiguration> findAll(Integer sequence,
                                              String itemName,
                                              String itemFamilyName,
                                              Long inventoryStatusId) {

        List<PutawayConfiguration> putawayConfigurations =  putawayConfigurationRepository.findAll(
                (Root<PutawayConfiguration> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) -> {
                    List<Predicate> predicates = new ArrayList<Predicate>();
                    if (sequence != null) {
                        predicates.add(criteriaBuilder.equal(root.get("sequence"), sequence));

                    }
                    if (!StringUtils.isBlank(itemName)) {
                        Item item = inventoryServiceRestemplateClient.getItemByName(itemName);
                        if (item != null) {
                            predicates.add(criteriaBuilder.equal(root.get("itemId"), item.getId()));
                        }
                        else {
                            // web client passed in a incorrect item name, we will return an empty result

                            predicates.add(criteriaBuilder.equal(root.get("itemId"), -1L));
                        }
                    }
                    if (!StringUtils.isBlank(itemFamilyName)) {
                        ItemFamily itemFamily = inventoryServiceRestemplateClient.getItemFamilyByName(itemFamilyName);
                        if (itemFamily != null) {
                            predicates.add(criteriaBuilder.equal(root.get("itemFamilyId"), itemFamily.getId()));
                        }
                        else {
                            // web client passed in a incorrect item family name, we will return an empty result
                            predicates.add(criteriaBuilder.equal(root.get("itemFamilyId"), -1L));
                        }
                    }
                    if (inventoryStatusId != null) {
                        predicates.add(criteriaBuilder.equal(root.get("inventoryStatusId"), inventoryStatusId));
                    }
                    Predicate[] p = new Predicate[predicates.size()];
                    return criteriaBuilder.and(predicates.toArray(p));
                }
        );

        if (putawayConfigurations.size() > 0) {
            loadLocationDetails(putawayConfigurations);
        }
        return putawayConfigurations;
    }

    private void loadLocationDetails(PutawayConfiguration putawayConfiguration) {

        if (putawayConfiguration.getItemId() != null && putawayConfiguration.getItem() == null) {
            putawayConfiguration.setItem(inventoryServiceRestemplateClient.getItemById(putawayConfiguration.getItemId()));
        }
        if (putawayConfiguration.getItemFamilyId() != null && putawayConfiguration.getItemFamily() == null) {
            putawayConfiguration.setItemFamily(inventoryServiceRestemplateClient.getItemFamilyById(putawayConfiguration.getItemFamilyId()));
        }

        if (putawayConfiguration.getInventoryStatusId() != null && putawayConfiguration.getInventoryStatus() == null) {
            putawayConfiguration.setInventoryStatus(inventoryServiceRestemplateClient.getInventoryStatusById(putawayConfiguration.getInventoryStatusId()));
        }


        if (putawayConfiguration.getLocationId() != null && putawayConfiguration.getLocation() == null) {
            putawayConfiguration.setLocation(warehouseLayoutServiceRestemplateClient.getLocationById(putawayConfiguration.getLocationId()));
        }
        if (putawayConfiguration.getLocationGroupId() != null && putawayConfiguration.getLocationGroup() == null) {
            putawayConfiguration.setLocationGroup(warehouseLayoutServiceRestemplateClient.getLocationGroupById(putawayConfiguration.getLocationGroupId()));
        }
        if (putawayConfiguration.getLocationGroupTypeId() != null && putawayConfiguration.getLocationGroupType() == null) {
            putawayConfiguration.setLocationGroupType(warehouseLayoutServiceRestemplateClient.getLocationGroupTypeById(putawayConfiguration.getLocationGroupTypeId()));
        }
    }

    private void loadLocationDetails(List<PutawayConfiguration> putawayConfigurations) {
        putawayConfigurations.forEach(putawayConfiguration -> loadLocationDetails(putawayConfiguration));
    }

    public PutawayConfiguration findBySequence(int sequence) {
        return putawayConfigurationRepository.findBySequence(sequence);
    }

    public PutawayConfiguration save(PutawayConfiguration putawayConfiguration) {
        return putawayConfigurationRepository.save(putawayConfiguration);
    }


    public PutawayConfiguration saveOrUpdate(PutawayConfiguration putawayConfiguration) {
        if (putawayConfiguration.getId() == null && findBySequence(putawayConfiguration.getSequence()) != null) {
            putawayConfiguration.setId(findBySequence(putawayConfiguration.getSequence()).getId());
        }
        return putawayConfigurationRepository.save(putawayConfiguration);
    }




    public void delete(PutawayConfiguration putawayConfiguration) {
        putawayConfigurationRepository.delete(putawayConfiguration);
    }
    public void delete(Long id) {
        putawayConfigurationRepository.deleteById(id);
    }
    public void delete(String putawayConfigurationIds) {
        if (!StringUtils.isBlank(putawayConfigurationIds)) {
            long[] putawayConfigurationIdArray = Arrays.asList(putawayConfigurationIds.split(",")).stream().mapToLong(Long::parseLong).toArray();
            for(long id : putawayConfigurationIdArray) {
                delete(id);
            }
        }
    }

    public List<PutawayConfigurationCSVWrapper> loadData(InputStream inputStream) throws IOException {

        CsvSchema schema = CsvSchema.builder().
                addColumn("sequence").
                addColumn("item").
                addColumn("itemFamily").
                addColumn("inventoryStatus").
                addColumn("location").
                addColumn("locationGroup").
                addColumn("locationGroupType").
                build().withHeader();

        return fileService.loadData(inputStream, schema, PutawayConfigurationCSVWrapper.class);
    }

    public void initTestData() {
        try {
            InputStream inputStream = new ClassPathResource(testDataFile).getInputStream();
            List<PutawayConfigurationCSVWrapper> putawayConfigurationCSVWrappers = loadData(inputStream);
            putawayConfigurationCSVWrappers.stream().forEach(putawayConfigurationCSVWrapper -> save(convertFromWrapper(putawayConfigurationCSVWrapper)));
        } catch (IOException ex) {
            logger.debug("Exception while load test data: {}", ex.getMessage());
        }
    }

    private PutawayConfiguration convertFromWrapper(PutawayConfigurationCSVWrapper putawayConfigurationCSVWrapper) {

        PutawayConfiguration putawayConfiguration = new PutawayConfiguration();
        putawayConfiguration.setSequence(putawayConfigurationCSVWrapper.getSequence());

        if (!StringUtils.isBlank(putawayConfigurationCSVWrapper.getItem())) {
            Item item = inventoryServiceRestemplateClient.getItemByName(putawayConfigurationCSVWrapper.getItem());
            if (item != null) {
                putawayConfiguration.setItemId(item.getId());
            }
        }
        if (!StringUtils.isBlank(putawayConfigurationCSVWrapper.getItemFamily())) {
            ItemFamily itemFamily = inventoryServiceRestemplateClient.getItemFamilyByName(putawayConfigurationCSVWrapper.getItemFamily());
            if (itemFamily != null) {
                putawayConfiguration.setItemFamilyId(itemFamily.getId());
            }
        }
        if (!StringUtils.isBlank(putawayConfigurationCSVWrapper.getInventoryStatus())) {
            InventoryStatus inventoryStatus = inventoryServiceRestemplateClient.getInventoryStatusByName(putawayConfigurationCSVWrapper.getInventoryStatus());
            if (inventoryStatus != null) {
                putawayConfiguration.setInventoryStatusId(inventoryStatus.getId());
            }
        }

        if (!StringUtils.isBlank(putawayConfigurationCSVWrapper.getLocation())) {
            Location location = warehouseLayoutServiceRestemplateClient.getLocationByName(putawayConfigurationCSVWrapper.getLocation());
            if (location != null) {
                putawayConfiguration.setLocationId(location.getId());
            }
        }

        if (!StringUtils.isBlank(putawayConfigurationCSVWrapper.getLocationGroup())) {
            LocationGroup locationGroup = warehouseLayoutServiceRestemplateClient.getLocationGroupByName(putawayConfigurationCSVWrapper.getLocationGroup());
            if (locationGroup != null) {
                putawayConfiguration.setLocationGroupId(locationGroup.getId());
            }
        }

        if (!StringUtils.isBlank(putawayConfigurationCSVWrapper.getLocationGroupType())) {
            LocationGroupType locationGroupType = warehouseLayoutServiceRestemplateClient.getLocationGroupTypeByName(putawayConfigurationCSVWrapper.getLocationGroupType());
            if (locationGroupType != null) {
                putawayConfiguration.setLocationGroupTypeId(locationGroupType.getId());
            }
        }
        return putawayConfiguration;
    }




}
