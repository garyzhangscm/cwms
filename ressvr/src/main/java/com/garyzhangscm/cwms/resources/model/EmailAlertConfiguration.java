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
import java.util.Objects;
import java.util.Properties;

@Entity
@Table(name = "email_alert_configuration")
public class EmailAlertConfiguration extends AuditibleEntity<String>  {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "email_alert_configuration_id")
    private Long id;

    @Column(name = "company_id")
    private Long companyId;

    @Column(name = "host")
    private String host;
    @Column(name = "port")
    private int port;

    @Column(name = "username")
    private String username;
    @Column(name = "password")
    private String password;

    // alert send from the email address
    @Column(name = "send_from_email")
    private String sendFromEmail;

    @Column(name = "transport_protocol")
    private String transportProtocol;
    @Column(name = "auth_flag")
    private Boolean authFlag;
    @Column(name = "starttls_enable_flag")
    private Boolean starttlsEnableFlag;
    @Column(name = "debug_flag")
    private Boolean debugFlag;


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

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getSendFromEmail() {
        return sendFromEmail;
    }

    public void setSendFromEmail(String sendFromEmail) {
        this.sendFromEmail = sendFromEmail;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getTransportProtocol() {
        return transportProtocol;
    }

    public void setTransportProtocol(String transportProtocol) {
        this.transportProtocol = transportProtocol;
    }

    public Boolean getAuthFlag() {
        return authFlag;
    }

    public void setAuthFlag(Boolean authFlag) {
        this.authFlag = authFlag;
    }

    public Boolean getStarttlsEnableFlag() {
        return starttlsEnableFlag;
    }

    public void setStarttlsEnableFlag(Boolean starttlsEnableFlag) {
        this.starttlsEnableFlag = starttlsEnableFlag;
    }

    public Boolean getDebugFlag() {
        return debugFlag;
    }

    public void setDebugFlag(Boolean debugFlag) {
        this.debugFlag = debugFlag;
    }
}
