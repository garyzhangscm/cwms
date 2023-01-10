/**
 * Copyright 2019
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

package com.garyzhangscm.cwms.resources.service;

import com.garyzhangscm.cwms.resources.exception.EmailException;
import com.garyzhangscm.cwms.resources.exception.ResourceNotFoundException;
import com.garyzhangscm.cwms.resources.model.*;
import com.garyzhangscm.cwms.resources.repository.AlertRepository;
import com.garyzhangscm.cwms.resources.repository.WarehouseAccessRepository;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.persistence.criteria.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
public class WarehouseAccessService {
    private static final Logger logger = LoggerFactory.getLogger(WarehouseAccessService.class);
    @Autowired
    private WarehouseAccessRepository warehouseAccessRepository;


    public WarehouseAccess findById(Long id) {
        return warehouseAccessRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.raiseException("Warehouse Access not found by id: " + id));
    }


    public List<WarehouseAccess> findAll(Long warehouseId,
                                         String username,
                                         Long userId) {

        return warehouseAccessRepository.findAll(
                (Root<WarehouseAccess> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) -> {
                    List<Predicate> predicates = new ArrayList<Predicate>();

                    predicates.add(criteriaBuilder.equal(root.get("warehouseId"), warehouseId));

                    if (Strings.isNotBlank(username) || Objects.nonNull(userId)) {

                        Join<WarehouseAccess,User> joinUser = root.join("user",JoinType.INNER);

                        if (Strings.isNotBlank(username)) {

                            predicates.add(criteriaBuilder.equal(joinUser.get("username"), username));
                        }
                        if (Objects.nonNull(userId)) {

                            predicates.add(criteriaBuilder.equal(joinUser.get("id"), userId));
                        }
                    }


                    Predicate[] p = new Predicate[predicates.size()];
                    return criteriaBuilder.and(predicates.toArray(p));
                }
        );
    }

    public WarehouseAccess findByWarehouseIdAndUserId(Long warehouseId, Long userId) {

        return warehouseAccessRepository.findByWarehouseIdAndUserId(warehouseId, userId);
    }

    public boolean hasAccess(Long warehouseId, Long userId) {
        return Objects.nonNull(
                findByWarehouseIdAndUserId(warehouseId, userId)
        );
    }

    public WarehouseAccess save(WarehouseAccess warehouseAccess) {
        return warehouseAccessRepository.save(warehouseAccess);
    }



    public WarehouseAccess saveOrUpdate(WarehouseAccess warehouseAccess) {
        if (Objects.isNull(warehouseAccess.getId()) &&
                !Objects.isNull(findByWarehouseIdAndUserId(
                        warehouseAccess.getWarehouseId(), warehouseAccess.getUser().getId()))) {
            warehouseAccess.setId(
                    findByWarehouseIdAndUserId(warehouseAccess.getWarehouseId(), warehouseAccess.getUser().getId()).getId());
        }
        return save(warehouseAccess);
    }


    public WarehouseAccess addAccess(WarehouseAccess warehouseAccess) {

        return saveOrUpdate(warehouseAccess);

    }

    public void removeAccess(WarehouseAccess warehouseAccess) {
        if (Objects.nonNull(warehouseAccess.getId())) {
            delete(warehouseAccess.getId());
        }
        else {
            WarehouseAccess existingWarehouseAccess = findByWarehouseIdAndUserId(
                    warehouseAccess.getWarehouseId(), warehouseAccess.getUser().getId()
            );
            if (Objects.nonNull(existingWarehouseAccess)) {
                delete(existingWarehouseAccess.getId());
            }
        }
    }
    public void removeAccess(Long warehouseId, Long userId) {
        WarehouseAccess warehouseAccess = findByWarehouseIdAndUserId(
                warehouseId, userId);
        if (Objects.nonNull(warehouseAccess)) {
            delete(warehouseAccess.getId());
        }
    }

    public void delete(Long id) {
        warehouseAccessRepository.deleteById(id);

    }

}
