/**
 * Copyright 2018
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

package com.garyzhangscm.cwms.integration.model.tiktok;


import com.garyzhangscm.cwms.integration.model.AuditibleEntity;
import org.codehaus.jackson.annotate.JsonProperty;

import javax.persistence.*;
import java.io.Serializable;

@Entity
@Table(name = "tiktok_seller_shop_integration_configuration")
public class TikTokSellerShopIntegrationConfiguration extends AuditibleEntity<String> implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "tiktok_seller_shop_integration_configuration_id")
    @JsonProperty(value="id")
    private Long id;

    @Column(name = "auth_code")
    private String authCode;


    @Column(name = "company_id")
    private Long companyId;
    @Column(name = "client_id")
    private Long clientId;


    @Column(name = "access_token")
    private String accessToken;
    @Column(name = "access_token_expire_in")
    private Long accessTokenExpireIn;
    @Column(name = "refresh_token")
    private String refreshToken;
    @Column(name = "refresh_token_expire_in")
    private Long refreshTokenExpireIn;
    @Column(name = "open_id")
    private String openId;
    @Column(name = "seller_name")
    private String sellerName;
    @Column(name = "seller_base_region")
    private String sellerBaseRegion;
    @Column(name = "user_type")
    private Integer userType;


    // auto refresh order every certain minutes
    // get the order in the past 2 * window minutes
    @Column(name = "auto_refresh_order_time_window_in_minute")
    private Integer autoRefreshOrderTimeWindowInMinute;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getAuthCode() {
        return authCode;
    }

    public void setAuthCode(String authCode) {
        this.authCode = authCode;
    }

    public Long getCompanyId() {
        return companyId;
    }

    public void setCompanyId(Long companyId) {
        this.companyId = companyId;
    }

    public Long getClientId() {
        return clientId;
    }

    public void setClientId(Long clientId) {
        this.clientId = clientId;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public Long getAccessTokenExpireIn() {
        return accessTokenExpireIn;
    }

    public void setAccessTokenExpireIn(Long accessTokenExpireIn) {
        this.accessTokenExpireIn = accessTokenExpireIn;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public Long getRefreshTokenExpireIn() {
        return refreshTokenExpireIn;
    }

    public void setRefreshTokenExpireIn(Long refreshTokenExpireIn) {
        this.refreshTokenExpireIn = refreshTokenExpireIn;
    }

    public String getOpenId() {
        return openId;
    }

    public void setOpenId(String openId) {
        this.openId = openId;
    }

    public String getSellerName() {
        return sellerName;
    }

    public void setSellerName(String sellerName) {
        this.sellerName = sellerName;
    }

    public String getSellerBaseRegion() {
        return sellerBaseRegion;
    }

    public void setSellerBaseRegion(String sellerBaseRegion) {
        this.sellerBaseRegion = sellerBaseRegion;
    }

    public Integer getUserType() {
        return userType;
    }

    public void setUserType(Integer userType) {
        this.userType = userType;
    }

    public Integer getAutoRefreshOrderTimeWindowInMinute() {
        return autoRefreshOrderTimeWindowInMinute;
    }

    public void setAutoRefreshOrderTimeWindowInMinute(Integer autoRefreshOrderTimeWindowInMinute) {
        this.autoRefreshOrderTimeWindowInMinute = autoRefreshOrderTimeWindowInMinute;
    }
}
