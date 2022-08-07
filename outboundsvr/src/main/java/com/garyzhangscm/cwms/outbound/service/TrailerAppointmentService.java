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
import com.garyzhangscm.cwms.outbound.exception.ResourceNotFoundException;
import com.garyzhangscm.cwms.outbound.model.*;
import com.garyzhangscm.cwms.outbound.repository.StopRepository;
import org.apache.commons.lang.StringUtils;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;


@Service
public class TrailerAppointmentService {
    private static final Logger logger = LoggerFactory.getLogger(TrailerAppointmentService.class);


    @Autowired
    private StopService stopService;
    @Autowired
    private ShipmentService shipmentService;
    @Autowired
    private OrderService orderService;
    @Autowired
    private OrderLineService orderLineService;
    @Autowired
    private WaveService waveService;

    @Autowired
    private CommonServiceRestemplateClient commonServiceRestemplateClient;

    public TrailerAppointment assignStopShipmentOrdersToTrailerAppointment(Long trailerAppointmentId,
                                                                           String stopIdList,
                                                                           String shipmentIdList,
                                                                           String orderIdList) {
        TrailerAppointment trailerAppointment =
                commonServiceRestemplateClient.getTrailerAppointmentById(trailerAppointmentId);
        if (Objects.isNull(trailerAppointment)) {
           throw ResourceNotFoundException.raiseException("trailer appointment not found by id: " + trailerAppointmentId);
        }
        logger.debug("We get trailer appointment {}, by id {}",
                trailerAppointment.getNumber(), trailerAppointment.getId());
        if (Strings.isNotBlank(stopIdList)) {
            assignStopsToTrailerAppointment(trailerAppointment, stopIdList);
        }
        if (Strings.isNotBlank(shipmentIdList)) {
            assignShipmentsToTrailerAppointment(trailerAppointment, shipmentIdList);
        }
        if (Strings.isNotBlank(orderIdList)) {
            assignOrdersToTrailerAppointment(trailerAppointment, orderIdList);
        }

        return trailerAppointment;
    }

    private void assignOrdersToTrailerAppointment(TrailerAppointment trailerAppointment, String orderIdList) {
        logger.debug("Start to assign trailer appointment {} to orders with id {}",
                trailerAppointment.getNumber(), orderIdList);
        Arrays.stream(orderIdList.split(",")).forEach(
                orderId -> {
                    orderService.assignTrailerAppointment(Long.parseLong(orderId), trailerAppointment);
                }
        );
    }

    private void assignShipmentsToTrailerAppointment(TrailerAppointment trailerAppointment, String shipmentIdList) {
        logger.debug("Start to assign trailer appointment {} to shipment with id {}",
                trailerAppointment.getNumber(), shipmentIdList);

        Arrays.stream(shipmentIdList.split(",")).forEach(
                shipmentId -> {
                    shipmentService.assignTrailerAppointment(Long.parseLong(shipmentId), trailerAppointment);
                }
        );
    }

    private void assignStopsToTrailerAppointment(TrailerAppointment trailerAppointment, String stopIdList) {
        logger.debug("Start to assign trailer appointment {} to stop with id {}",
                trailerAppointment.getNumber(), stopIdList);
        Arrays.stream(stopIdList.split(",")).forEach(
                stopId -> {
                    stopService.assignTrailerAppointment(Long.parseLong(stopId), trailerAppointment);
                }
        );
    }

    /**
     * Process trailer appointment integration
     * @param trailerAppointment
     */
    public void processIntegration(TrailerAppointment trailerAppointment) {

        // first let's create a wave with the trailer appointment's number
        // we will only have one wave for the entire trailer appointment

        Wave wave = waveService.createWave(
                trailerAppointment.getWarehouseId(),
                trailerAppointment.getNumber());

        // second, let's plan a shipment for each order in the stops

        for (Stop stop : trailerAppointment.getStops()) {
            stopService.processIntegration(wave, stop, trailerAppointment.getId());

        }

    }
}
