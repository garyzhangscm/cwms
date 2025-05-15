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

package com.garyzhangscm.cwms.layout.service;

import com.garyzhangscm.cwms.layout.exception.ResourceNotFoundException;
import com.garyzhangscm.cwms.layout.model.*;
import com.garyzhangscm.cwms.layout.repository.PickZoneRepository;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.persistence.criteria.*;
import javax.transaction.Transactional;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class PickZoneService   {

    private static final Logger logger = LoggerFactory.getLogger(PickZoneService.class);
    @Autowired
    private PickZoneRepository pickZoneRepository;
    
    @Autowired
    private CompanyService companyService;
    @Autowired
    private WarehouseService warehouseService;
    @Autowired
    private LocationService locationService; 

    public PickZone findById(Long id) {
        return pickZoneRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.raiseException("location group not found by id: " + id));
    }


    public List<PickZone> findAll(Long warehouseId, String name) {
        return pickZoneRepository.findAll(
                (Root<PickZone> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) -> {
                    List<Predicate> predicates = new ArrayList<Predicate>();

                    Join<PickZone, Warehouse> joinWarehouse = root.join("warehouse", JoinType.INNER);
                    predicates.add(criteriaBuilder.equal(joinWarehouse.get("id"), warehouseId));

                    if (StringUtils.isNotBlank(name)) {
                        predicates.add(criteriaBuilder.equal(root.get("name"), name));
                    }

                    Predicate[] p = new Predicate[predicates.size()];
                    return criteriaBuilder.and(predicates.toArray(p));
                }
        );

    }

    public PickZone findByName(Long warehouseId, String name){
        return pickZoneRepository.findByName(warehouseId, name);
    }

    public PickZone save(PickZone pickZone) {
        return pickZoneRepository.save(pickZone);
    }

    public PickZone saveOrUpdate(PickZone pickZone) {
        if (Objects.isNull(pickZone.getId()) &&
                Objects.nonNull(findByName(pickZone.getWarehouse().getId(), pickZone.getName()))) {
            pickZone.setId(findByName(pickZone.getWarehouse().getId(), pickZone.getName()).getId());
        }
        return save(pickZone);
    }
    public void delete(PickZone pickZone) {
        pickZoneRepository.delete(pickZone);
    }
    public void delete(Long id) {
        pickZoneRepository.deleteById(id);
    }

    public void removePickZone(long id) {
        PickZone pickZone = findById(id);
        // Remove all the locations in this zone
        locationService.removeLocationByPickZone(pickZone);

        // remove the pick zone
        delete(id);
    }

    public PickZone addPickZone(PickZone pickZone) {
        pickZone.getPickableUnitOfMeasures().forEach(
                pickableUnitOfMeasure -> pickableUnitOfMeasure.setPickZone(pickZone)
        );
        return saveOrUpdate(pickZone);
    }

    @Transactional
    public void removePickZones(Warehouse warehouse) {
        pickZoneRepository.deleteByWarehouseId(warehouse.getId());
    }
}
