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

import com.garyzhangscm.cwms.outbound.clients.CommonServiceRestemplateClient;
import com.garyzhangscm.cwms.outbound.clients.InventoryServiceRestemplateClient;
import com.garyzhangscm.cwms.outbound.clients.WarehouseLayoutServiceRestemplateClient;
import com.garyzhangscm.cwms.outbound.exception.GenericException;
import com.garyzhangscm.cwms.outbound.exception.PickingException;
import com.garyzhangscm.cwms.outbound.exception.ResourceNotFoundException;
import com.garyzhangscm.cwms.outbound.model.*;
import com.garyzhangscm.cwms.outbound.repository.CartonizationRepository;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.persistence.criteria.*;
import javax.transaction.Transactional;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;


@Service
public class CartonizationService {
    private static final Logger logger = LoggerFactory.getLogger(CartonizationService.class);

    @Autowired
    private CartonizationRepository cartonizationRepository;
    @Autowired
    private CartonizationConfigurationService cartonizationConfigurationService;
    @Autowired
    private CartonService cartonService;


    @Autowired
    private WarehouseLayoutServiceRestemplateClient warehouseLayoutServiceRestemplateClient;
    @Autowired
    private InventoryServiceRestemplateClient inventoryServiceRestemplateClient;
    @Autowired
    private CommonServiceRestemplateClient commonServiceRestemplateClient;



    public Cartonization findById(Long id) {
        return cartonizationRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.raiseException("carton not found by id: " + id));
    }

    public Cartonization findByNumber(Long warehouseId, String number) {
        return cartonizationRepository.findByWarehouseIdAndNumber(warehouseId, number);
    }

    public List<Cartonization> findByGroupKeyValueAndStatus(String groupKeyValue, CartonizationStatus cartonizationStatus) {
        return cartonizationRepository.findByGroupKeyValueAndStatus(groupKeyValue, cartonizationStatus);
    }

    public Cartonization save(Cartonization cartonization) {
        return cartonizationRepository.save(cartonization);
    }


    public Cartonization saveOrUpdate(Cartonization cartonization) {
        if (Objects.isNull(cartonization.getId()) &&
                Objects.nonNull(findByNumber(cartonization.getWarehouseId(),cartonization.getNumber()))) {
            cartonization.setId(findByNumber(cartonization.getWarehouseId(),cartonization.getNumber()).getId());
        }
        return save(cartonization);
    }

    public List<Cartonization> findAll(Long warehouseId, String number, String status, String cartonName) {

        List<Cartonization> cartonizations =  cartonizationRepository.findAll(
                (Root<Cartonization> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) -> {
                    List<Predicate> predicates = new ArrayList<Predicate>();

                    predicates.add(criteriaBuilder.equal(root.get("warehouseId"), warehouseId));

                    if (StringUtils.isNotBlank(number)) {
                        predicates.add(criteriaBuilder.equal(root.get("number"), number));

                    }

                    if (StringUtils.isNotBlank(status)) {
                        predicates.add(criteriaBuilder.equal(root.get("status"), CartonizationStatus.valueOf(status)));

                    }
                    if (StringUtils.isNotBlank(cartonName)) {
                        Join<Cartonization, Carton> joinCarton = root.join("carton", JoinType.INNER);
                        predicates.add(criteriaBuilder.equal(joinCarton.get("name"), cartonName));

                    }
                    Predicate[] p = new Predicate[predicates.size()];
                    return criteriaBuilder.and(predicates.toArray(p));
                }
        );


        return cartonizations;
    }
    private List<CartonizationConfiguration> findMatchedCartonizationConfiguration(Pick pick) {
        return cartonizationConfigurationService.findMatchedCartonizationConfiguration(pick);
    }

    @Transactional
    public Cartonization processCartonization(Pick pick) {
        logger.debug(">> Start to process cartionization <<");
        if (!isCartonizationEnabled(pick)) {
            logger.debug(">> Cartonization is not enabled for the pick");
            return null;
        }
        try {
                Cartonization cartonization = findMatchedCartonization(pick);
                if (Objects.nonNull(cartonization)) {
                    logger.debug("We find a cartonization {} for current pick: {} ");
                    // Please note findMatchedCartonization has side affect
                    // When the original carton in the cartonization can't
                    // fit with the new pick but we are able to enlarge the carton
                    // by size and make the new pick fit into current cartonization
                    // Hence, we will save the new cartonization here
                    return saveOrUpdate(cartonization);

                }
                else {
                    logger.debug(">> No suitable cartonization being find, will try to create a new cartonization");
                    return saveOrUpdate(createCartonization(pick));
                }
        }
        catch (GenericException ex) {
            logger.debug(">> No suitable cartonization being find, will try to create a new cartonization");
            // create a new cartonization for the pick
            return saveOrUpdate(createCartonization(pick));
        }
    }

    private Cartonization createCartonization(Pick pick) {
        logger.debug("Start to create new cartonization");
        Carton carton = cartonService.getBestCarton(pick);
        if (Objects.isNull(carton)) {
            // OK we are not able to find a suitable carton for the pick
            logger.debug("Not able to find a suitable carton for the current pick");
            return null;
        }
        logger.debug("The best size for the current pick is {}", carton.getName());

        List<CartonizationConfiguration> cartonizationConfigurations = findMatchedCartonizationConfiguration(pick);
        if (cartonizationConfigurations.size() == 0) {
            // No configuration defined for the pick
            logger.debug("No configuration defined for cartonization");
            return null;
        }
        CartonizationConfiguration cartonizationConfiguration = cartonizationConfigurations.get(0);

        Cartonization cartonization = new Cartonization();
        cartonization.setNumber(getNextCartonizationNumber(pick.getWarehouseId()));

        cartonization.setCarton(carton);
        cartonization.setGroupKeyValue(getGroupKeyValue(cartonizationConfiguration, pick));
        cartonization.setStatus(CartonizationStatus.OPEN);
        cartonization.setWarehouseId(pick.getWarehouseId());

        return cartonization;

    }

    private String getNextCartonizationNumber(Long warehouseId) {
        return commonServiceRestemplateClient.getNextNumber(warehouseId, "cartonization-number");
    }

    public void processPickList(Cartonization cartonization, PickList pickList) {
        cartonization.setPickList(pickList);
        saveOrUpdate(cartonization);
    }

    /**
     * Check if we will need to cartonize the pick, only when
     * 1. The location has cartonization enabled
     * 2. The item has cartonization enabled
     * @param pick
     * @return
     */
    private boolean isCartonizationEnabled(Pick pick) {

        if (Objects.isNull(pick.getItem())) {
            pick.setItem(inventoryServiceRestemplateClient.getItemById(pick.getItemId()));
        }
        if (Objects.isNull(pick.getSourceLocation())) {
            pick.setSourceLocation(warehouseLayoutServiceRestemplateClient.getLocationById(
                    pick.getSourceLocationId()
            ));
        }
        return pick.getItem().getAllowCartonization() &&
                pick.getSourceLocation().getLocationGroup().getAllowCartonization();
    }

    private Cartonization findMatchedCartonization(Pick pick) {
        List<CartonizationConfiguration> cartonizationConfigurations = findMatchedCartonizationConfiguration(pick);
        return findMatchedCartonization(cartonizationConfigurations, pick);
    }
    private Cartonization findMatchedCartonization(List<CartonizationConfiguration> cartonizationConfigurations, Pick pick) {

        for(CartonizationConfiguration cartonizationConfiguration : cartonizationConfigurations) {
            try {
                logger.debug("Start to find existing OPEN cartonization based on the configuraiton {}",
                        cartonizationConfiguration);
                Cartonization cartonization = findMatchedCartonization(cartonizationConfiguration, pick);
                return cartonization;
            }
            catch (GenericException ex) {
                // if we can't find a list, let's just ignore and continue with the next configuration
                logger.debug("Fail when try the configuration {}, exception: \n{}",
                        cartonizationConfiguration.getId(), ex.getMessage());
            }
        }
        throw PickingException.raiseException( "Can't find matched open cartonization while trying all the cartonization configurations");

    }
    private Cartonization findMatchedCartonization(CartonizationConfiguration cartonizationConfiguration, Pick pick) {

        String groupKeyValue = getGroupKeyValue(cartonizationConfiguration, pick);

        logger.debug("Will try to find existing carton based on groupKey: {}",
                groupKeyValue);
        // Only return the open cartonization with same group key
        List<Cartonization> cartonizations = findByGroupKeyValueAndStatus(groupKeyValue, CartonizationStatus.OPEN);
        if (cartonizations.size() == 0) {
            throw PickingException.raiseException( "Can't find matched open cartonization with the configuration");
        }
        // Find best matched cartonization.
        // Note: findBestCartonizationBySpace may change the cartonization.carton's size to best fit
        // the pick.
        Cartonization bestCartonization
                = cartonizations.stream().
                    filter(cartonization -> findBestCartonizationBySpace(cartonization, pick))
                    .findFirst()
                    .orElseThrow(() -> PickingException.raiseException( "Can't find matched open cartonization with the configuration"));

        return bestCartonization;
    }

    /**
     * Check if we can fit the pick into a existing cartonization
     * @param cartonization  existing cartonization
     * @param pick new pick
     * @return
     */
    private boolean findBestCartonizationBySpace(Cartonization cartonization, Pick pick) {
        double totalSpace = cartonization.getTotalSpace();
        double usedSpace = cartonization.getUsedSpace();
        double size = pick.getSize();

        logger.debug("cartonization's id : {}, carton's name : {}, total space: {}, used space: {}, pick's size: {}",
                cartonization.getId(),
                cartonization.getCarton().getName(),
                totalSpace,usedSpace, size);
        if ((usedSpace + size) <= totalSpace) {
            return true;
        }
        // Current carton is not big enough for the new pick, let's
        // check if we can extends the carton to get more space
        Carton nextSizeCarton
                = cartonService.getNextSizeCarton(cartonization.getWarehouseId(), cartonization.getCarton());

        if (Objects.isNull(nextSizeCarton)) {
            return false;
        }
        else {
            // try with the next carton size
            cartonization.setCarton(nextSizeCarton);
            return findBestCartonizationBySpace(cartonization, pick);

        }

    }


    private String getGroupKeyValue(CartonizationConfiguration cartonizationConfiguration, Pick pick) {

        List<CartonizationGroupRule> cartonizationGroupRules = cartonizationConfiguration.getGroupRules();
        List<String> groupKeyValues = new ArrayList<>();
        return cartonizationGroupRules.stream().
                map(cartonizationGroupRule -> getGroupKeyValue(cartonizationGroupRule, pick)).collect(Collectors.joining("-"));
    }

    private String getGroupKeyValue(CartonizationGroupRule cartonizationGroupRule, Pick pick) {
        String value = "";
        switch (cartonizationGroupRule) {
            case BY_ORDER:
                value = pick.getOrderNumber();
                break;
            case BY_SHIPMENT:
                value = pick.getShipmentLine().getShipmentNumber();
                break;
            case BY_LOCATION_GROUP:
                // if we can get the source location's group name
                if (Objects.isNull(pick.getSourceLocation())) {
                   pick.setSourceLocation(warehouseLayoutServiceRestemplateClient.getLocationById(pick.getSourceLocationId()));
                }
                value = pick.getSourceLocation().getLocationGroup().getName();
                break;
            default:
                value = "";
        }
        return value;

    }









}
