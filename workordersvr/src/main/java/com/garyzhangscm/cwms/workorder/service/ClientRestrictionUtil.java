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

package com.garyzhangscm.cwms.workorder.service;

import com.garyzhangscm.cwms.workorder.model.ClientRestriction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import jakarta.persistence.criteria.CriteriaBuilder;

import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

@Service
public class ClientRestrictionUtil {

    private static final Logger logger = LoggerFactory.getLogger(ClientRestrictionUtil.class);

    /**
     * Add client restriction to the other user specified query paramaters
     * @param predicates
     * @param clientRestriction
     * @return
     * @throws IOException
     */
    public Predicate addClientRestriction(Root root,
                                          List<Predicate> predicates,
                                          ClientRestriction clientRestriction,
                                          CriteriaBuilder criteriaBuilder) {

        Predicate[] p = new Predicate[predicates.size()];

        // special handling for 3pl
        Predicate predicate = criteriaBuilder.and(predicates.toArray(p));

        if (Objects.isNull(clientRestriction) ||
                !Boolean.TRUE.equals(clientRestriction.getThreePartyLogisticsFlag()) ||
                Boolean.TRUE.equals(clientRestriction.getAllClientAccess())) {
            // not a 3pl warehouse, let's not put any restriction on the client
            // (unless the client restriction is from the web request, which we already
            // handled previously
            return predicate;
        }


        // build the accessible client list predicated based on the
        // client ID that the user has access
        Predicate accessibleClientListPredicate;
        if (clientRestriction.getClientAccesses().trim().isEmpty()) {
            // the user can't access any client, then the user
            // can only access the non 3pl data
            accessibleClientListPredicate = criteriaBuilder.isNull(root.get("clientId"));
        }
        else {
            CriteriaBuilder.In<Long> inClientIds = criteriaBuilder.in(root.get("clientId"));
            for(String id : clientRestriction.getClientAccesses().trim().split(",")) {
                inClientIds.value(Long.parseLong(id));
            }
            accessibleClientListPredicate = criteriaBuilder.and(inClientIds);
        }

        if (Boolean.TRUE.equals(clientRestriction.getNonClientDataAccessible())) {
            // the user can access the non 3pl data
            return criteriaBuilder.and(predicate,
                    criteriaBuilder.or(
                            criteriaBuilder.isNull(root.get("clientId")),
                            accessibleClientListPredicate));
        }
        else {

            // the user can NOT access the non 3pl data
            return criteriaBuilder.and(predicate,
                    criteriaBuilder.and(
                            criteriaBuilder.isNotNull(root.get("clientId")),
                            accessibleClientListPredicate));
        }

    }


}
