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


import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.garyzhangscm.cwms.integration.model.AuditibleEntity;

import java.io.Serializable;
import java.util.List;

public class TikTokProduct extends AuditibleEntity<String> implements Serializable {

    @JsonProperty(value="create_time")
    private Long createTime;
    @JsonProperty(value="description")
    private String description;
    @JsonProperty(value="external_product_id")
    private String externalProductId;
    @JsonProperty(value="id")
    private String id;
    @JsonProperty(value="is_cod_allowed")
    private Boolean isCodAllowed;
    @JsonProperty(value="package_dimensions")
    private TikTokDimension packageDimensions;


    @JsonProperty(value="package_weight")
    private TikTokWeight packageWeight;
    @JsonProperty(value="skus")
    private List<TikTokSKU> skus;
    @JsonProperty(value="status")
    private String status;

    @JsonProperty(value="title")
    private String title;
    @JsonProperty(value="update_time")
    private Long updateTime;


    @Override
    public String toString() {
        try {
            return new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(this);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return null;
    }

    public Long getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Long createTime) {
        this.createTime = createTime;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getExternalProductId() {
        return externalProductId;
    }

    public void setExternalProductId(String externalProductId) {
        this.externalProductId = externalProductId;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Boolean getCodAllowed() {
        return isCodAllowed;
    }

    public void setCodAllowed(Boolean codAllowed) {
        isCodAllowed = codAllowed;
    }

    public TikTokDimension getPackageDimensions() {
        return packageDimensions;
    }

    public void setPackageDimensions(TikTokDimension packageDimensions) {
        this.packageDimensions = packageDimensions;
    }

    public TikTokWeight getPackageWeight() {
        return packageWeight;
    }

    public void setPackageWeight(TikTokWeight packageWeight) {
        this.packageWeight = packageWeight;
    }

    public List<TikTokSKU> getSkus() {
        return skus;
    }

    public void setSkus(List<TikTokSKU> skus) {
        this.skus = skus;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Long getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Long updateTime) {
        this.updateTime = updateTime;
    }
}
