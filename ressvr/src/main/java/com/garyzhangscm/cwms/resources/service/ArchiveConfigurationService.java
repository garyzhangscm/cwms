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

import com.garyzhangscm.cwms.resources.exception.EmailException;
import com.garyzhangscm.cwms.resources.exception.ResourceNotFoundException;
import com.garyzhangscm.cwms.resources.model.Alert;
import com.garyzhangscm.cwms.resources.model.AlertStatus;
import com.garyzhangscm.cwms.resources.model.AlertType;
import com.garyzhangscm.cwms.resources.model.ArchiveConfiguration;
import com.garyzhangscm.cwms.resources.repository.AlertRepository;
import com.garyzhangscm.cwms.resources.repository.ArchiveConfigurationRepository;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
public class ArchiveConfigurationService {
    private static final Logger logger = LoggerFactory.getLogger(ArchiveConfigurationService.class);
    @Autowired
    private ArchiveConfigurationRepository archiveConfigurationRepository;

    public ArchiveConfiguration findById(Long id) {
        ArchiveConfiguration archiveConfiguration =  archiveConfigurationRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.raiseException("Archive Configur not found by id: " + id));
        return archiveConfiguration;
    }


    public List<ArchiveConfiguration> findAll(Long warehouseId) {

        return archiveConfigurationRepository.findAll(
                (Root<ArchiveConfiguration> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) -> {
                    List<Predicate> predicates = new ArrayList<Predicate>();

                    predicates.add(criteriaBuilder.equal(root.get("warehouseId"), warehouseId));

                    Predicate[] p = new Predicate[predicates.size()];
                    return criteriaBuilder.and(predicates.toArray(p));
                },
                Sort.by(Sort.Direction.DESC, "createdTime")

        );
    }

    public ArchiveConfiguration findByWarehouse(Long warehouseId) {

        return archiveConfigurationRepository.findByWarehouseId(warehouseId);
    }



    public ArchiveConfiguration save(ArchiveConfiguration archiveConfiguration) {
        return archiveConfigurationRepository.save(archiveConfiguration);
    }



    public ArchiveConfiguration saveOrUpdate(ArchiveConfiguration archiveConfiguration) {
        if (Objects.isNull(archiveConfiguration.getId()) &&
                !Objects.isNull(findByWarehouse(
                        archiveConfiguration.getWarehouseId()))) {
            archiveConfiguration.setId(
                    findByWarehouse(archiveConfiguration.getWarehouseId()).getId());
        }
        return save(archiveConfiguration);
    }


    public ArchiveConfiguration addArchiveConfiguration(ArchiveConfiguration archiveConfiguration) {
        return saveOrUpdate(archiveConfiguration);

    }

    public ArchiveConfiguration changeArchiveConfiguration(Long id, ArchiveConfiguration archiveConfiguration) {
        archiveConfiguration.setId(id);
        return saveOrUpdate(archiveConfiguration);

    }

    public void delete(Long id) {
        archiveConfigurationRepository.deleteById(id);

    }
}
