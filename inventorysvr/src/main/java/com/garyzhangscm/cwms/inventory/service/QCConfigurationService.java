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

import com.garyzhangscm.cwms.inventory.exception.ResourceNotFoundException;
import com.garyzhangscm.cwms.inventory.model.*;
import com.garyzhangscm.cwms.inventory.repository.QCConfigurationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import java.util.*;

@Service
public class QCConfigurationService {
    private static final Logger logger = LoggerFactory.getLogger(QCConfigurationService.class);

    @Autowired
    private QCConfigurationRepository qcConfigurationRepository;


    public QCConfiguration findById(Long id ) {
        return qcConfigurationRepository.findById(id)
                     .orElseThrow(() -> ResourceNotFoundException.raiseException("QC Rule not found by id: " + id));

    }

    public List<QCConfiguration> findAll(Long warehouseId) {

        return qcConfigurationRepository.findAll(
            (Root<QCConfiguration> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) -> {
                List<Predicate> predicates = new ArrayList<Predicate>();

                predicates.add(criteriaBuilder.equal(root.get("warehouseId"), warehouseId));

                Predicate[] p = new Predicate[predicates.size()];
                return criteriaBuilder.and(predicates.toArray(p));
            }
        );
    }

    public QCConfiguration getQCConfiguration(Long warehouseId) {
        List<QCConfiguration> qcConfigurations = findAll(warehouseId);
        if (qcConfigurations.size() > 0) {
            return qcConfigurations.get(0);
        }
        return null;
    }


    public QCConfiguration save(QCConfiguration qcConfiguration) {
        return qcConfigurationRepository.save(qcConfiguration);
    }

    public QCConfiguration saveOrUpdate(QCConfiguration qcConfiguration) {
        if (Objects.isNull(qcConfiguration.getId()) &&
                Objects.nonNull(getQCConfiguration(qcConfiguration.getWarehouseId()))) {
            qcConfiguration.setId(
                    getQCConfiguration(qcConfiguration.getWarehouseId()).getId()
            );
        }
        return qcConfigurationRepository.save(qcConfiguration);
    }



    public void delete(QCConfiguration qcConfiguration) {
        qcConfigurationRepository.delete(qcConfiguration);
    }
    public void delete(Long id) {
        qcConfigurationRepository.deleteById(id);
    }


    public QCConfiguration addQCConfiguration(QCConfiguration qcConfiguration) {
        return saveOrUpdate(qcConfiguration);

    }

    public QCConfiguration changeQCConfiguration(Long id, QCConfiguration qcConfiguration) {
        return saveOrUpdate(qcConfiguration);
    }
}
