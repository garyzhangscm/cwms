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
import com.garyzhangscm.cwms.outbound.model.*;
import com.garyzhangscm.cwms.outbound.repository.ShipmentRepository;
import com.garyzhangscm.cwms.outbound.repository.WaveRepository;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestParam;

import javax.transaction.Transactional;
import java.util.*;


@Service
public class WaveService {
    private static final Logger logger = LoggerFactory.getLogger(WaveService.class);

    @Autowired
    private WaveRepository waveRepository;
    @Autowired
    private ShipmentService shipmentService;
    @Autowired
    private OrderService orderService;

    @Autowired
    private ShipmentLineService shipmentLineService;

    @Autowired
    private CommonServiceRestemplateClient commonServiceRestemplateClient;
    @Autowired
    private WarehouseLayoutServiceRestemplateClient warehouseLayoutServiceRestemplateClient;
    @Autowired
    private InventoryServiceRestemplateClient inventoryServiceRestemplateClient;

    public Wave findById(Long id) {
        return findById(id, true);
    }

    public Wave findById(Long id, boolean loadAttribute) {

        Wave wave =  waveRepository.findById(id).orElse(null);
        if (wave != null && loadAttribute) {
            loadAttribute(wave);
        }
        return wave;
    }

    public List<Wave> findAll(String number ) {
        return findAll(number, true);
    }

    public List<Wave> findAll(String number, boolean loadAttribute) {
        List<Wave> waves;

        if (StringUtils.isBlank(number)) {
            waves = waveRepository.findAll();
        } else {
            Wave wave = waveRepository.findByNumber(number);
            if (wave != null) {
                waves = Arrays.asList(new Wave[]{wave});
            } else {
                waves = new ArrayList<>();
            }
        }
        if (waves.size() > 0 && loadAttribute) {
            loadAttribute(waves);
        }
        return waves;
    }

    public Wave findByNumber(String number) {
        return findByNumber(number, true);
    }

    public Wave findByNumber(String number, boolean loadAttribute) {
        Wave wave = waveRepository.findByNumber(number);
        if (wave != null && loadAttribute) {
            loadAttribute(wave);
        }
        return wave;
    }

    private void loadAttribute(List<Wave> waves) {
        waves.forEach(wave -> loadAttribute(wave));
    }
    private void loadAttribute(Wave wave) {
        wave.getShipmentLines().forEach(shipmentLine -> {
            loadOrderLineAttribute(shipmentLine.getOrderLine());
            if (shipmentLine.getShortAllocation() != null) {
                loadShortAllocationAttribute(shipmentLine.getShortAllocation());
            }
            loadPickAttribute(shipmentLine.getPicks());
        });
    }
    private void loadOrderLineAttribute(OrderLine orderLine) {
        if (orderLine.getInventoryStatusId() != null && orderLine.getInventoryStatus() == null) {
            orderLine.setInventoryStatus(inventoryServiceRestemplateClient.getInventoryStatusById(orderLine.getInventoryStatusId()));
        }
        if (orderLine.getItemId() != null && orderLine.getItem() == null) {
            orderLine.setItem(inventoryServiceRestemplateClient.getItemById(orderLine.getItemId()));
        }
    }

    private void loadShortAllocationAttribute(ShortAllocation shortAllocation) {

        if (shortAllocation.getItemId() != null && shortAllocation.getItem() == null) {
            shortAllocation.setItem(inventoryServiceRestemplateClient.getItemById(shortAllocation.getItemId()));
        }
    }

    private void loadPickAttribute(List<Pick> picks) {
        picks.stream().filter(Objects::nonNull).forEach(this::loadPickAttribute);
    }
    private void loadPickAttribute(Pick pick) {

        if (pick.getItemId() != null && pick.getItem() == null) {
            pick.setItem(inventoryServiceRestemplateClient.getItemById(pick.getItemId()));
        }
        if (pick.getSourceLocationId() != null && pick.getSourceLocation() == null) {
            pick.setSourceLocation(warehouseLayoutServiceRestemplateClient.getLocationById(pick.getSourceLocationId()));
        }
        if (pick.getDestinationLocationId() != null && pick.getDestinationLocation() == null) {
            pick.setDestinationLocation(warehouseLayoutServiceRestemplateClient.getLocationById(pick.getDestinationLocationId()));
        }
    }
    public Wave save(Wave wave) {
        return waveRepository.save(wave);
    }


    public void delete(Wave wave) {
        waveRepository.delete(wave);
    }

    public void delete(Long id) {
        waveRepository.deleteById(id);
    }

    public void delete(String waveIds) {
        if (!waveIds.isEmpty()) {
            long[] waveIdArray = Arrays.asList(waveIds.split(",")).stream().mapToLong(Long::parseLong).toArray();
            for (long id : waveIdArray) {
                delete(id);
            }
        }
    }

    public void cancelWave(Wave wave) {

    }
    public void cancelWave(Long id) {
        cancelWave(findById(id));
    }

    // Plan a list of order lines into a wave
    @Transactional
    public Wave planWave(Long warehouseId, String waveNumber, List<OrderLine> orderLines) {

        if (StringUtils.isBlank(waveNumber)) {
            waveNumber = getNextWaveNumber();
            logger.debug("wave number is not passed in during plan wave, auto generated number: {}", waveNumber);
        }

        logger.debug("Start to plan {} order lines into wave # {}", orderLines.size(), waveNumber);
        Wave wave = findByNumber(waveNumber);
        if (wave == null) {
            wave = new Wave();
            wave.setNumber(waveNumber);
            wave.setStatus(WaveStatus.PLANED);
            wave.setWarehouseId(warehouseId);
            wave = save(wave);
        }
        String shipmentNumber = shipmentService.getNextShipmentNumber();

        shipmentService.planShipments(wave, shipmentNumber, orderLines);
        return findByNumber(waveNumber);
    }

    public Wave createWave(Long warehouseId, String waveNumber) {

        Wave wave = findByNumber(waveNumber);
        if (wave == null) {
            wave = new Wave();
            wave.setNumber(waveNumber);
            wave.setStatus(WaveStatus.PLANED);
            wave.setWarehouseId(warehouseId);
            return save(wave);
        }
        else {
            return wave;
        }
    }

    public List<Order> findWaveCandidate(String orderNumber,
                                         String customerName) {

        return orderService.findWavableOrders(orderNumber, customerName);
    }

    public Wave allocateWave(Long id) {
        return allocateWave(findById(id));
    }
    public Wave allocateWave(Wave wave) {

        // Allocate each open shipment line
        wave.getShipmentLines().forEach(shipmentLine -> {
            shipmentLineService.allocateShipmentLine(shipmentLine);
        });
        wave.setStatus(WaveStatus.ALLOCATED);

        // return the latest information
        return save(wave);

    }

    private String getNextWaveNumber(){
        return commonServiceRestemplateClient.getNextNumber("wave-number");
    }
}
