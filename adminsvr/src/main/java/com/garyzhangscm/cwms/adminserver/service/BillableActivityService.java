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

package com.garyzhangscm.cwms.adminserver.service;

import com.garyzhangscm.cwms.adminserver.clients.ResourceServiceRestemplateClient;
import com.garyzhangscm.cwms.adminserver.clients.WarehouseLayoutServiceRestemplateClient;
import com.garyzhangscm.cwms.adminserver.exception.ResourceNotFoundException;
import com.garyzhangscm.cwms.adminserver.model.*;
import com.garyzhangscm.cwms.adminserver.model.wms.Warehouse;
import com.garyzhangscm.cwms.adminserver.repository.BillableActivityRepository;
import com.garyzhangscm.cwms.adminserver.repository.BillableRequestRepository;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.*;

@Service
public class BillableActivityService {

    private static final Logger logger = LoggerFactory.getLogger(BillableActivityService.class);

    @Autowired
    private BillableActivityRepository billableActivityRepository;

    @Autowired
    private WarehouseLayoutServiceRestemplateClient warehouseLayoutServiceRestemplateClient;


    public BillableActivity findById(Long id) {
        return billableActivityRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.raiseException("billable activity not found by id: " + id));

    }




    public List<BillableActivity> findAll(Long companyId,
                                         Long warehouseId,
                                         Long clientId,
                                         ZonedDateTime startTime,
                                         ZonedDateTime endTime,
                                         String category
                                         ) {

        return
                billableActivityRepository.findAll(
                (Root<BillableActivity> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) -> {
                    List<Predicate> predicates = new ArrayList<Predicate>();

                    predicates.add(criteriaBuilder.equal(root.get("companyId"), companyId));
                    predicates.add(criteriaBuilder.equal(root.get("warehouseId"), warehouseId));

                    if (Objects.nonNull(clientId)) {

                        predicates.add(criteriaBuilder.equal(root.get("clientId"), clientId));
                    }

                    if (Objects.nonNull(startTime)) {
                        predicates.add(criteriaBuilder.greaterThanOrEqualTo(
                                root.get("createdTime"), startTime));

                    }

                    if (Objects.nonNull(endTime)) {
                        predicates.add(criteriaBuilder.lessThanOrEqualTo(
                                root.get("createdTime"), endTime));

                    }

                    if (Strings.isNotBlank(category)) {

                        predicates.add(criteriaBuilder.equal(root.get("billableCategory"), BillableCategory.valueOf(category)));
                    }

                    Predicate[] p = new Predicate[predicates.size()];
                    return criteriaBuilder.and(predicates.toArray(p));
                }
        );

    }

    public BillableActivity save(BillableActivity billableActivity) {
        return billableActivityRepository.save(billableActivity);
    }

    public void addBillableActivity(BillableActivity billableActivity) {
        if (Objects.isNull(billableActivity.getCompanyId()) &&
                Objects.nonNull(billableActivity.getWarehouseId())) {

            Warehouse warehouse =
                    warehouseLayoutServiceRestemplateClient.getWarehouseById(
                            billableActivity.getWarehouseId()
                    );
            billableActivity.setCompanyId(warehouse.getCompanyId());
        }

        save(billableActivity);
    }
}
