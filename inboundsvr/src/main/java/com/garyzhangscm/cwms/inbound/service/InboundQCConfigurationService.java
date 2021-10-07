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
import com.garyzhangscm.cwms.inbound.clients.CommonServiceRestemplateClient;
import com.garyzhangscm.cwms.inbound.clients.InventoryServiceRestemplateClient;
import com.garyzhangscm.cwms.inbound.clients.WarehouseLayoutServiceRestemplateClient;
import com.garyzhangscm.cwms.inbound.exception.ReceiptOperationException;
import com.garyzhangscm.cwms.inbound.exception.ResourceNotFoundException;
import com.garyzhangscm.cwms.inbound.model.*;
import com.garyzhangscm.cwms.inbound.repository.InboundQCConfigurationRepository;
import com.garyzhangscm.cwms.inbound.repository.ReceiptLineRepository;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class InboundQCConfigurationService {
    private static final Logger logger = LoggerFactory.getLogger(InboundQCConfigurationService.class);

    @Autowired
    private InboundQCConfigurationRepository inboundQCConfigurationRepository;

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


    public InboundQCConfiguration findById(Long id) {
        return findById(id, true);
    }

    public InboundQCConfiguration findById(Long id, boolean includeDetails) {
        InboundQCConfiguration inboundQCConfiguration = inboundQCConfigurationRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.raiseException("inbound qc configuration not found by id: " + id));
        if (includeDetails) {
            loadReceiptLineAttribute(inboundQCConfiguration);
        }
        return inboundQCConfiguration;
    }
    public List<InboundQCConfiguration> findAll(Long supplierId,
                                                Long itemFamilyId,
                                                Long itemId,
                                                Long fromInventoryStatusId,
                                                Long warehouseId, Long companyId) {
        return findAll(supplierId, itemFamilyId, itemId, fromInventoryStatusId,
                warehouseId, companyId, true);
    }
    public List<InboundQCConfiguration> findAll(Long supplierId,
                                                Long itemFamilyId,
                                                Long itemId,
                                                Long fromInventoryStatusId,
                                                Long warehouseId,
                                                Long companyId,
                                                boolean includeDetails) {

        List<InboundQCConfiguration> inboundQCConfigurations =
            inboundQCConfigurationRepository.findAll(
                (Root<InboundQCConfiguration> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) -> {
                    List<Predicate> predicates = new ArrayList<Predicate>();

                    predicates.add(criteriaBuilder.equal(root.get("companyId"), companyId));

                    if (Objects.nonNull(supplierId)) {
                        predicates.add(criteriaBuilder.equal(root.get("supplierId"), supplierId));

                    }

                    if (Objects.nonNull(fromInventoryStatusId)) {
                        predicates.add(criteriaBuilder.equal(root.get("fromInventoryStatusId"), fromInventoryStatusId));

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


        if (inboundQCConfigurations.size() > 0 && includeDetails) {
            loadReceiptLineAttribute(inboundQCConfigurations);
        }
        return inboundQCConfigurations;
    }


    public void loadReceiptLineAttribute(List<InboundQCConfiguration> inboundQCConfigurations) {
        for(InboundQCConfiguration inboundQCConfiguration : inboundQCConfigurations) {
            loadReceiptLineAttribute(inboundQCConfiguration);
        }
    }

    public void loadReceiptLineAttribute(InboundQCConfiguration inboundQCConfiguration) {

        if (inboundQCConfiguration.getSupplierId() != null && inboundQCConfiguration.getSupplier() == null) {
            inboundQCConfiguration.setSupplier(
                    commonServiceRestemplateClient.getSupplierById(
                            inboundQCConfiguration.getSupplierId()));

        }
        if (inboundQCConfiguration.getItemFamilyId() != null && inboundQCConfiguration.getItemFamily() == null) {
            inboundQCConfiguration.setItemFamily(
                    inventoryServiceRestemplateClient.getItemFamilyById(
                            inboundQCConfiguration.getItemFamilyId()));

        }
        if (inboundQCConfiguration.getItemId() != null && inboundQCConfiguration.getItem() == null) {
            inboundQCConfiguration.setItem(
                    inventoryServiceRestemplateClient.getItemById(
                            inboundQCConfiguration.getItemId()));

        }
        if (inboundQCConfiguration.getWarehouseId() != null && inboundQCConfiguration.getWarehouse() == null) {
            inboundQCConfiguration.setWarehouse(
                    warehouseLayoutServiceRestemplateClient.getWarehouseById(
                            inboundQCConfiguration.getWarehouseId()));

        }
        if (inboundQCConfiguration.getCompanyId() != null && inboundQCConfiguration.getCompany() == null) {
            inboundQCConfiguration.setCompany(
                    warehouseLayoutServiceRestemplateClient.getCompanyById(
                            inboundQCConfiguration.getCompanyId()));

        }

        if (inboundQCConfiguration.getFromInventoryStatusId() != null && inboundQCConfiguration.getFromInventoryStatus() == null) {
            inboundQCConfiguration.setFromInventoryStatus(
                    inventoryServiceRestemplateClient.getInventoryStatusById(
                            inboundQCConfiguration.getFromInventoryStatusId()));

        }

        if (inboundQCConfiguration.getToInventoryStatusId() != null && inboundQCConfiguration.getToInventoryStatus() == null) {
            inboundQCConfiguration.setToInventoryStatus(
                    inventoryServiceRestemplateClient.getInventoryStatusById(
                            inboundQCConfiguration.getToInventoryStatusId()));

        }

    }

    @Transactional
    public InboundQCConfiguration save(InboundQCConfiguration inboundQCConfiguration) {
        return inboundQCConfigurationRepository.save(inboundQCConfiguration);
    }

    @Transactional
    public InboundQCConfiguration saveOrUpdate(InboundQCConfiguration inboundQCConfiguration) {

        List<InboundQCConfiguration> inboundQCConfigurations = findAll(
                inboundQCConfiguration.getSupplierId(),
                inboundQCConfiguration.getItemFamilyId(),
                inboundQCConfiguration.getItemId(),
                inboundQCConfiguration.getFromInventoryStatusId(),
                inboundQCConfiguration.getWarehouseId(),
                inboundQCConfiguration.getCompanyId(),
                false
        );
        // see if we have an exact match
        inboundQCConfigurations.stream().filter(
            existingInboundQCConfiguration ->
                    Objects.equals(existingInboundQCConfiguration, inboundQCConfiguration)
        ).forEach(
            existingInboundQCConfiguration ->
                inboundQCConfiguration.setId(
                        existingInboundQCConfiguration.getId()
                )
        );

        return save(inboundQCConfiguration);
    }


    @Transactional
    public void delete(InboundQCConfiguration inboundQCConfiguration) {
        inboundQCConfigurationRepository.delete(inboundQCConfiguration);
    }
    @Transactional
    public void delete(Long id) {
        inboundQCConfigurationRepository.deleteById(id);
    }

    public InboundQCConfiguration getBestMatchedInboundQCConfiguration(Long supplierId,
                                                                       Long itemFamilyId,
                                                                       Long itemId,
                                                                       Long warehouseId,
                                                                       Long companyId) {
        return getBestMatchedInboundQCConfiguration(
                supplierId, itemFamilyId, itemId,  null, warehouseId, companyId
        );

    }
    public InboundQCConfiguration getBestMatchedInboundQCConfiguration(
                                                     Long supplierId,
                                                     Long itemFamilyId,
                                                     Long itemId,
                                                     Long fromInventoryStatusId,
                                                     Long warehouseId,
                                                     Long companyId) {
        List<InboundQCConfiguration> allInboundQCConfiguration =
                findAll(null, null, null, null,
                        null, companyId, true);

        if (allInboundQCConfiguration.size() == 0) {
            logger.debug("Can't find any inbound QC configuration. Suppose we don't need QC");
            return null;
        }
        // we will get the best matched qc configuration based on the priority
        // 1. supplier + item
        // 2. supplier
        // 3. item
        // 4. warehouse id
        // 5. company id
        List<InboundQCConfiguration> matchedInboundQCConfiguration =
            allInboundQCConfiguration.stream().filter(
                        inboundQCConfiguration -> isMatch(
                                inboundQCConfiguration,
                                supplierId, itemFamilyId, itemId, fromInventoryStatusId,
                                warehouseId, companyId
                        )
                ).collect(Collectors.toList());

        if (matchedInboundQCConfiguration.size() == 0) {
            logger.debug("Can't find any inbound qc configuration matched with " +
                    "supplierId: {},itemFamilyId: {},itemId: {}, fromInventoryStatusId: {}, " +
                    "warehouseId: {}, companyId: {}",
                    supplierId, itemFamilyId,  itemId, fromInventoryStatusId,
                    warehouseId, companyId);
            return null;
        }
        InboundQCConfiguration bestInboundQCConfiguration = matchedInboundQCConfiguration.get(0);
        for (InboundQCConfiguration inboundQCConfiguration : matchedInboundQCConfiguration) {
            logger.debug("inboundQCConfiguration {}'s priority: {}",
                    inboundQCConfiguration.getId(),
                    getPriority(inboundQCConfiguration));
            logger.debug("current bestInboundQCConfiguration {}'s priority: {}",
                    bestInboundQCConfiguration.getId(),
                    getPriority(bestInboundQCConfiguration));
            logger.debug("comparePriority(inboundQCConfiguration, bestInboundQCConfiguration): {}",
                    comparePriority(inboundQCConfiguration, bestInboundQCConfiguration) );
            if (comparePriority(inboundQCConfiguration, bestInboundQCConfiguration) > 0) {
                bestInboundQCConfiguration = inboundQCConfiguration;
            }
        }
        logger.debug("bestInboundQCConfiguration: {} for supplier: {}， item {}, warehouse {}",
                bestInboundQCConfiguration.getId(),
                supplierId,
                itemId,
                warehouseId);
        return bestInboundQCConfiguration;
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
     * @param firstInboundQCConfiguration
     * @param secondInboundQCConfiguration
     * @return
     */
    private int comparePriority(InboundQCConfiguration firstInboundQCConfiguration, InboundQCConfiguration secondInboundQCConfiguration) {
        return getPriority(secondInboundQCConfiguration).compareTo(
                getPriority(firstInboundQCConfiguration)
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
     * @param inboundQCConfiguration
     * @return
     */
    private Integer getPriority(InboundQCConfiguration inboundQCConfiguration) {
        if (Objects.nonNull(inboundQCConfiguration.getSupplierId()) &&
                Objects.nonNull(inboundQCConfiguration.getItemId())) {
            return 1;
        }
        if (Objects.nonNull(inboundQCConfiguration.getSupplierId()) &&
                Objects.nonNull(inboundQCConfiguration.getItemFamilyId())) {
            return 2;
        }
        if (Objects.nonNull(inboundQCConfiguration.getSupplierId())) {
            return 3;
        }
        if (Objects.nonNull(inboundQCConfiguration.getItemId())) {
            return 4;

        }
        if (Objects.nonNull(inboundQCConfiguration.getItemFamilyId())) {
            return 5;

        }
        if (Objects.nonNull(inboundQCConfiguration.getWarehouseId())) {
            return 6;
        }
        return 5;
    }

    private boolean isMatch(InboundQCConfiguration inboundQCConfiguration,
                    Long supplierId,
                    Long itemFamilyId,
                    Long itemId,
                    Long fromInventoryStatusId,
                    Long warehouseId,
                    Long companyId) {
        if (Objects.nonNull(inboundQCConfiguration.getCompanyId()) &&
                !inboundQCConfiguration.getCompanyId().equals(companyId)) {
            return false;
        }


        if (Objects.nonNull(inboundQCConfiguration.getWarehouseId()) &&
                !inboundQCConfiguration.getWarehouseId().equals(warehouseId)) {
            return false;
        }

        if (Objects.nonNull(inboundQCConfiguration.getItemFamilyId()) &&
                !inboundQCConfiguration.getItemFamilyId().equals(itemFamilyId)) {
            return false;
        }
        if (Objects.nonNull(inboundQCConfiguration.getItemId()) &&
                !inboundQCConfiguration.getItemId().equals(itemId)) {
            return false;
        }

        if (Objects.nonNull(inboundQCConfiguration.getSupplierId()) &&
                !inboundQCConfiguration.getSupplierId().equals(supplierId)
                ) {
            return false;
        }

        // from inventory status is optional
        if (Objects.nonNull(fromInventoryStatusId) &&
                Objects.nonNull(inboundQCConfiguration.getFromInventoryStatusId()) &&
                !inboundQCConfiguration.getFromInventoryStatusId().equals(fromInventoryStatusId)
        ) {
            return false;
        }
        return true;
    }

    public InboundQCConfiguration addInboundQCConfiguration(
            InboundQCConfiguration inboundQCConfiguration) {
        return  saveOrUpdate(
                inboundQCConfiguration
        );
    }

    public InboundQCConfiguration changeInboundQCConfiguration(
            Long id, InboundQCConfiguration inboundQCConfiguration) {
        return  saveOrUpdate(
                inboundQCConfiguration
        );
    }
}
