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
import com.garyzhangscm.cwms.resources.repository.AlertRepository;
import com.garyzhangscm.cwms.resources.repository.AlertSubscriptionRepository;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
public class AlertService {
    private static final Logger logger = LoggerFactory.getLogger(AlertService.class);
    @Autowired
    private AlertRepository alertRepository;
    @Autowired
    private AlertSubscriptionService alertSubscriptionService;


    public Alert findById(Long id) {
        Alert alert =  alertRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.raiseException("Alert not found by id: " + id));
        return alert;
    }


    public List<Alert> findAll(Long companyId,
                               String type,
                               String status,
                               String keyWords) {

        return alertRepository.findAll(
                (Root<Alert> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) -> {
                    List<Predicate> predicates = new ArrayList<Predicate>();

                    predicates.add(criteriaBuilder.equal(root.get("companyId"), companyId));

                    if (Strings.isNotBlank(type)) {

                        predicates.add(criteriaBuilder.equal(root.get("type"), AlertType.valueOf(type)));
                    }
                    if (Strings.isNotBlank(status)) {

                        predicates.add(criteriaBuilder.equal(root.get("status"), AlertStatus.valueOf(status)));
                    }
                    if (Strings.isNotBlank(keyWords)) {

                        if (keyWords.contains("%")) {

                            predicates.add(criteriaBuilder.like(root.get("keyWords"), keyWords));
                        }
                        else {

                            predicates.add(criteriaBuilder.equal(root.get("keyWords"), keyWords));
                        }
                    }

                    Predicate[] p = new Predicate[predicates.size()];
                    return criteriaBuilder.and(predicates.toArray(p));
                }
        );
    }

    public Alert findByKeyWords(Long companyId, String keyWords) {

        return alertRepository.findByCompanyIdAndKeyWords(companyId, keyWords);
    }

    public List<Alert> findPendingAlerts() {

        return alertRepository.findByStatus(AlertStatus.PENDING);
    }


    public Alert save(Alert alert) {
        return alertRepository.save(alert);
    }



    public Alert saveOrUpdate(Alert alert) {
        if (Objects.isNull(alert.getId()) &&
                !Objects.isNull(findByKeyWords(
                        alert.getCompanyId(), alert.getKeyWords()))) {
            alert.setId(
                    findByKeyWords(alert.getCompanyId(), alert.getKeyWords()).getId());
        }
        return save(alert);
    }


    public Alert addAlert(Alert alert) {
        alert.setStatus(AlertStatus.PENDING);
        return saveOrUpdate(alert);

    }

    public Alert changeAlert(Long id, Alert alert) {
        alert.setId(id);
        return saveOrUpdate(alert);

    }

    public void delete(Long id) {
        alertRepository.deleteById(id);

    }

    public void sendAlert(Alert alert) {

        alert.setStatus(AlertStatus.IN_PROCESS);
        saveOrUpdate(alert);
        logger.debug("Start to send alert \n {}", alert);
        alertSubscriptionService.sendAlert(alert);
        // set the status of the alert to Send
        alert.setStatus(AlertStatus.SENT);
        saveOrUpdate(alert);
    }

    /**
     * Reset the alert's status back to PENDING so it will be sent again
     * @param id
     * @return
     */
    public Alert resetAlert(Long id) {
        Alert alert = findById(id);
        alert.setStatus(AlertStatus.PENDING);
        return saveOrUpdate(alert);
    }
}
