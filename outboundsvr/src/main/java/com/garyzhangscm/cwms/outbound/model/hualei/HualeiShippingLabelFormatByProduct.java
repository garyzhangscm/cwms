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

package com.garyzhangscm.cwms.outbound.model.hualei;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.garyzhangscm.cwms.outbound.model.AuditibleEntity;
import com.garyzhangscm.cwms.outbound.model.Order;
import org.codehaus.jackson.annotate.JsonProperty;

import javax.persistence.*;
import java.io.Serializable;

@Entity
@Table(name = "hualei_shipping_label_format_by_product")
public class HualeiShippingLabelFormatByProduct extends AuditibleEntity<String> implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "hualei_shipping_label_format_by_product_id")
    @JsonProperty(value="id")
    private Long id;

    @Column(name = "warehouse_id")
    private Long warehouseId;


    @Column(name = "product_id")
    private String productId;

    @Column(name = "shipping_label_format")
    @Enumerated(EnumType.STRING)
    private ShippingLabelFormat shippingLabelFormat;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "hualei_configuration_id")
    private HualeiConfiguration hualeiConfiguration;

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

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public ShippingLabelFormat getShippingLabelFormat() {
        return shippingLabelFormat;
    }

    public void setShippingLabelFormat(ShippingLabelFormat shippingLabelFormat) {
        this.shippingLabelFormat = shippingLabelFormat;
    }
}
