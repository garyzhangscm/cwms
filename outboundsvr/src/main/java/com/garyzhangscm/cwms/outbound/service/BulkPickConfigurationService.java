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
import com.garyzhangscm.cwms.outbound.model.Order;
import com.garyzhangscm.cwms.outbound.repository.BulkPickConfigurationRepository;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;


@Service
public class BulkPickConfigurationService {
    private static final Logger logger = LoggerFactory.getLogger(BulkPickConfigurationService.class);

    @Autowired
    private BulkPickConfigurationRepository bulkPickConfigurationRepository;

    public BulkPickConfiguration findById(Long id) {
        return bulkPickConfigurationRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.raiseException("bulk picking configuration not found by id: " + id));

    }

    public List<BulkPickConfiguration> findAll(Long warehouseId,
                                               String pickType) {
        return bulkPickConfigurationRepository.findAll(
                (Root<BulkPickConfiguration> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) -> {
                    List<Predicate> predicates = new ArrayList<Predicate>();

                    predicates.add(criteriaBuilder.equal(root.get("warehouseId"), warehouseId));


                    if (Strings.isNotBlank(pickType)) {
                        predicates.add(criteriaBuilder.equal(root.get("pickType"), PickType.valueOf(pickType)));
                    }
                    Predicate[] p = new Predicate[predicates.size()];
                    return criteriaBuilder.and(predicates.toArray(p));
                }
        );
    }



    public BulkPickConfiguration findByWarehouseAndPickType(Long warehouseId,
                                                            PickType pickType) {
        return bulkPickConfigurationRepository.findByWarehouseIdAndPickType(warehouseId, pickType);
    }



    public BulkPickConfiguration save(BulkPickConfiguration bulkPickConfiguration) {
        return bulkPickConfigurationRepository.save(bulkPickConfiguration);
    }

    public BulkPickConfiguration saveOrUpdate(BulkPickConfiguration bulkPickConfiguration) {
        if (bulkPickConfiguration.getId() == null && findByWarehouseAndPickType(
                bulkPickConfiguration.getWarehouseId(), bulkPickConfiguration.getPickType()) != null) {
            bulkPickConfiguration.setId(findByWarehouseAndPickType(
                    bulkPickConfiguration.getWarehouseId(), bulkPickConfiguration.getPickType()).getId());
        }
        return save(bulkPickConfiguration);
    }


    public void delete(BulkPickConfiguration bulkPickConfiguration) {
        bulkPickConfigurationRepository.delete(bulkPickConfiguration);
    }

    public void delete(Long id) {
        bulkPickConfigurationRepository.deleteById(id);
    }



    public BulkPickConfiguration addBulkPickConfiguration(BulkPickConfiguration bulkPickConfiguration) {
        return saveOrUpdate(bulkPickConfiguration);
    }

    public BulkPickConfiguration changeBulkPickConfiguration(Long id, BulkPickConfiguration bulkPickConfiguration) {
        bulkPickConfiguration.setId(id);
        return saveOrUpdate(bulkPickConfiguration);

    }
}
