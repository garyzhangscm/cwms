/**
 * Copyright 2019
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

package com.garyzhangscm.cwms.resources.service;

import com.garyzhangscm.cwms.resources.exception.ResourceNotFoundException;
import com.garyzhangscm.cwms.resources.model.RFAppVersion;
import com.garyzhangscm.cwms.resources.model.RFAppVersionByRFCode;
import com.garyzhangscm.cwms.resources.model.RFConfiguration;
import com.garyzhangscm.cwms.resources.repository.RFAppVersionRepository;
import com.garyzhangscm.cwms.resources.repository.RFConfigurationRepository;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class RFConfigurationService {
    private static final Logger logger = LoggerFactory.getLogger(RFConfigurationService.class);
    @Autowired
    private RFConfigurationRepository rfConfigurationRepository;


    public RFConfiguration findById(Long id) {
        RFConfiguration rfConfiguration =  rfConfigurationRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.raiseException("rf configuration not found by id: " + id));
        return rfConfiguration;
    }

    public List<RFConfiguration> findAll(Long warehouseId,
                                         String rfCode) {

        return rfConfigurationRepository.findAll(
                (Root<RFConfiguration> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) -> {
                    List<Predicate> predicates = new ArrayList<Predicate>();

                    predicates.add(criteriaBuilder.equal(root.get("warehouseId"), warehouseId));

                    if (Strings.isNotBlank(rfCode)) {
                        predicates.add(criteriaBuilder.equal(root.get("rfCode"), rfCode));
                    }
                    else {
                        // if rf code is not passed in , then return the global one
                        predicates.add(criteriaBuilder.isNull(root.get("rfCode")));

                    }
                    Predicate[] p = new Predicate[predicates.size()];
                    return criteriaBuilder.and(predicates.toArray(p));
                }
        );



    }

    public RFConfiguration save(RFConfiguration rfConfiguration) {

        return rfConfigurationRepository.save(rfConfiguration);
    }

    public RFConfiguration findByRFCode(Long warehouseId, String rfCode) {
        List<RFConfiguration> rfConfigurations = findAll(warehouseId, rfCode);
        if (!rfConfigurations.isEmpty())  {
            // find one specific for the rf
            return rfConfigurations.get(0);
        }
        // no configuration is defined for the rf code, find the global one
        rfConfigurations = findAll(warehouseId, null);
        if (!rfConfigurations.isEmpty())  {
            // find one specific for the rf
            return rfConfigurations.get(0);
        }
        return null;

    }

    public RFConfiguration saveOrUpdate(RFConfiguration rfConfiguration) {
        if (Objects.isNull(rfConfiguration.getId()) &&
                Objects.nonNull(findByRFCode(
                        rfConfiguration.getWarehouseId(), rfConfiguration.getRfCode()))) {
            rfConfiguration.setId(
                    findByRFCode(rfConfiguration.getWarehouseId(), rfConfiguration.getRfCode()).getId());
        }
        return save(rfConfiguration);
    }


    public RFConfiguration changeRFConfiguration(Long warehouseId, RFConfiguration rfConfiguration) {
        return saveOrUpdate(rfConfiguration);
    }
}
