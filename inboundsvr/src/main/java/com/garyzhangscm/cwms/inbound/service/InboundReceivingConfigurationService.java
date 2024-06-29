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

import com.garyzhangscm.cwms.inbound.clients.CommonServiceRestemplateClient;
import com.garyzhangscm.cwms.inbound.clients.InventoryServiceRestemplateClient;
import com.garyzhangscm.cwms.inbound.clients.WarehouseLayoutServiceRestemplateClient;
import com.garyzhangscm.cwms.inbound.exception.ResourceNotFoundException;
import com.garyzhangscm.cwms.inbound.model.InboundQCConfiguration;
import com.garyzhangscm.cwms.inbound.model.InboundReceivingConfiguration;
import com.garyzhangscm.cwms.inbound.model.Receipt;
import com.garyzhangscm.cwms.inbound.model.Warehouse;
import com.garyzhangscm.cwms.inbound.repository.InboundQCConfigurationRepository;
import com.garyzhangscm.cwms.inbound.repository.InboundReceivingConfigurationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class InboundReceivingConfigurationService {
    private static final Logger logger = LoggerFactory.getLogger(InboundReceivingConfigurationService.class);

    @Autowired
    private InboundReceivingConfigurationRepository inboundReceivingConfigurationRepository;

    @Autowired
    private ReceiptService receiptService;

    @Autowired
    private InventoryServiceRestemplateClient inventoryServiceRestemplateClient;
    @Autowired
    private CommonServiceRestemplateClient commonServiceRestemplateClient;
    @Autowired
    private WarehouseLayoutServiceRestemplateClient warehouseLayoutServiceRestemplateClient;
    @Autowired
    private FileService fileService;


    public InboundReceivingConfiguration findById(Long id) {
        return findById(id, true);
    }

    public InboundReceivingConfiguration findById(Long id, boolean includeDetails) {
        InboundReceivingConfiguration inboundReceivingConfiguration = inboundReceivingConfigurationRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.raiseException("inbound receiving configuration not found by id: " + id));
        if (includeDetails) {
            loadAttribute(inboundReceivingConfiguration);
        }
        return inboundReceivingConfiguration;
    }
    public List<InboundReceivingConfiguration> findAll(Long supplierId,
                                                Long itemFamilyId,
                                                Long itemId,
                                                Long warehouseId, Long companyId) {
        return findAll(supplierId, itemFamilyId, itemId,
                warehouseId, companyId, true);
    }
    public List<InboundReceivingConfiguration> findAll(Long supplierId,
                                                Long itemFamilyId,
                                                Long itemId,
                                                Long warehouseId,
                                                Long companyId,
                                                boolean includeDetails) {

        List<InboundReceivingConfiguration> inboundReceivingConfigurations =
                inboundReceivingConfigurationRepository.findAll(
                (Root<InboundReceivingConfiguration> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) -> {
                    List<Predicate> predicates = new ArrayList<Predicate>();

                    predicates.add(criteriaBuilder.equal(root.get("companyId"), companyId));

                    if (Objects.nonNull(supplierId)) {
                        predicates.add(criteriaBuilder.equal(root.get("supplierId"), supplierId));

                    }
                    if (Objects.nonNull(itemFamilyId)) {
                        predicates.add(criteriaBuilder.equal(root.get("itemFamilyId"), itemFamilyId));

                    }
                    if (Objects.nonNull(itemId)) {
                        predicates.add(criteriaBuilder.equal(root.get("itemId"), itemId));

                    }

                    if (Objects.nonNull(warehouseId)) {
                        predicates.add(criteriaBuilder.equal(root.get("warehouseId"), warehouseId));

                    }

                    Predicate[] p = new Predicate[predicates.size()];
                    return criteriaBuilder.and(predicates.toArray(p));
                }
        );


        if (inboundReceivingConfigurations.size() > 0 && includeDetails) {
            loadAttribute(inboundReceivingConfigurations);
        }
        return inboundReceivingConfigurations;
    }


    public void loadAttribute(List<InboundReceivingConfiguration> inboundReceivingConfigurations) {
        for(InboundReceivingConfiguration inboundReceivingConfiguration : inboundReceivingConfigurations) {
            loadAttribute(inboundReceivingConfiguration);
        }
    }

    public void loadAttribute(InboundReceivingConfiguration inboundReceivingConfiguration) {

        if (inboundReceivingConfiguration.getSupplierId() != null && inboundReceivingConfiguration.getSupplier() == null) {
            inboundReceivingConfiguration.setSupplier(
                    commonServiceRestemplateClient.getSupplierById(
                            inboundReceivingConfiguration.getSupplierId()));

        }
        if (inboundReceivingConfiguration.getItemFamilyId() != null && inboundReceivingConfiguration.getItemFamily() == null) {
            inboundReceivingConfiguration.setItemFamily(
                    inventoryServiceRestemplateClient.getItemFamilyById(
                            inboundReceivingConfiguration.getItemFamilyId()));

        }
        if (inboundReceivingConfiguration.getItemId() != null && inboundReceivingConfiguration.getItem() == null) {
            inboundReceivingConfiguration.setItem(
                    inventoryServiceRestemplateClient.getItemById(
                            inboundReceivingConfiguration.getItemId()));

        }
        if (inboundReceivingConfiguration.getWarehouseId() != null && inboundReceivingConfiguration.getWarehouse() == null) {
            inboundReceivingConfiguration.setWarehouse(
                    warehouseLayoutServiceRestemplateClient.getWarehouseById(
                            inboundReceivingConfiguration.getWarehouseId()));

        }
        if (inboundReceivingConfiguration.getCompanyId() != null && inboundReceivingConfiguration.getCompany() == null) {
            inboundReceivingConfiguration.setCompany(
                    warehouseLayoutServiceRestemplateClient.getCompanyById(
                            inboundReceivingConfiguration.getCompanyId()));

        }

    }

    @Transactional
    public InboundReceivingConfiguration save(InboundReceivingConfiguration inboundReceivingConfiguration) {
        return inboundReceivingConfigurationRepository.save(inboundReceivingConfiguration);
    }

    @Transactional
    public InboundReceivingConfiguration saveOrUpdate(InboundReceivingConfiguration inboundReceivingConfiguration) {

        List<InboundReceivingConfiguration> inboundReceivingConfigurations = findAll(
                inboundReceivingConfiguration.getSupplierId(),
                inboundReceivingConfiguration.getItemFamilyId(),
                inboundReceivingConfiguration.getItemId(),
                inboundReceivingConfiguration.getWarehouseId(),
                inboundReceivingConfiguration.getCompanyId(),
                false
        );
        // see if we have an exact match
        inboundReceivingConfigurations.stream().filter(
            existingInboundQCConfiguration ->
                    Objects.equals(existingInboundQCConfiguration, inboundReceivingConfiguration)
        ).forEach(
            existingInboundQCConfiguration ->
                    inboundReceivingConfiguration.setId(
                        existingInboundQCConfiguration.getId()
                )
        );

        return save(inboundReceivingConfiguration);
    }


    @Transactional
    public void delete(InboundReceivingConfiguration inboundReceivingConfiguration) {
        inboundReceivingConfigurationRepository.delete(inboundReceivingConfiguration);
    }
    @Transactional
    public void delete(Long id) {
        inboundReceivingConfigurationRepository.deleteById(id);
    }


    public InboundReceivingConfiguration getBestMatchedInboundReceivingConfiguration(
            Receipt receipt
    ) {
        Warehouse warehouse = warehouseLayoutServiceRestemplateClient.getWarehouseById(
                receipt.getWarehouseId()
        );

        return getBestMatchedInboundReceivingConfiguration(receipt.getSupplierId(),
                null, null, receipt.getWarehouseId(), warehouse.getCompanyId());

    }
    public InboundReceivingConfiguration getBestMatchedInboundReceivingConfiguration(
                                                     Long supplierId,
                                                     Long itemFamilyId,
                                                     Long itemId,
                                                     Long warehouseId,
                                                     Long companyId) {
        logger.debug("start to get best matched inbound receiving configuration with " +
                        "\n supplier id: {}, item family id: {}, item id: {},   warehouse id: {}, company id: {}",
                supplierId, itemFamilyId, itemId, warehouseId, companyId);
        List<InboundReceivingConfiguration> allInboundReceivingConfiguration =
                findAll(null, null, null, null,
                         companyId, false);

        if (allInboundReceivingConfiguration.size() == 0) {
            logger.debug("Can't find any inbound receiving configuration");
            return null;
        }
        // we will get the best matched qc configuration based on the priority
        // 1. supplier + item
        // 2. supplier
        // 3. item
        // 4. warehouse id
        // 5. company id
        List<InboundReceivingConfiguration> matchedInboundReceivingConfiguration =
            allInboundReceivingConfiguration.stream().filter(
                    InboundReceivingConfiguration -> isMatch(
                            InboundReceivingConfiguration,
                                supplierId, itemFamilyId, itemId,
                                warehouseId, companyId
                        )
                ).collect(Collectors.toList());

        if (matchedInboundReceivingConfiguration.size() == 0) {
            logger.debug("Can't find any inbound receiving configuration matched with " +
                    "supplierId: {},itemFamilyId: {},itemId: {},  " +
                    "warehouseId: {}, companyId: {}",
                    supplierId, itemFamilyId,  itemId,
                    warehouseId, companyId);
            return null;
        }
        InboundReceivingConfiguration bestInboundReceivingConfiguration = matchedInboundReceivingConfiguration.get(0);
        for (InboundReceivingConfiguration inboundReceivingConfiguration : matchedInboundReceivingConfiguration) {
            logger.debug("inboundReceivingConfiguration {}'s priority: {}",
                    inboundReceivingConfiguration.getId(),
                    getPriority(inboundReceivingConfiguration));
            logger.debug("current bestInboundReceivingConfiguration {}'s priority: {}",
                    bestInboundReceivingConfiguration.getId(),
                    getPriority(bestInboundReceivingConfiguration));
            logger.debug("comparePriority(inboundReceivingConfiguration, bestInboundReceivingConfiguration): {}",
                    comparePriority(inboundReceivingConfiguration, bestInboundReceivingConfiguration));
            if (comparePriority(inboundReceivingConfiguration, bestInboundReceivingConfiguration) > 0) {
                bestInboundReceivingConfiguration = inboundReceivingConfiguration;
            }
        }
        logger.debug("bestInboundReceivingConfiguration: {} for supplier: {}， item {}, warehouse {}",
                bestInboundReceivingConfiguration.getId(),
                supplierId,
                itemId,
                warehouseId);
        return bestInboundReceivingConfiguration;
    }

    /**
     * Return 1 if the first one has high priority. Return 0 if both have the same priority
     * return -1 if the last one has high priority
     * Priority number is based off
     * 1. supplier + item
     * 2. supplier
     * 3. item
     * 4. warehouse id
     * 5. company id
     * low number means high priority
     * @param firstInboundReceivingConfiguration
     * @param secondInboundReceivingConfiguration
     * @return
     */
    private int comparePriority(InboundReceivingConfiguration firstInboundReceivingConfiguration,
                                InboundReceivingConfiguration secondInboundReceivingConfiguration) {
        return getPriority(secondInboundReceivingConfiguration).compareTo(
                getPriority(firstInboundReceivingConfiguration)
        );

    }

    /**
     * Get the priority of the configuration based off
     * 1. supplier + item
     * 2. supplier + item family
     * 3. supplier
     * 4. item
     * 5. item family
     * 6. warehouse id
     * 7. company id
     * @param inboundReceivingConfiguration
     * @return
     */
    private Integer getPriority(InboundReceivingConfiguration inboundReceivingConfiguration) {
        if (Objects.nonNull(inboundReceivingConfiguration.getSupplierId()) &&
                Objects.nonNull(inboundReceivingConfiguration.getItemId())) {
            return 1;
        }
        if (Objects.nonNull(inboundReceivingConfiguration.getSupplierId()) &&
                Objects.nonNull(inboundReceivingConfiguration.getItemFamilyId())) {
            return 2;
        }
        if (Objects.nonNull(inboundReceivingConfiguration.getSupplierId())) {
            return 3;
        }
        if (Objects.nonNull(inboundReceivingConfiguration.getItemId())) {
            return 4;

        }
        if (Objects.nonNull(inboundReceivingConfiguration.getItemFamilyId())) {
            return 5;

        }
        if (Objects.nonNull(inboundReceivingConfiguration.getWarehouseId())) {
            return 6;
        }
        return 5;
    }

    private boolean isMatch(InboundReceivingConfiguration inboundReceivingConfiguration,
                    Long supplierId,
                    Long itemFamilyId,
                    Long itemId,
                    Long warehouseId,
                    Long companyId) {
        if (Objects.nonNull(inboundReceivingConfiguration.getCompanyId()) &&
                !inboundReceivingConfiguration.getCompanyId().equals(companyId)) {
            logger.debug("company id in the receiving configuration is {}, company id being compared is {}, mis match",
                    inboundReceivingConfiguration.getCompanyId(),
                    companyId);
            return false;
        }


        if (Objects.nonNull(inboundReceivingConfiguration.getWarehouseId()) &&
                !inboundReceivingConfiguration.getWarehouseId().equals(warehouseId)) {
            logger.debug("warehouse id in the receiving configuration is {}, warehouse id being compared is {}, mis match",
                    inboundReceivingConfiguration.getWarehouseId(),
                    warehouseId);
            return false;
        }

        if (Objects.nonNull(inboundReceivingConfiguration.getItemFamilyId()) &&
                !inboundReceivingConfiguration.getItemFamilyId().equals(itemFamilyId)) {
            logger.debug("item family id in the receiving configuration is {}, item family id being compared is {}, mis match",
                    inboundReceivingConfiguration.getItemFamilyId(),
                    itemFamilyId);
            return false;
        }
        if (Objects.nonNull(inboundReceivingConfiguration.getItemId()) &&
                !inboundReceivingConfiguration.getItemId().equals(itemId)) {
            logger.debug("item id in the receiving configuration is {}, item id being compared is {}, mis match",
                    inboundReceivingConfiguration.getItemId(),
                    itemId);
            return false;
        }

        if (Objects.nonNull(inboundReceivingConfiguration.getSupplierId()) &&
                !inboundReceivingConfiguration.getSupplierId().equals(supplierId)
                ) {
            logger.debug("supplier id in the receiving configuration is {}, supplier id being compared is {}, mis match",
                    inboundReceivingConfiguration.getSupplierId(),
                    supplierId);
            return false;
        }

        logger.debug("receiving configuration id {} matches with " +
                        "supplierId: {},itemFamilyId: {},itemId: {} , " +
                        "warehouseId: {}, companyId: {}",
                inboundReceivingConfiguration.getId(),
                supplierId, itemFamilyId,  itemId,
                warehouseId, companyId);
        return true;
    }

    public InboundReceivingConfiguration addInboundReceivingConfiguration(
            InboundReceivingConfiguration inboundReceivingConfiguration) {
        return  saveOrUpdate(
                inboundReceivingConfiguration
        );
    }

    public InboundReceivingConfiguration changeInboundReceivingConfiguration(
            Long id, InboundReceivingConfiguration inboundReceivingConfiguration) {
        return  saveOrUpdate(
                inboundReceivingConfiguration
        );
    }

    public void handleItemOverride(Long warehouseId, Long oldItemId, Long newItemId) {
        logger.debug("start to process item override for inbound qc configuration, current warehouse {}, from item id {} to item id {}",
                warehouseId, oldItemId, newItemId);
        inboundReceivingConfigurationRepository.processItemOverride(oldItemId, newItemId, warehouseId);
    }

}
