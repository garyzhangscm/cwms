package com.garyzhangscm.cwms.outbound.service;

import com.easypost.exception.EasyPostException;
import com.easypost.exception.General.MissingParameterError;
import com.easypost.model.Event;
import com.easypost.model.Rate;
import com.easypost.model.Shipment;
import com.easypost.service.EasyPostClient;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.garyzhangscm.cwms.outbound.clients.CommonServiceRestemplateClient;
import com.garyzhangscm.cwms.outbound.clients.WarehouseLayoutServiceRestemplateClient;
import com.garyzhangscm.cwms.outbound.exception.ResourceNotFoundException;
import com.garyzhangscm.cwms.outbound.model.*;
import com.garyzhangscm.cwms.outbound.repository.EasyPostCarrierRepository;
import com.garyzhangscm.cwms.outbound.repository.EasyPostConfigurationRepository;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/*
 * https://www.easypost.com/
 *
 * an integration solution for shipping with UPS / USPS / Fedex / etc
 * we will use this to simplify the process to print shipping label
 * and track the package
 * please refer to https://github.com/EasyPost/easypost-java
 * for how to use the java SDK
* */
@Service
public class EasyPostConfigurationService {

    private static final Logger logger = LoggerFactory.getLogger(EasyPostConfigurationService.class);


    @Autowired
    private EasyPostConfigurationRepository easyPostConfigurationRepository;

    @Autowired
    private EasyPostCarrierRepository easyPostCarrierRepository;

    @Autowired
    private CommonServiceRestemplateClient commonServiceRestemplateClient;

    public EasyPostConfiguration findById(Long id, boolean loadDetails) {
        EasyPostConfiguration easyPostConfiguration = easyPostConfigurationRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.raiseException("easy post configuration not found by id: " + id));
        if (loadDetails) {
            loadOrderAttribute(easyPostConfiguration);
        }

        return easyPostConfiguration;
    }

    public EasyPostConfiguration findById(Long id) {
        return findById(id, true);
    }


    public EasyPostConfiguration findByWarehouseId(Long warehouseId,
                               Boolean loadDetails) {

        EasyPostConfiguration easyPostConfiguration =
                easyPostConfigurationRepository.findByWarehouseId(warehouseId);
        if (Objects.nonNull(easyPostConfiguration) && loadDetails) {
            loadOrderAttribute(easyPostConfiguration);
        }

        return easyPostConfiguration;

    }

    public EasyPostConfiguration findByWarehouseId(Long warehouseId){
        return findByWarehouseId(warehouseId, true);
    }


    private void loadOrderAttribute(EasyPostConfiguration easyPostConfiguration) {
        easyPostConfiguration.getCarriers().forEach(
                easyPostCarrier -> {
                    if (Objects.nonNull(easyPostCarrier.getCarrierId()) && Objects.isNull(easyPostCarrier.getCarrier())) {
                        easyPostCarrier.setCarrier(
                                commonServiceRestemplateClient.getCarrierById(
                                        easyPostCarrier.getCarrierId()
                                )
                        );
                    }
                }
        );
    }

    public EasyPostConfiguration save(EasyPostConfiguration easyPostConfiguration) {
        return save(easyPostConfiguration, true);
    }
    public EasyPostConfiguration save(EasyPostConfiguration easyPostConfiguration, boolean loadDetails) {
        EasyPostConfiguration newEasyPostConfiguration
                = easyPostConfigurationRepository.save(easyPostConfiguration);
        if (loadDetails) {

            loadOrderAttribute(newEasyPostConfiguration);
        }
        return newEasyPostConfiguration;
    }

    public EasyPostCarrier save(EasyPostCarrier easyPostCarrier) {
        return easyPostCarrierRepository.save(easyPostCarrier);
    }

    public EasyPostConfiguration saveOrUpdate(EasyPostConfiguration easyPostConfiguration) {
        return saveOrUpdate(easyPostConfiguration, true);
    }
    public EasyPostConfiguration saveOrUpdate(EasyPostConfiguration easyPostConfiguration, boolean loadDetails) {
        if (Objects.isNull(easyPostConfiguration.getId()) &&
                Objects.nonNull(findByWarehouseId(easyPostConfiguration.getWarehouseId(), false))) {
            easyPostConfiguration.setId(findByWarehouseId(easyPostConfiguration.getWarehouseId(), false).getId());
        }
        return save(easyPostConfiguration, loadDetails);
    }
    public EasyPostCarrier saveOrUpdate(EasyPostCarrier easyPostCarrier) {
        if (Objects.isNull(easyPostCarrier.getId()) &&
                Objects.nonNull(findCarrierById(easyPostCarrier.getWarehouseId(), easyPostCarrier.getCarrierId()))) {
            easyPostCarrier.setId(findCarrierById(easyPostCarrier.getWarehouseId(),  easyPostCarrier.getCarrierId()).getId());
        }
        return save(easyPostCarrier);
    }

    private EasyPostCarrier findCarrierById(Long warehouseId, Long carrierId) {
        return easyPostCarrierRepository.findByWarehouseIdAndCarrierId(
                warehouseId, carrierId
        );
    }
    public EasyPostConfiguration addConfiguration(EasyPostConfiguration easyPostConfiguration) {
        easyPostConfiguration.getCarriers()
                .stream().filter(

                easyPostCarrier -> Objects.isNull(easyPostCarrier.getEasyPostConfiguration())
        ).forEach(
                easyPostCarrier ->  easyPostCarrier.setEasyPostConfiguration(easyPostConfiguration)
        );

        return saveOrUpdate(easyPostConfiguration);
    }
    public EasyPostConfiguration changeConfiguration(EasyPostConfiguration easyPostConfiguration) {

        easyPostConfiguration.getCarriers()
                .stream().filter(

                easyPostCarrier -> Objects.isNull(easyPostCarrier.getEasyPostConfiguration())
        ).forEach(
                easyPostCarrier ->  easyPostCarrier.setEasyPostConfiguration(easyPostConfiguration)
        );

        return saveOrUpdate(easyPostConfiguration);
    }

    public EasyPostCarrier addCarrier(Long easyPostConfigurationId, EasyPostCarrier easyPostCarrier) {
        if (Objects.isNull(easyPostCarrier.getEasyPostConfiguration())) {

            EasyPostConfiguration easyPostConfiguration = findById(easyPostConfigurationId, false);
            easyPostCarrier.setEasyPostConfiguration(easyPostConfiguration);
        }
        return saveOrUpdate(easyPostCarrier);
    }

    public EasyPostCarrier changeCarrier(Long easyPostConfigurationId, EasyPostCarrier easyPostCarrier) {
        if (Objects.isNull(easyPostCarrier.getEasyPostConfiguration())) {

            EasyPostConfiguration easyPostConfiguration = findById(easyPostConfigurationId, false);
            easyPostCarrier.setEasyPostConfiguration(easyPostConfiguration);
        }
        return saveOrUpdate(easyPostCarrier);
    }

    @Transactional
    public void removeCarrier(Long easyPostCarrierId) {
        easyPostCarrierRepository.deleteById(easyPostCarrierId);
    }



}
