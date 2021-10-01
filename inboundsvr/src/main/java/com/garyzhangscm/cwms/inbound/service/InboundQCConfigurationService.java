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
                                                Long itemId,
                                                Long warehouseId, Long companyId) {
        return findAll(supplierId, itemId, warehouseId, companyId, true);
    }
    public List<InboundQCConfiguration> findAll(Long supplierId,
                                                Long itemId,
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

    }

    @Transactional
    public InboundQCConfiguration save(InboundQCConfiguration inboundQCConfiguration) {
        return inboundQCConfigurationRepository.save(inboundQCConfiguration);
    }

    @Transactional
    public InboundQCConfiguration saveOrUpdate(InboundQCConfiguration inboundQCConfiguration) {

        List<InboundQCConfiguration> inboundQCConfigurations = findAll(
                inboundQCConfiguration.getSupplierId(),
                inboundQCConfiguration.getItemId(),
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
                                                     Long itemId,
                                                     Long warehouseId,
                                                     Long companyId) {
        List<InboundQCConfiguration> matchedInboundQCConfiguration =
                findAll(null, null, null, companyId, false);

        if (matchedInboundQCConfiguration.size() == 0) {
            return null;
        }
        // we will get the best matched qc configuration based on the priority
        // 1. supplier
        // 2. item
        // 3. warehouse id
        // 4. company id
        Map<String, InboundQCConfiguration> inboundQCConfigurationMap =
                new HashMap<>();
        matchedInboundQCConfiguration.forEach(
                inboundQCConfiguration -> {
                    if (Objects.nonNull(inboundQCConfiguration.getSupplierId())) {
                        inboundQCConfigurationMap.put("supplier", inboundQCConfiguration);
                    }
                    else if (Objects.nonNull(inboundQCConfiguration.getItemId())) {
                        inboundQCConfigurationMap.put("item", inboundQCConfiguration);
                    }
                    else if (Objects.nonNull(inboundQCConfiguration.getWarehouseId())) {
                        inboundQCConfigurationMap.put("warehouse", inboundQCConfiguration);
                    }
                    else if (Objects.nonNull(inboundQCConfiguration.getCompanyId())) {
                        inboundQCConfigurationMap.put("company", inboundQCConfiguration);
                    }
                }
        );
        if (inboundQCConfigurationMap.containsKey("supplier")) {
            return inboundQCConfigurationMap.get("supplier");
        }
        else if (inboundQCConfigurationMap.containsKey("item")) {
            return inboundQCConfigurationMap.get("item");
        }
        else if (inboundQCConfigurationMap.containsKey("warehouse")) {
            return inboundQCConfigurationMap.get("warehouse");
        }
        else if (inboundQCConfigurationMap.containsKey("company")) {
            return inboundQCConfigurationMap.get("company");
        }
        return null;
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
