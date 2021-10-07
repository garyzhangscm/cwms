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

package com.garyzhangscm.cwms.inventory.service;

import com.garyzhangscm.cwms.inventory.clients.CommonServiceRestemplateClient;
import com.garyzhangscm.cwms.inventory.clients.WarehouseLayoutServiceRestemplateClient;
import com.garyzhangscm.cwms.inventory.exception.ResourceNotFoundException;
import com.garyzhangscm.cwms.inventory.model.*;
import com.garyzhangscm.cwms.inventory.repository.QCRuleConfigurationRepository;
import com.garyzhangscm.cwms.inventory.repository.QCRuleRepository;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.persistence.criteria.*;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class QCRuleConfigurationService {
    private static final Logger logger = LoggerFactory.getLogger(QCRuleConfigurationService.class);

    @Autowired
    private QCRuleConfigurationRepository qcRuleConfigurationRepository;
    @Autowired
    private QCRuleService qcRuleService;

    @Autowired
    private CommonServiceRestemplateClient commonServiceRestemplateClient;
    @Autowired
    private WarehouseLayoutServiceRestemplateClient warehouseLayoutServiceRestemplateClient;

    public QCRuleConfiguration findById(Long id) {
        return findById(id, true);

    }

    public QCRuleConfiguration findById(Long id, boolean loadDetails) {
        QCRuleConfiguration qcRuleConfiguration =
                qcRuleConfigurationRepository.findById(id)
                     .orElseThrow(() -> ResourceNotFoundException.raiseException("QC Rule not found by id: " + id));

        if (loadDetails) {
            loadAttribute(qcRuleConfiguration);
        }
        return qcRuleConfiguration;
    }

    private void loadAttribute(List<QCRuleConfiguration> qcRuleConfigurationList) {
        qcRuleConfigurationList.forEach(
                qcRuleConfiguration -> loadAttribute(qcRuleConfiguration)
        );

    }
    private void loadAttribute(QCRuleConfiguration qcRuleConfiguration) {
        if (Objects.nonNull(qcRuleConfiguration.getSupplierId()) &&
                Objects.isNull(qcRuleConfiguration.getSupplier())) {
            qcRuleConfiguration.setSupplier(
                    commonServiceRestemplateClient.getSupplierById(
                            qcRuleConfiguration.getSupplierId()
                    )
            );
        }
    }


    public List<QCRuleConfiguration> findAll(Long warehouseId,
                                             Long itemId,
                                             Long inventoryStatusId,
                                             Long supplierId) {
        return findAll(warehouseId, itemId, inventoryStatusId, supplierId, true);
    }
    public List<QCRuleConfiguration> findAll(Long warehouseId,
                                            Long itemId,
                                            Long inventoryStatusId,
                                            Long supplierId,
                                             boolean loadDetails) {

        List<QCRuleConfiguration> qcRuleConfigurations =
                qcRuleConfigurationRepository.findAll(
            (Root<QCRuleConfiguration> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) -> {
                List<Predicate> predicates = new ArrayList<Predicate>();

                predicates.add(criteriaBuilder.equal(root.get("warehouseId"), warehouseId));

                if (Objects.nonNull(itemId)) {

                    Join<QCRuleConfiguration, Item> joinItem = root.join("item", JoinType.INNER);
                    predicates.add(criteriaBuilder.equal(joinItem.get("id"), itemId));
                }
                if (Objects.nonNull(inventoryStatusId)) {

                    Join<QCRuleConfiguration, InventoryStatus> joinInventoryStatus = root.join("inventoryStatus", JoinType.INNER);
                    predicates.add(criteriaBuilder.equal(joinInventoryStatus.get("id"), inventoryStatusId));
                }
                if (Objects.nonNull(supplierId)) {

                    predicates.add(criteriaBuilder.equal(root.get("supplierId"), supplierId));
                }



                Predicate[] p = new Predicate[predicates.size()];
                return criteriaBuilder.and(predicates.toArray(p));
            }
        );
        if (qcRuleConfigurations.size() > 0 && loadDetails) {
            loadAttribute(qcRuleConfigurations);
        }
        return qcRuleConfigurations;

    }



    public QCRuleConfiguration save(QCRuleConfiguration qcRuleConfiguration) {
        return qcRuleConfigurationRepository.save(qcRuleConfiguration);
    }


    public void delete(QCRuleConfiguration qcRuleConfiguration) {
        qcRuleConfigurationRepository.delete(qcRuleConfiguration);
    }
    public void delete(Long id) {
        qcRuleConfigurationRepository.deleteById(id);
    }


    public QCRuleConfiguration addQCRuleConfiguration(QCRuleConfiguration qcRuleConfiguration) {

        // Save the configuration without rule first
        // since we are adding new configuration, the configuration doesn't have an ID yet.
        // configuration / rule are many to many relationship so we need both
        // configuration and rule having the ID so that the relationship can be
        // persist in the qc_rule_configuration_rule table
        // So we will save the configuration without rule,
        // then attach all the rules to the saved configuration and persist the
        // relationship again
        Set<QCRule> qcRules = qcRuleConfiguration.getQcRules();
        qcRuleConfiguration.setQcRules(new HashSet<>());
        QCRuleConfiguration newQCRuleConfiguration = save(qcRuleConfiguration);


        qcRules.forEach(qcRule -> {
            QCRule newQCRule = qcRuleService.findById(qcRule.getId());
            newQCRuleConfiguration.assignQCRule(newQCRule);
        });

        return save(newQCRuleConfiguration);
    }

    public QCRuleConfiguration changeQCRuleConfiguration(Long id, QCRuleConfiguration qcRuleConfiguration) {
        qcRuleConfiguration.setId(id);

        // Save the configuration without rule first
        // since we are adding new configuration, the configuration doesn't have an ID yet.
        // configuration / rule are many to many relationship so we need both
        // configuration and rule having the ID so that the relationship can be
        // persist in the qc_rule_configuration_rule table
        // So we will save the configuration without rule,
        // then attach all the rules to the saved configuration and persist the
        // relationship again
        Set<QCRule> qcRules = qcRuleConfiguration.getQcRules();
        qcRuleConfiguration.setQcRules(new HashSet<>());
        QCRuleConfiguration newQCRuleConfiguration = save(qcRuleConfiguration);


        qcRules.forEach(qcRule -> {
            QCRule newQCRule = qcRuleService.findById(qcRule.getId());
            newQCRuleConfiguration.assignQCRule(newQCRule);
        });

        return save(newQCRuleConfiguration);
    }

    public List<QCRuleConfiguration> findAllMatchedQCRuleConfiguration(Supplier supplier, Inventory inventory) {
        List<QCRuleConfiguration> qcRuleConfigurations = findAll(
                inventory.getWarehouseId(), null, null, null, false
        );
        logger.debug("We have {} qc rule configuration defined",
                qcRuleConfigurations.size());
        if (qcRuleConfigurations.size() == 0) {
            return new ArrayList<>();
        }
        return qcRuleConfigurations.stream().filter(
                        qcRuleConfiguration -> isMatch(qcRuleConfiguration,
                                supplier, inventory)
                ).collect(Collectors.toList());
    }
    public QCRuleConfiguration findBestMatchedQCRuleConfiguration(Supplier supplier, Inventory inventory) {

        List<QCRuleConfiguration> matchedQCRuleConfigurations =
                findAllMatchedQCRuleConfiguration(supplier, inventory);

        logger.debug("We have {} matched qc rule configuration defined" +
                     ", based on the criteria, supplier: {} / {}, lpn: {}, item: {} / {}" +
                ", item family: {} / {}, inventory status {} / {}",
                matchedQCRuleConfigurations.size(),
                Objects.isNull(supplier) ? "" : supplier.getId(),
                Objects.isNull(supplier) ? "" : supplier.getName(),
                Objects.isNull(inventory) ? "" : inventory.getLpn(),
                Objects.isNull(inventory) ? "" : inventory.getItem().getId(),
                Objects.isNull(inventory) ? "" : inventory.getItem().getName(),
                Objects.isNull(inventory) ? "" :
                        Objects.isNull(inventory.getItem().getItemFamily())? "" : inventory.getItem().getItemFamily().getId(),
                Objects.isNull(inventory) ? "" :
                        Objects.isNull(inventory.getItem().getItemFamily())? "" : inventory.getItem().getItemFamily().getName(),
                Objects.isNull(inventory) ?  "" : inventory.getInventoryStatus().getId(),
                Objects.isNull(inventory) ?  "" : inventory.getInventoryStatus().getName());

        if (matchedQCRuleConfigurations.size() == 0) {
            logger.debug("Can't find any matched QC rules based on the criteria");
            return null;
        }
        Collections.sort(matchedQCRuleConfigurations, (Comparator.comparing(QCRuleConfiguration::getSequence)));

        return matchedQCRuleConfigurations.get(0);

    }

    private boolean isMatch(QCRuleConfiguration qcRuleConfiguration, Supplier supplier, Inventory inventory) {

        if (Objects.nonNull(qcRuleConfiguration.getWarehouseId()) &&
                !qcRuleConfiguration.getWarehouseId().equals(inventory.getWarehouseId())) {
            return false;
        }

        // supplier may be null
        if (Objects.nonNull(qcRuleConfiguration.getSupplierId()) &&
                (
                    Objects.isNull(supplier) ||
                       !qcRuleConfiguration.getSupplierId().equals(supplier.getId())
                )) {
            return false;
        }

        if (Objects.nonNull(qcRuleConfiguration.getItem()) &&
                !qcRuleConfiguration.getItem().getId().equals(inventory.getItem().getId())) {
            return false;
        }

        if (Objects.nonNull(qcRuleConfiguration.getItemFamily()) &&
                !qcRuleConfiguration.getItemFamily().getId().equals(inventory.getItem().getItemFamily().getId())) {
            return false;
        }

        if (Objects.nonNull(qcRuleConfiguration.getInventoryStatus()) &&
                !qcRuleConfiguration.getInventoryStatus().getId().equals(inventory.getInventoryStatus().getId())) {
            return false;
        }
        return true;
    }



}
