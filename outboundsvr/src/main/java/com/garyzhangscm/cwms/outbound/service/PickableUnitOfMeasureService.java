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
import com.garyzhangscm.cwms.outbound.model.*;

import com.garyzhangscm.cwms.outbound.repository.PickableUnitOfMeasureRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.stereotype.Service;

import java.util.*;



@Service
public class PickableUnitOfMeasureService {
    private static final Logger logger = LoggerFactory.getLogger(PickableUnitOfMeasureService.class);

    @Autowired
    private PickableUnitOfMeasureRepository pickableUnitOfMeasureRepository;


    public PickableUnitOfMeasure findById(Long id) {
        return pickableUnitOfMeasureRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.raiseException("pickable unit of measure not found by id: " + id));
    }

    public List<PickableUnitOfMeasure> findAll() {
        return pickableUnitOfMeasureRepository.findAll();
    }

    public PickableUnitOfMeasure save(PickableUnitOfMeasure pickableUnitOfMeasure) {
        logger.debug("Save pickableUnitOfMeasure\n{}", pickableUnitOfMeasure);
        return pickableUnitOfMeasureRepository.save(pickableUnitOfMeasure);
    }



    public void delete(PickableUnitOfMeasure pickableUnitOfMeasure) {
        pickableUnitOfMeasureRepository.delete(pickableUnitOfMeasure);
    }

    public void delete(Long id) {
        pickableUnitOfMeasureRepository.deleteById(id);
    }

    public void delete(String pickableUnitOfMeasureIds) {
        if (!pickableUnitOfMeasureIds.isEmpty()) {
            long[] pickableUnitOfMeasureIdArray = Arrays.asList(pickableUnitOfMeasureIds.split(",")).stream().mapToLong(Long::parseLong).toArray();
            for (long id : pickableUnitOfMeasureIdArray) {
                delete(id);
            }
        }
    }

}
