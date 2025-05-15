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

package com.garyzhangscm.cwms.workorder.service;

import com.garyzhangscm.cwms.workorder.exception.ResourceNotFoundException;
import com.garyzhangscm.cwms.workorder.model.lightMES.LightMESConfiguration;
import com.garyzhangscm.cwms.workorder.repository.LightMESConfigurationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


@Service
public class LightMESConfigurationService {
    private static final Logger logger = LoggerFactory.getLogger(LightMESConfigurationService.class);

    @Autowired
    private LightMESConfigurationRepository lightMESConfigurationRepository;

    public LightMESConfiguration findById(Long id) {
        return lightMESConfigurationRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.raiseException("light MES configuration not found by id: " + id));

    }



    public LightMESConfiguration findByWarehouse(Long warehouseId) {
        return lightMESConfigurationRepository.findByWarehouseId(warehouseId);
    }



    public LightMESConfiguration save(LightMESConfiguration lightMESConfiguration) {
        return lightMESConfigurationRepository.save(lightMESConfiguration);
    }

    public LightMESConfiguration saveOrUpdate(LightMESConfiguration lightMESConfiguration) {
        if (lightMESConfiguration.getId() == null && findByWarehouse(
                lightMESConfiguration.getWarehouseId()) != null) {
            lightMESConfiguration.setId(findByWarehouse(
                    lightMESConfiguration.getWarehouseId()).getId());
        }
        return save(lightMESConfiguration);
    }


    public void delete(LightMESConfiguration lightMESConfiguration) {
        lightMESConfigurationRepository.delete(lightMESConfiguration);
    }

    public void delete(Long id) {
        lightMESConfigurationRepository.deleteById(id);
    }



    public LightMESConfiguration addLightMESConfiguration(LightMESConfiguration lightMESConfiguration) {
        return saveOrUpdate(lightMESConfiguration);
    }

    public LightMESConfiguration changeLightMESConfiguration(Long id, LightMESConfiguration lightMESConfiguration) {
        lightMESConfiguration.setId(id);
        return saveOrUpdate(lightMESConfiguration);

    }
}
