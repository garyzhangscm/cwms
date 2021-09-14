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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.garyzhangscm.cwms.adminserver.clients.CommonServiceRestemplateClient;
import com.garyzhangscm.cwms.adminserver.clients.ResourceServiceRestemplateClient;
import com.garyzhangscm.cwms.adminserver.clients.WarehouseLayoutServiceRestemplateClient;
import com.garyzhangscm.cwms.adminserver.exception.ResourceNotFoundException;
import com.garyzhangscm.cwms.adminserver.exception.SystemFatalException;
import com.garyzhangscm.cwms.adminserver.model.BillableRequest;
import com.garyzhangscm.cwms.adminserver.model.DataInitialRequest;
import com.garyzhangscm.cwms.adminserver.model.DataInitialRequestStatus;
import com.garyzhangscm.cwms.adminserver.model.User;
import com.garyzhangscm.cwms.adminserver.model.wms.*;
import com.garyzhangscm.cwms.adminserver.repository.BillableRequestRepository;
import com.garyzhangscm.cwms.adminserver.repository.DataInitialRequestRepository;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.persistence.criteria.*;
import java.util.*;

@Service
public class BillableRequestService {

    private static final Logger logger = LoggerFactory.getLogger(BillableRequestService.class);

    @Autowired
    private BillableRequestRepository billableRequestRepository;

    public BillableRequest findById(Long id ) {
        BillableRequest billableRequest = billableRequestRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.raiseException("billable request not found by id: " + id));
        return billableRequest;
    }

    public List<BillableRequest> findAll(Long companyId,
                              Long warehouseId) {

        return billableRequestRepository.findAll(
                (Root<BillableRequest> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) -> {
                    List<Predicate> predicates = new ArrayList<Predicate>();

                    if (Objects.isNull(companyId)) {

                        predicates.add(criteriaBuilder.equal(root.get("companyId"), companyId));
                    }
                    if (Objects.isNull(warehouseId)) {

                        predicates.add(criteriaBuilder.equal(root.get("warehouseId"), warehouseId));
                    }

                    Predicate[] p = new Predicate[predicates.size()];
                    return criteriaBuilder.and(predicates.toArray(p));
                }
        );

    }

    public BillableRequest save(BillableRequest billableRequest) {
        return billableRequestRepository.save(billableRequest);
    }

}
