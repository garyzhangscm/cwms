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

import com.garyzhangscm.cwms.resources.exception.GenericException;
import com.garyzhangscm.cwms.resources.exception.ResourceNotFoundException;
import com.garyzhangscm.cwms.resources.exception.UserOperationException;
import com.garyzhangscm.cwms.resources.model.*;
import com.garyzhangscm.cwms.resources.repository.AlertSubscriptionRepository;
import com.garyzhangscm.cwms.resources.repository.AlertTemplateRepository;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.persistence.criteria.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class AlertTemplateService {
    private static final Logger logger = LoggerFactory.getLogger(AlertTemplateService.class);
    @Autowired
    private AlertTemplateRepository alertTemplateRepository;



    public AlertTemplate findById(Long id) {
        AlertTemplate alertTemplate =  alertTemplateRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.raiseException("Alert Template not found by id: " + id));
        return alertTemplate;
    }


    public List<AlertTemplate> findAll(Long companyId,
                                           String type,
                                           String deliveryChannel) {

        return alertTemplateRepository.findAll(
                (Root<AlertTemplate> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) -> {
                    List<Predicate> predicates = new ArrayList<Predicate>();

                    predicates.add(criteriaBuilder.equal(root.get("companyId"), companyId));

                    if (Strings.isNotBlank(type)) {

                        predicates.add(criteriaBuilder.equal(root.get("type"), AlertType.valueOf(type)));
                    }
                    if (Strings.isNotBlank(deliveryChannel)) {

                        predicates.add(criteriaBuilder.equal(root.get("deliveryChannel"), AlertDeliveryChannel.valueOf(deliveryChannel)));
                    }


                    Predicate[] p = new Predicate[predicates.size()];
                    return criteriaBuilder.and(predicates.toArray(p));
                }
        );
    }


    public AlertTemplate save(AlertTemplate alertTemplate) {
        return alertTemplateRepository.save(alertTemplate);
    }

    public AlertTemplate getAlertTemplateByTypeAndDeliveryChannel(
            Long companyId,
            AlertType type,
            AlertDeliveryChannel deliveryChannel
    ) {
        // make sure the 3 parameters are all passed in
        if (Objects.isNull(companyId)) {
            throw UserOperationException.raiseException("Company ID is required to get the alert template");
        }
        if (Objects.isNull(type)) {
            throw UserOperationException.raiseException("Alert type is required to get the alert template");
        }
        if (Objects.isNull(deliveryChannel)) {
            throw UserOperationException.raiseException("Alert Delivery Channel is required to get the alert template");
        }

        List<AlertTemplate> alertTemplates = findAll(companyId, type.toString(), deliveryChannel.toString());
        if (alertTemplates.isEmpty()) {
            return null;
        }
        return alertTemplates.get(0);

    }


    public AlertTemplate saveOrUpdate(AlertTemplate alertTemplate) {
        if (Objects.isNull(alertTemplate.getId()) &&
                !Objects.isNull(getAlertTemplateByTypeAndDeliveryChannel(
                        alertTemplate.getCompanyId(),
                        alertTemplate.getType(), alertTemplate.getDeliveryChannel()))) {
            alertTemplate.setId(
                    getAlertTemplateByTypeAndDeliveryChannel(
                            alertTemplate.getCompanyId(),
                            alertTemplate.getType(), alertTemplate.getDeliveryChannel()).getId());
        }
        return save(alertTemplate);
    }


    public AlertTemplate addAlertTemplate(AlertTemplate alertTemplate) {
        return saveOrUpdate(alertTemplate);

    }

    public AlertTemplate changeAlertTemplate(Long id, AlertTemplate alertTemplate) {
        alertTemplate.setId(id);
        return saveOrUpdate(alertTemplate);

    }

    public void delete(Long id) {
        alertTemplateRepository.deleteById(id);

    }

    /**
     * find the alert template by alert and alert subscription
     * from subscription, we can get the alert type and channel
     * @param alert
     * @param alertSubscription
     * @return
     */
    public AlertTemplate findMatchedAlertTemplate(Alert alert, AlertSubscription alertSubscription) {
        return getAlertTemplateByTypeAndDeliveryChannel(
                alert.getCompanyId(), alert.getType(),
                alertSubscription.getDeliveryChannel()
        );
    }

    public void processAlertTemplate(Alert alert, AlertSubscription alertSubscription) {
        AlertTemplate alertTemplate = findMatchedAlertTemplate(alert, alertSubscription);
        if (Objects.isNull(alertTemplate)) {
            // OK, there's no template defined for the alert and delivery channel combination
            // let's return nothing
            return;
        }
        // let's get the parameters map from the alert
        fillTemplate(alert, alertTemplate);

    }

    /**
     * Fill in the template and replace with the message
     * @param alert
     * @param alertTemplate
     */
    private void fillTemplate(Alert alert, AlertTemplate alertTemplate) {
        String template = alertTemplate.getTemplate();
        Map<String, String> parameterMap = alert.getParameterMap();
        logger.debug("start to fill template for alert \n {}", alert);
        logger.debug("alert template: \n{}", alertTemplate);
        parameterMap.forEach((key, value) -> {
            template.replaceAll("@" + key + "@", value);
        });

        logger.debug("after replacement, alert template: \n{}", alertTemplate);
        alert.setMessage(template);

    }

}
