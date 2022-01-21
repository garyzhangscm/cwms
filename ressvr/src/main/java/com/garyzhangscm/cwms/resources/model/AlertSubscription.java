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

package com.garyzhangscm.cwms.resources.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.persistence.*;

@Entity
@Table(name = "alert_subscription")
public class AlertSubscription extends AuditibleEntity<String>  {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "alert_subscription_id")
    private Long id;

    @Column(name = "company_id")
    private Long companyId;

    @ManyToOne
    @JoinColumn(name="user_id")
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "type")
    private AlertType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "delivery_channel")
    private AlertDeliveryChannel deliveryChannel;


    // may filter out the alert by key words
    // so that the user will only get the alert
    // when the key wards of the alert contains any
    // of the key words from the list.
    // The list will contains a list of key words
    // separated by ,
    @Column(name = "key_words_list")
    private String keyWordsList;
    public AlertSubscription(){}
    public AlertSubscription(Long companyId, User user, AlertType type, AlertDeliveryChannel deliveryChannel, String keyWordsList) {
        this.companyId = companyId;
        this.user = user;
        this.type = type;
        this.deliveryChannel = deliveryChannel;
        this.keyWordsList = keyWordsList;
    }

    @Override
    public String toString() {
        try {
            return new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(this);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return null;
    }


    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getCompanyId() {
        return companyId;
    }

    public void setCompanyId(Long companyId) {
        this.companyId = companyId;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getKeyWordsList() {
        return keyWordsList;
    }

    public void setKeyWordsList(String keyWordsList) {
        this.keyWordsList = keyWordsList;
    }

    public AlertType getType() {
        return type;
    }

    public void setType(AlertType type) {
        this.type = type;
    }

    public AlertDeliveryChannel getDeliveryChannel() {
        return deliveryChannel;
    }

    public void setDeliveryChannel(AlertDeliveryChannel deliveryChannel) {
        this.deliveryChannel = deliveryChannel;
    }
}
