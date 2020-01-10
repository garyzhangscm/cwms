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
import com.garyzhangscm.cwms.outbound.model.OrderLine;
import com.garyzhangscm.cwms.outbound.model.Shipment;
import com.garyzhangscm.cwms.outbound.model.Wave;
import com.garyzhangscm.cwms.outbound.repository.ShipmentRepository;
import com.garyzhangscm.cwms.outbound.repository.WaveRepository;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;


@Service
public class WaveService {
    private static final Logger logger = LoggerFactory.getLogger(WaveService.class);

    @Autowired
    private WaveRepository waveRepository;
    @Autowired
    private ShipmentService shipmentService;

    @Autowired
    private CommonServiceRestemplateClient commonServiceRestemplateClient;
    @Autowired
    private WarehouseLayoutServiceRestemplateClient warehouseLayoutServiceRestemplateClient;
    @Autowired
    private InventoryServiceRestemplateClient inventoryServiceRestemplateClient;


    public Wave findById(Long id) {
        return waveRepository.findById(id).orElse(null);
    }


    public List<Wave> findAll(String number ) {
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
        return waves;
    }

    public Wave findByNumber(String number) {
        return waveRepository.findByNumber(number);
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
    public Wave planWave(String waveNumber, List<OrderLine> orderLines) {

        Wave wave = findByNumber(waveNumber);
        if (wave == null) {
            wave = new Wave();
            wave.setNumber(waveNumber);
            wave = save(wave);
        }

        shipmentService.planShipments(wave, orderLines);
        return findByNumber(waveNumber);
    }

    public Wave createWave(String waveNumber) {

        Wave wave = findByNumber(waveNumber);
        if (wave == null) {
            wave = new Wave();
            wave.setNumber(waveNumber);
            return save(wave);
        }
        else {
            return wave;
        }
    }

    public void allocateWave(Wave wave) {

    }
}
