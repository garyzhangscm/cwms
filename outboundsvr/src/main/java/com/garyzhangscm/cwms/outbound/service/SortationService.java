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
import com.garyzhangscm.cwms.outbound.exception.ResourceNotFoundException;
import com.garyzhangscm.cwms.outbound.exception.ShippingException;
import com.garyzhangscm.cwms.outbound.model.*;
import com.garyzhangscm.cwms.outbound.repository.SortationByShipmentLineHistoryRepository;
import com.garyzhangscm.cwms.outbound.repository.SortationByShipmentLineRepository;
import com.garyzhangscm.cwms.outbound.repository.SortationByShipmentRepository;
import com.garyzhangscm.cwms.outbound.repository.SortationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;


@Service
public class SortationService {
    private static final Logger logger = LoggerFactory.getLogger(SortationService.class);

    @Autowired
    private SortationRepository sortationRepository;

    @Autowired
    private PickService pickService;
    @Autowired
    private WaveService waveService;


    @Autowired
    private CommonServiceRestemplateClient commonServiceRestemplateClient;

    @Autowired
    private SortationByShipmentRepository sortationByShipmentRepository;

    @Autowired
    private SortationByShipmentLineRepository sortationByShipmentLineRepository;

    @Autowired
    private InventoryServiceRestemplateClient inventoryServiceRestemplateClient;

    @Autowired
    private SortationByShipmentLineHistoryRepository sortationByShipmentLineHistoryRepository;


    public Sortation save(Sortation sortation) {
        return sortationRepository.save(sortation);
    }

    public Sortation saveOrUpdate(Sortation sortation) {
        if (Objects.isNull(sortation.getId()) &&
                Objects.nonNull(findByNumber(sortation.getWarehouseId(), sortation.getNumber()))) {
                sortation.setId(
                        findByNumber(sortation.getWarehouseId(), sortation.getNumber()).getId()
                );
        }
        return save(sortation);
    }

    public Sortation findByNumber(Long warehouseId, String number) {
        return  sortationRepository.findByWarehouseIdAndNumber(warehouseId, number);


    }
    public Sortation findByWarehouseAndWave(Long warehouseId, String waveNumber, Long locationId) {
        return sortationRepository.findByWarehouseAndWave(warehouseId, waveNumber, locationId);
    }
    public Sortation getByWaveNumber(Long warehouseId, String waveNumber, Long locationId) {
        Sortation sortation = findByWarehouseAndWave(warehouseId, waveNumber, locationId);

        // if we already have the sortation request, then return it
        if (Objects.nonNull(sortation)) {
            return sortation;
        }

        logger.debug("We can't find existing sortation by wave {}, let's create a new one for the wave",
                waveNumber);
        return createSortationByWave(warehouseId, waveNumber, locationId);

    }

    /**
     * Create the sortation for the wave
     * @param warehouseId
     * @param waveNumber
     * @return
     */
    private Sortation createSortationByWave(Long warehouseId, String waveNumber, Long locationId) {
        // let's get all the locations that contains the picked in wave and already arrived at the location
        Wave wave = waveService.findByNumber(warehouseId, waveNumber, false);

        logger.debug("Let's find the picks for the wave {}", wave.getNumber());
        List<Pick> picks = pickService.findByWave(wave, false).stream().filter(
                        pick -> pick.getPickedQuantity() > 0
                ).collect(Collectors.toList());

        if (picks.size() == 0) {
            throw ShippingException.raiseException("can't sort the wave " + waveNumber +
                    " as there's no pick for the wave");
        }
        logger.debug("Found {} picks for this wave {}, let's find all picked inventory",
                picks.size(), wave.getNumber());

        List<Inventory> inventories = inventoryServiceRestemplateClient.getPickedInventory(
                wave.getWarehouseId(), picks, null, locationId, false);

        logger.debug("Got {} picked inventory for this wave {}",
                inventories.size(), wave.getNumber());
        // key: pick id
        // value: total quantity arrived at location
        Map<Long, Long> arrivedQuantityByPick = new HashMap<>();
        inventories.forEach(
                inventory -> {
                    Long arrivedQuantity = arrivedQuantityByPick.getOrDefault(inventory.getPickId(), 0l);
                    arrivedQuantityByPick.put(inventory.getPickId(), arrivedQuantity + inventory.getQuantity());
                }
        );

        logger.debug("start to create new sortation for this wave");
        Sortation sortation = new Sortation();
        sortation.setWarehouseId(warehouseId);
        sortation.setNumber(getNextSortationNumber(warehouseId));
        sortation.setWave(wave);
        sortation.setLocationId(locationId);
        sortation.setSortationByShipments(new ArrayList<>());

        // start to setup the shipment and lines for the new sortation request

        Set<Shipment> shipments = picks.stream().map(pick -> pick.getShipmentLine().getShipment()).collect(Collectors.toSet());

        logger.debug("found {} shipment in the wave {}", shipments.size(), wave.getNumber());
        shipments.forEach(
                shipment -> {
                    SortationByShipment sortationByShipment = new SortationByShipment();
                    sortationByShipment.setSortation(sortation);
                    sortationByShipment.setShipment(shipment);

                    logger.debug("## create one record for each line in shipment {}", shipment.getNumber());
                    shipment.getShipmentLines().forEach(
                            shipmentLine -> {
                                SortationByShipmentLine sortationByShipmentLine = new SortationByShipmentLine();
                                sortationByShipmentLine.setSortationByShipment(sortationByShipment);
                                sortationByShipmentLine.setShipmentLine(shipmentLine);
                                sortationByShipmentLine.setExpectedQuantity(
                                        shipmentLine.getPicks().stream().map(
                                                pick -> pick.getQuantity()).mapToLong(Long::longValue).sum()
                                );

                                sortationByShipmentLine.setArrivedQuantity(
                                        shipmentLine.getPicks().stream().map(
                                                pick -> arrivedQuantityByPick.getOrDefault(pick.getId(), 0l)).
                                                mapToLong(Long::longValue).sum()
                                );
                                sortationByShipmentLine.setSortedQuantity(0l);
                                sortationByShipment.addSortationByShipmentLine(sortationByShipmentLine);
                            }
                    );
                    sortation.addSortationByShipment(sortationByShipment);
                }
        );
        logger.debug("Done, save the sortation result");

        return saveOrUpdate(sortation);


    }

    private String getNextSortationNumber(Long warehouseId) {

        return commonServiceRestemplateClient.getNextNumber(warehouseId, "sortation-number");
    }

    private SortationByShipmentLine save(SortationByShipmentLine sortationByShipmentLine) {
        return sortationByShipmentLineRepository.save(sortationByShipmentLine);
    }
    public SortationByShipmentLine processWaveSortationByItem(Long warehouseId, String number,
                                                              Long itemId, Long quantity) {
        Sortation sortation = findByNumber(warehouseId, number);
        if (Objects.isNull(sortation)) {
            throw ShippingException.raiseException("can't find sortation request by number " + number);
        }
        // find the right sortation & shipment for this item
        for (SortationByShipment sortationByShipment : sortation.getSortationByShipments()) {
            for (SortationByShipmentLine sortationByShipmentLine : sortationByShipment.getSortationByShipmentLines()) {
                if (sortationByShipmentLine.getShipmentLine().getOrderLine().getItemId().equals(itemId) &&
                    sortationByShipmentLine.getSortedQuantity() + quantity <= sortationByShipmentLine.getArrivedQuantity()) {
                    // ok, we found the matching sortation by shipment line
                    sortationByShipmentLine.setSortedQuantity(
                            sortationByShipmentLine.getSortedQuantity() + quantity
                    );
                    return save(sortationByShipmentLine);
                }
            }
        }

        throw ShippingException.raiseException("can't find valid item with id " + itemId + " from sortation request" +
                number + "  for wave " + sortation.getWave().getNumber());
    }

    public SortationByShipment getSortationByShipment(Long id) {
        return sortationByShipmentRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.raiseException("sortation by shipment not found by id: " + id));
    }


    public SortationByShipmentLine processShipmentLineSortationById(Long id, Long quantity) {
        SortationByShipmentLine sortationByShipmentLine =
                sortationByShipmentLineRepository.findById(id)
                        .orElseThrow(() -> ResourceNotFoundException.raiseException(
                                "sortation by shipment line not found by id: " + id));

        if (sortationByShipmentLine.getSortedQuantity() + quantity > sortationByShipmentLine.getArrivedQuantity()) {
            throw ShippingException.raiseException("can't complete current sort as it extends the arrived quantity");
        }

        sortationByShipmentLine.setSortedQuantity(sortationByShipmentLine.getSortedQuantity() + quantity);

        return sortationByShipmentLineRepository.save(sortationByShipmentLine);
    }

    public List<Inventory> findPickedInventoryByItem(Long warehouseId, String waveNumber,
                                                     Long itemId,
                                                     Long locationId) {
        Wave wave = waveService.findByNumber(warehouseId, waveNumber, false);
        return waveService.getPickedInventory(wave, locationId).stream().filter(
                inventory -> inventory.getItem().getId().equals(itemId)
        ).collect(Collectors.toList());
    }
}
