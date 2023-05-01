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
import com.garyzhangscm.cwms.outbound.model.PickType;
import com.garyzhangscm.cwms.outbound.model.hualei.HualeiConfiguration;
import com.garyzhangscm.cwms.outbound.repository.BulkPickConfigurationRepository;
import com.garyzhangscm.cwms.outbound.repository.HualeiConfigurationRepository;
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
public class HualeiConfigurationService {
    private static final Logger logger = LoggerFactory.getLogger(HualeiConfigurationService.class);

    @Autowired
    private HualeiConfigurationRepository hualeiConfigurationRepository;

    public HualeiConfiguration findById(Long id) {
        return hualeiConfigurationRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.raiseException("Hualei configuration not found by id: " + id));

    }

    public HualeiConfiguration findByWarehouse(Long warehouseId) {
        return hualeiConfigurationRepository.findByWarehouseId(warehouseId);
    }



    public HualeiConfiguration save(HualeiConfiguration hualeiConfiguration) {
        return hualeiConfigurationRepository.save(hualeiConfiguration);
    }

    public HualeiConfiguration saveOrUpdate(HualeiConfiguration hualeiConfiguration) {
        if (hualeiConfiguration.getId() == null && findByWarehouse(
                hualeiConfiguration.getWarehouseId()) != null) {
            hualeiConfiguration.setId(findByWarehouse(
                    hualeiConfiguration.getWarehouseId()).getId());
        }
        return save(hualeiConfiguration);
    }


    public void delete(HualeiConfiguration hualeiConfiguration) {
        hualeiConfigurationRepository.delete(hualeiConfiguration);
    }

    public void delete(Long id) {
        hualeiConfigurationRepository.deleteById(id);
    }



    public HualeiConfiguration addHualeiConfiguration(HualeiConfiguration hualeiConfiguration) {
        hualeiConfiguration.getHualeiShippingLabelFormatByProducts().forEach(
                hualeiShippingLabelFormatByProduct ->
                        hualeiShippingLabelFormatByProduct.setHualeiConfiguration(hualeiConfiguration)
        );
        return saveOrUpdate(hualeiConfiguration);
    }

    public HualeiConfiguration changeHualeiConfiguration(Long id, HualeiConfiguration hualeiConfiguration) {
        hualeiConfiguration.setId(id);
        hualeiConfiguration.getHualeiShippingLabelFormatByProducts().forEach(
                hualeiShippingLabelFormatByProduct ->
                        hualeiShippingLabelFormatByProduct.setHualeiConfiguration(hualeiConfiguration)
        );
        return saveOrUpdate(hualeiConfiguration);

    }
}
