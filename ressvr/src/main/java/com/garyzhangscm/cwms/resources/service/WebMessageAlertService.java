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

import com.garyzhangscm.cwms.resources.exception.ResourceNotFoundException;
import com.garyzhangscm.cwms.resources.model.*;
import com.garyzhangscm.cwms.resources.repository.AlertSubscriptionRepository;
import com.garyzhangscm.cwms.resources.repository.WebMessageAlertRepository;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.autoconfigure.metrics.MetricsProperties;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import javax.persistence.criteria.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
public class WebMessageAlertService {
    private static final Logger logger = LoggerFactory.getLogger(WebMessageAlertService.class);
    @Autowired
    private WebMessageAlertRepository webMessageAlertRepository;

    private static  final int pageSize = 10;

    public WebMessageAlert findById(Long id) {
        WebMessageAlert webMessageAlert =  webMessageAlertRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.raiseException("Web Message Alert not found by id: " + id));
        return webMessageAlert;
    }


    public List<WebMessageAlert> findAll(Long companyId,
                                         String username,
                                         Long alertId,
                                         Boolean readFlag) {
        return findAll(companyId, username, alertId, readFlag, 0, Integer.MAX_VALUE);
    }

    public List<WebMessageAlert> findAll(Long companyId,
                                         String username,
                                         Long alertId,
                                         Boolean readFlag,
                                         Integer pageNumber) {
        return findAll(companyId, username, alertId, readFlag, pageNumber, pageSize);
    }

    public List<WebMessageAlert> findAll(Long companyId,
                                         String username,
                                         Long alertId,
                                         Boolean readFlag,
                                         Integer pageNumber,
                                         Integer pageSize) {

        Pageable pageable = PageRequest.of(
                Objects.isNull(pageNumber) ? 0 : pageNumber,
                Objects.isNull(pageSize) ? pageSize : Integer.MAX_VALUE,
                Sort.by(Sort.Direction.DESC, "createdTime", "id"));

        Page<WebMessageAlert> webMessageAlerts =  webMessageAlertRepository.findAll(
                (Root<WebMessageAlert> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) -> {

                    List<Predicate> predicates = new ArrayList<Predicate>();

                    predicates.add(criteriaBuilder.equal(root.get("companyId"), companyId));

                    if (Objects.nonNull(alertId)) {

                        Join<WebMessageAlert,Alert> joinAlert = root.join("alert",JoinType.INNER);

                        predicates.add(criteriaBuilder.equal(joinAlert.get("id"), alertId));
                    }

                    if (Strings.isNotBlank(username)) {

                        Join<WebMessageAlert,User> joinUser = root.join("user",JoinType.INNER);

                        predicates.add(criteriaBuilder.equal(joinUser.get("username"), username));
                    }
                    if (Objects.nonNull(readFlag)) {

                        predicates.add(criteriaBuilder.equal(root.get("readFlag"), readFlag));
                    }
                    Predicate[] p = new Predicate[predicates.size()];
                    return criteriaBuilder.and(predicates.toArray(p));
                },
                pageable
        );

        return webMessageAlerts.getContent();

    }



    public WebMessageAlert save(WebMessageAlert webMessageAlert) {
        return webMessageAlertRepository.save(webMessageAlert);
    }



    public WebMessageAlert saveOrUpdate(WebMessageAlert webMessageAlert) {
        if (Objects.isNull(webMessageAlert.getId()) &&
                !Objects.isNull(findUserWebMessage(
                        webMessageAlert.getUser(),
                        webMessageAlert.getAlert()))) {
            webMessageAlert.setId(
                    findUserWebMessage(
                            webMessageAlert.getUser(),
                            webMessageAlert.getAlert()).getId());
        }
        return save(webMessageAlert);
    }

    private WebMessageAlert findUserWebMessage(User user, Alert alert) {
        List<WebMessageAlert> webMessageAlerts = findAll(
                alert.getCompanyId(),
                user.getUsername(),
                alert.getId(), null
        );
        if (webMessageAlerts.isEmpty()) {
            return  null;
        }
        return webMessageAlerts.get(0);
    }


    public WebMessageAlert addWebMessageAlert(WebMessageAlert webMessageAlert) {
        return saveOrUpdate(webMessageAlert);

    }

    public WebMessageAlert addWebMessageAlert(Alert alert, User user, boolean readFlag) {
        return addWebMessageAlert(
                new WebMessageAlert(
                        alert, user, readFlag
                )
        );

    }
    public  WebMessageAlert addWebMessageAlert(Alert alert, User user) {
        return addWebMessageAlert(alert, user, false);
    }

    public WebMessageAlert readWebMessageAlert(Long webMessageAlertId) {
        WebMessageAlert webMessageAlert = findById(webMessageAlertId);
        // if the user already read the alert, then do nothing
        // else mark the message as read
        if (Boolean.TRUE.equals(webMessageAlert.getReadFlag())) {
            return webMessageAlert;
        }
        webMessageAlert.setReadFlag(true);
        webMessageAlert.setReadDate(LocalDateTime.now());
        return saveOrUpdate(webMessageAlert);
    }

    public List<WebMessageAlert> getUserUnreadWebMessageAlert(Long companyId, String username) {
        return findAll(companyId, username, null, false );
    }
}
