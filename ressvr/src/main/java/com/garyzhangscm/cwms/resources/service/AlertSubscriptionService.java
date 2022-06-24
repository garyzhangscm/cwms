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
import com.garyzhangscm.cwms.resources.repository.EmailAlertConfigurationRepository;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.persistence.criteria.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class AlertSubscriptionService {
    private static final Logger logger = LoggerFactory.getLogger(AlertSubscriptionService.class);
    @Autowired
    private AlertSubscriptionRepository alertSubscriptionRepository;

    @Autowired
    private AlertTemplateService alertTemplateService;

    @Autowired
    private WebMessageAlertService webMessageAlertService;
    @Autowired
    private EMailService eMailService;
    @Autowired
    private UserService userService;


    public AlertSubscription findById(Long id) {
        AlertSubscription emailAlertConfiguration =  alertSubscriptionRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.raiseException("Email alert configuration not found by id: " + id));
        return emailAlertConfiguration;
    }


    public List<AlertSubscription> findAll(Long companyId,
                                           String username,
                                           String type,
                                           String deliveryChannel) {

        return alertSubscriptionRepository.findAll(
                (Root<AlertSubscription> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) -> {
                    List<Predicate> predicates = new ArrayList<Predicate>();

                    predicates.add(criteriaBuilder.equal(root.get("companyId"), companyId));

                    if (Strings.isNotBlank(username)) {

                        Join<AlertSubscription,User> joinUser = root.join("user",JoinType.INNER);

                        predicates.add(criteriaBuilder.equal(joinUser.get("username"), username));
                    }
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

    public AlertSubscription findUserSubscription(Long companyId,
                                                  User user,
                                                  AlertType type,
                                                  AlertDeliveryChannel deliveryChannel) {

        return alertSubscriptionRepository.findUserSubscription(
                companyId, user.getId(), type, deliveryChannel
        );
    }

    public AlertSubscription save(AlertSubscription alertSubscription) {
        return alertSubscriptionRepository.save(alertSubscription);
    }



    public AlertSubscription saveOrUpdate(AlertSubscription alertSubscription) {
        if (Objects.isNull(alertSubscription.getId()) &&
                !Objects.isNull(findUserSubscription(
                        alertSubscription.getCompanyId(), alertSubscription.getUser(),
                        alertSubscription.getType(), alertSubscription.getDeliveryChannel()))) {
            alertSubscription.setId(
                    findUserSubscription(
                            alertSubscription.getCompanyId(), alertSubscription.getUser(),
                            alertSubscription.getType(), alertSubscription.getDeliveryChannel()).getId());
        }
        return save(alertSubscription);
    }


    public AlertSubscription addAlertSubscription(AlertSubscription alertSubscription) {
        return saveOrUpdate(alertSubscription);

    }

    public AlertSubscription changeAlertSubscription(Long id, AlertSubscription alertSubscription) {
        alertSubscription.setId(id);
        return saveOrUpdate(alertSubscription);

    }

    public void delete(Long id) {
        alertSubscriptionRepository.deleteById(id);

    }

    public void sendAlert(Alert alert) {
        // get all the user that subscribed to this alert
        List<AlertSubscription> alertSubscriptions = findAll(
                alert.getCompanyId(),
                null,
                alert.getType().name(),
                null
        );

        for (AlertSubscription alertSubscription : alertSubscriptions) {
            sendAlert(alert, alertSubscription);
        }
    }
    public void sendAlert(Alert alert, AlertSubscription alertSubscription) {

        // setup the message
        // if it is setup already, then do nothing
        // else check if there's a template defined
        // -- if so, fill the template with parameters
        setupAlertMessage(alert, alertSubscription);

        switch (alertSubscription.getDeliveryChannel()) {
            case BY_SMS:
                // throw new UnsupportedOperationException("alert by SMS is not support yet");
                logger.debug("alert by SMS is not support yet");
                break;
            case BY_WEB_MESSAGE:
                sendWebMessageAlert(alert, alertSubscription);
                break;
            default:
                sendEmailAlert(alert, alertSubscription);
                break;
        }
    }

    /**
     * Setup the message for the alert
     * if the alert already have the message, then do nothing
     * else if the alert have a template, then fill the message with template
     *
     * @param alert
     */
    private void setupAlertMessage(Alert alert, AlertSubscription alertSubscription) {
        if (Strings.isNotBlank(alert.getMessage())) {
            return;
        }
        // check if we have a template associated with the alert and subscripion type

        alertTemplateService.processAlertTemplate(alert, alertSubscription);



    }

    /**
     * Send alert by web message
     * @param alert
     * @param alertSubscription
     */
    private void sendWebMessageAlert(Alert alert, AlertSubscription alertSubscription) {

        webMessageAlertService.addWebMessageAlert(alert, alertSubscription.getUser());
    }

    private void sendEmailAlert(Alert alert, AlertSubscription alertSubscription) {

        logger.debug("Start to send alert to user {} by email {}", alertSubscription.getUser().getName(),
                alertSubscription.getUser().getEmail());

        if (Strings.isNotBlank(alertSubscription.getUser().getEmail())) {

            eMailService.sendMail(
                    alert.getCompanyId(),
                    alertSubscription.getUser().getEmail(),
                    alert.getTitle(),
                    alert.getMessage()
            );
        }
    }

    public AlertSubscription removeAlertSubscription(Long id) {
        AlertSubscription alertSubscription = findById(id);
        delete(id);
        return alertSubscription;
    }

    public AlertSubscription subscribe(Long companyId, String alertType, String alertDeliveryChannel, String username) {
        // only add the subscription if the user hasn't subscribed to it yet
        List<AlertSubscription> userAlertSubscription = findAll(companyId, username, alertType, alertDeliveryChannel);
        if(userAlertSubscription.isEmpty()) {
            User user = userService.findByUsername(companyId,username, false);
            AlertSubscription alertSubscription =
                    new AlertSubscription(companyId, user,
                            AlertType.valueOf(alertType),
                            AlertDeliveryChannel.valueOf(alertDeliveryChannel),
                            "");
            return saveOrUpdate(alertSubscription);
        }
        else {
            // we should only have one record returned for the user & alert type & delivery channel
            return userAlertSubscription.get(0);
        }
    }
    public AlertSubscription unsubscribe(Long companyId, String alertType, String alertDeliveryChannel, String username) {
        // only remove the subscription if the user already subscribe to it
        List<AlertSubscription> userAlertSubscriptions = findAll(companyId, username, alertType, alertDeliveryChannel);
        if(userAlertSubscriptions.isEmpty()) {
            return null;
        }
        else {
            // we should only have one record returned for the user & alert type & delivery channel
            AlertSubscription alertSubscription = userAlertSubscriptions.get(0);
            userAlertSubscriptions.forEach(
                    userAlertSubscription -> delete(userAlertSubscription.getId())
            );
            return alertSubscription;
        }
    }

    public AlertSubscription changeKeyWords(Long companyId, String alertType,
                                            String alertDeliveryChannel, String username, String keyWordsList) {

        List<AlertSubscription> userAlertSubscriptions = findAll(companyId, username, alertType, alertDeliveryChannel);
        if (userAlertSubscriptions.isEmpty()) {
            return null;
        }
        return userAlertSubscriptions.stream().map(
                userAlertSubscription -> {
                    userAlertSubscription.setKeyWordsList(keyWordsList);
                    return saveOrUpdate(userAlertSubscription);
            }
        ).findFirst().get();
    }
}
