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
import com.garyzhangscm.cwms.outbound.repository.ShipmentLineRepository;
import com.garyzhangscm.cwms.outbound.repository.ShipmentRepository;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;


@Service
public class ShipmentLineService {
    private static final Logger logger = LoggerFactory.getLogger(ShipmentLineService.class);

    @Autowired
    private ShipmentLineRepository shipmentLineRepository;

    @Autowired
    private CommonServiceRestemplateClient commonServiceRestemplateClient;
    @Autowired
    private WarehouseLayoutServiceRestemplateClient warehouseLayoutServiceRestemplateClient;
    @Autowired
    private InventoryServiceRestemplateClient inventoryServiceRestemplateClient;


    public ShipmentLine findById(Long id) {
        return shipmentLineRepository.findById(id).orElse(null);
    }


    public ShipmentLine save(ShipmentLine shipmentLine) {
        return shipmentLineRepository.save(shipmentLine);
    }


    public void delete(ShipmentLine shipmentLine) {
        shipmentLineRepository.delete(shipmentLine);
    }

    public void delete(Long id) {
        shipmentLineRepository.deleteById(id);
    }


    public ShipmentLine createShipmentLine(Wave wave, Shipment shipment, OrderLine orderLine) {
        return createShipmentLine(wave, shipment, orderLine, orderLine.getOpenQuantity());
    }


    public ShipmentLine createShipmentLine(Wave wave, Shipment shipment, OrderLine orderLine, Long shipmentLineQuantity) {
        ShipmentLine shipmentLine = new ShipmentLine();
        shipmentLine.setNumber(getNextShipmentLineNumber(shipment));
        shipmentLine.setOrderLine(orderLine);
        shipmentLine.setQuantity(shipmentLineQuantity);
        shipmentLine.setShippedQuantity(0L);
        shipmentLine.setWave(wave);
        return save(shipmentLine);
    }



    private String getNextShipmentNumber(){
        return commonServiceRestemplateClient.getNextNumber("shipment_number");
    }

    private String getNextShipmentLineNumber(Shipment shipment) {
        if (shipment.getShipmentLines().isEmpty()) {
            return "0";
        } else {
            // Suppose the line number is all numeric
            int max = 0;
            for (ShipmentLine shipmentLine : shipment.getShipmentLines()) {
                try {
                    if (Integer.parseInt(shipmentLine.getNumber()) > max) {
                        max = Integer.parseInt(shipmentLine.getNumber());
                    }
                } catch (Exception e) {
                    continue;
                }
            }
            return String.valueOf(max + 1);
        }
    }

    public List<ShipmentLine> findByWaveId(Long waveId){
        return shipmentLineRepository.findByWaveId(waveId);
    }

    public List<Pick> allocateShipmentLine(ShipmentLine shipmentLine) {
        
    }
}
