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

package com.garyzhangscm.cwms.layout.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import com.garyzhangscm.cwms.layout.clients.CommonServiceRestemplateClient;
import com.garyzhangscm.cwms.layout.clients.InboundServiceRestemplateClient;
import com.garyzhangscm.cwms.layout.clients.InventoryServiceRestemplateClient;
import com.garyzhangscm.cwms.layout.clients.OutboundServiceRestemplateClient;
import com.garyzhangscm.cwms.layout.exception.ResourceNotFoundException;
import com.garyzhangscm.cwms.layout.model.*;
import com.garyzhangscm.cwms.layout.repository.WarehouseConfigurationRepository;
import com.garyzhangscm.cwms.layout.repository.WarehouseRepository;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import javax.persistence.criteria.*;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
public class WarehouseConfigurationService   {

    private static final Logger logger = LoggerFactory.getLogger(WarehouseConfigurationService.class);

    @Autowired
    private WarehouseConfigurationRepository warehouseConfigurationRepository;


    public WarehouseConfiguration findById(Long id ) {
        return warehouseConfigurationRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.raiseException("warehouse configuration not found by id: " + id));

    }



    public List<WarehouseConfiguration> findAll(Long companyId,
                                               String companyCode,
                                               Long warehouseId,
                                               String warehouseName) {

        return warehouseConfigurationRepository.findAll(
                (Root<WarehouseConfiguration> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) -> {
                    List<Predicate> predicates = new ArrayList<Predicate>();

                    if (Objects.nonNull(companyId) || Objects.nonNull(warehouseId) ||
                            Strings.isNotBlank(companyCode) || Strings.isNotBlank(warehouseName)) {

                        Join<WarehouseConfiguration, Warehouse> joinWarehouse = root.join("warehouse", JoinType.INNER);
                        if (Objects.nonNull(warehouseId)) {
                            predicates.add(criteriaBuilder.equal(joinWarehouse.get("id"), warehouseId));

                        }
                        else if (Strings.isNotBlank(warehouseName)) {
                            predicates.add(criteriaBuilder.equal(joinWarehouse.get("name"), warehouseName));

                        }
                        if (Objects.nonNull(companyId) ||Strings.isNotBlank(companyCode)) {

                            Join<Warehouse, Company> joinCompany = joinWarehouse.join("company", JoinType.INNER);

                            if (Objects.nonNull(companyId)) {
                                predicates.add(criteriaBuilder.equal(joinCompany.get("id"), companyId));

                            }
                            else if (Strings.isNotBlank(companyCode)) {
                                predicates.add(criteriaBuilder.equal(joinCompany.get("code"), companyCode));

                            }
                        }

                    }
                    Predicate[] p = new Predicate[predicates.size()];
                    return criteriaBuilder.and(predicates.toArray(p));
                }
        );
    }


    public WarehouseConfiguration save(WarehouseConfiguration warehouseConfiguration) {
        return warehouseConfigurationRepository.save(warehouseConfiguration);
    }

    public WarehouseConfiguration findByWarehouse(Long warehouseId) {

        return warehouseConfigurationRepository.findByWarehouse(warehouseId);
    }

    public WarehouseConfiguration saveOrUpdate(WarehouseConfiguration warehouseConfiguration) {
        if (warehouseConfiguration.getId() == null && findByWarehouse(warehouseConfiguration.getWarehouse().getId()) != null) {
            warehouseConfiguration.setId(findByWarehouse(warehouseConfiguration.getWarehouse().getId()).getId());
        }
        return save(warehouseConfiguration);
    }

    public void delete(WarehouseConfiguration warehouseConfiguration) {
        warehouseConfigurationRepository.delete(warehouseConfiguration);
    }
    public void delete(Long id) {
        warehouseConfigurationRepository.deleteById(id);
    }


    public WarehouseConfiguration changeWarehouseConfiguration(Long companyId, WarehouseConfiguration warehouseConfiguration) {
        return saveOrUpdate(warehouseConfiguration);
    }
}
