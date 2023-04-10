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

import com.garyzhangscm.cwms.outbound.exception.ResourceNotFoundException;
import com.garyzhangscm.cwms.outbound.model.BulkPickConfiguration;
import com.garyzhangscm.cwms.outbound.model.PickConfiguration;
import com.garyzhangscm.cwms.outbound.model.PickType;
import com.garyzhangscm.cwms.outbound.repository.BulkPickConfigurationRepository;
import com.garyzhangscm.cwms.outbound.repository.PickConfigurationRepository;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.List;


@Service
public class PickConfigurationService {
    private static final Logger logger = LoggerFactory.getLogger(PickConfigurationService.class);

    @Autowired
    private PickConfigurationRepository pickConfigurationRepository;

    public PickConfiguration findById(Long id) {
        return pickConfigurationRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.raiseException("picking configuration not found by id: " + id));

    }



    public PickConfiguration findByWarehouse(Long warehouseId) {
        return pickConfigurationRepository.findByWarehouseId(warehouseId);
    }



    public PickConfiguration save(PickConfiguration pickConfiguration) {
        return pickConfigurationRepository.save(pickConfiguration);
    }

    public PickConfiguration saveOrUpdate(PickConfiguration pickConfiguration) {
        if (pickConfiguration.getId() == null && findByWarehouse(
                pickConfiguration.getWarehouseId()) != null) {
            pickConfiguration.setId(findByWarehouse(
                    pickConfiguration.getWarehouseId()).getId());
        }
        return save(pickConfiguration);
    }


    public void delete(PickConfiguration pickConfiguration) {
        pickConfigurationRepository.delete(pickConfiguration);
    }

    public void delete(Long id) {
        pickConfigurationRepository.deleteById(id);
    }



    public PickConfiguration addPickConfiguration(PickConfiguration pickConfiguration) {
        return saveOrUpdate(pickConfiguration);
    }

    public PickConfiguration changePickConfiguration(Long id, PickConfiguration pickConfiguration) {
        pickConfiguration.setId(id);
        return saveOrUpdate(pickConfiguration);

    }
}
