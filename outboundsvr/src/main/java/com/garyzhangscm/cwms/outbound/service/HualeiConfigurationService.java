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
import com.garyzhangscm.cwms.outbound.model.hualei.HualeiProduct;
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
import java.util.Objects;


@Service
public class HualeiConfigurationService {
    private static final Logger logger = LoggerFactory.getLogger(HualeiConfigurationService.class);

    @Autowired
    private HualeiConfigurationRepository hualeiConfigurationRepository;

    @Autowired
    private HualeiProductService hualeiProductService;

    public HualeiConfiguration findById(Long id) {
        HualeiConfiguration hualeiConfiguration =  hualeiConfigurationRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.raiseException("Hualei configuration not found by id: " + id));

        loadAttribute(hualeiConfiguration);
        return hualeiConfiguration;
    }

    public HualeiConfiguration findByWarehouse(Long warehouseId) {
        HualeiConfiguration hualeiConfiguration =
                hualeiConfigurationRepository.findByWarehouseId(warehouseId);
        if (Objects.nonNull(hualeiConfiguration)) {
            loadAttribute(hualeiConfiguration);
        }
        return hualeiConfiguration;
    }

    public void loadAttribute(HualeiConfiguration hualeiConfiguration) {
        // load the tracking number query URL from the carrier
        logger.debug("Start to load the tracking number query url for hualei configuration of warehouse {}",
                hualeiConfiguration.getWarehouseId());
        hualeiConfiguration.getHualeiShippingLabelFormatByProducts().forEach(
                hualeiShippingLabelFormatByProduct -> {

                    HualeiProduct hualeiProduct = hualeiProductService.findByProductId(
                            hualeiConfiguration.getWarehouseId(),
                            hualeiShippingLabelFormatByProduct.getProductId()
                    );
                    logger.debug("get product by id {} ? {} ",
                            hualeiShippingLabelFormatByProduct.getProductId(),
                        Objects.isNull(hualeiProduct) ? "No" : "Yes");
                    logger.debug("Objects.nonNull(hualeiProduct): {} ", Objects.nonNull(hualeiProduct));
                    logger.debug("Objects.nonNull(hualeiProduct.getCarrier()): {} ", Objects.nonNull(hualeiProduct.getCarrier()));

                    if (Objects.nonNull(hualeiProduct) && Objects.nonNull(hualeiProduct.getCarrier())) {
                        hualeiShippingLabelFormatByProduct.setTrackingInfoUrl(
                                hualeiProduct.getCarrier().getTrackingInfoUrl()
                        );
                        logger.debug("setup the tracking number {}  from carrier {}",
                                hualeiProduct.getCarrier().getTrackingInfoUrl(),
                                hualeiProduct.getCarrier().getName());

                    }
                }
        );
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
        HualeiConfiguration newHualeiConfiguration = saveOrUpdate(hualeiConfiguration);
        loadAttribute(newHualeiConfiguration);
        return newHualeiConfiguration;
    }

    public HualeiConfiguration changeHualeiConfiguration(Long id, HualeiConfiguration hualeiConfiguration) {
        hualeiConfiguration.setId(id);
        hualeiConfiguration.getHualeiShippingLabelFormatByProducts().forEach(
                hualeiShippingLabelFormatByProduct ->
                        hualeiShippingLabelFormatByProduct.setHualeiConfiguration(hualeiConfiguration)
        );
        HualeiConfiguration newHualeiConfiguration =  saveOrUpdate(hualeiConfiguration);
        loadAttribute(newHualeiConfiguration);
        return newHualeiConfiguration;

    }
}
