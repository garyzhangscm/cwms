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

package com.garyzhangscm.cwms.quickbook.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.codehaus.jackson.annotate.JsonProperty;

import javax.persistence.*;
import java.io.Serializable;
import java.time.ZonedDateTime;


@Entity
@Table(name = "quickbook_online_token")
public class QuickBookOnlineToken extends AuditibleEntity<String> implements Serializable {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "quickbook_online_token_id")
    @JsonProperty(value="id")
    private Long id;

    @Column(name = "warehouse_id")
    private Long warehouseId;

    @Transient
    private Warehouse warehouse;

    @Column(name = "company_id")
    private Long companyId;

    @Transient
    private Company company;

    @Column(name = "realm_id")
    private String realmId;

    @Column(name = "authorization_code")
    private String authorizationCode;

    @Column(name = "token")
    private String token;

    @Column(name = "refresh_token")
    private String refreshToken;


    @Column(name = "last_token_request_time")
    @JsonDeserialize(using = CustomZonedDateTimeDeserializer.class)
    @JsonSerialize(using = CustomZonedDateTimeSerializer.class)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private ZonedDateTime lastTokenRequestTime;
    // @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    // @JsonSerialize(using = LocalDateTimeSerializer.class)
    // @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    // private LocalDateTime lastTokenRequestTime;

    // last call to QBO CDC api
    @Column(name = "last_cdc_call_time")
    private String lastCDCCallTime;

    public QuickBookOnlineToken() {

    }
    public QuickBookOnlineToken(Long companyId, Long warehouseId, String realmId,
                                String authorizationCode, String token,
                                String refreshToken, ZonedDateTime lastTokenRequestTime) {
        this.warehouseId = warehouseId;
        this.companyId = companyId;
        this.realmId = realmId;
        this.authorizationCode = authorizationCode;
        this.token = token;
        this.refreshToken = refreshToken;
        this.lastTokenRequestTime = lastTokenRequestTime;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getWarehouseId() {
        return warehouseId;
    }

    public void setWarehouseId(Long warehouseId) {
        this.warehouseId = warehouseId;
    }

    public Warehouse getWarehouse() {
        return warehouse;
    }

    public void setWarehouse(Warehouse warehouse) {
        this.warehouse = warehouse;
    }

    public Long getCompanyId() {
        return companyId;
    }

    public void setCompanyId(Long companyId) {
        this.companyId = companyId;
    }

    public Company getCompany() {
        return company;
    }

    public void setCompany(Company company) {
        this.company = company;
    }

    public String getRealmId() {
        return realmId;
    }

    public void setRealmId(String realmId) {
        this.realmId = realmId;
    }

    public String getAuthorizationCode() {
        return authorizationCode;
    }

    public void setAuthorizationCode(String authorizationCode) {
        this.authorizationCode = authorizationCode;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public ZonedDateTime getLastTokenRequestTime() {
        return lastTokenRequestTime;
    }

    public void setLastTokenRequestTime(ZonedDateTime lastTokenRequestTime) {
        this.lastTokenRequestTime = lastTokenRequestTime;
    }

    public String getLastCDCCallTime() {
        return lastCDCCallTime;
    }

    public void setLastCDCCallTime(String lastCDCCallTime) {
        this.lastCDCCallTime = lastCDCCallTime;
    }
}
