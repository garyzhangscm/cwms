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

package com.garyzhangscm.cwms.inbound.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.codehaus.jackson.annotate.JsonProperty;

import javax.persistence.*;
import java.util.Objects;


@Entity
@Table(name = "inbound_qc_configuration")
public class InboundQCConfiguration extends AuditibleEntity<String>{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "inbound_qc_configuration_id")
    @JsonProperty(value="id")
    private Long id;

    // criteria
    // 1. supplier
    // 2. item
    // 3. warehouse
    // 4. company
    // from most specific to most generic
    @Column(name = "supplier_id")
    private Long supplierId;
    @Transient
    private Supplier supplier;

    @Column(name = "item_id")
    private Long itemId;
    @Transient
    private Item item;


    @Column(name = "warehouse_id")
    private Long warehouseId;
    @Transient
    private Warehouse warehouse;


    @Column(name = "company_id")
    private Long companyId;
    @Transient
    private Company company;


    @Column(name = "qc_quantity_per_receipt")
    private Long qcQuantityPerReceipt;

    @Column(name = "qc_percentage")
    private Double qcPercentage;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        InboundQCConfiguration that = (InboundQCConfiguration) o;
        return Objects.equals(id, that.id) ||
                (Objects.equals(supplierId, that.supplierId)
                        && Objects.equals(itemId, that.itemId)
                        && Objects.equals(warehouseId, that.warehouseId)
                        && Objects.equals(companyId, that.companyId));
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, supplierId, itemId, warehouseId, companyId);
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

    public Long getSupplierId() {
        return supplierId;
    }

    public void setSupplierId(Long supplierId) {
        this.supplierId = supplierId;
    }

    public Long getItemId() {
        return itemId;
    }

    public void setItemId(Long itemId) {
        this.itemId = itemId;
    }

    public Long getWarehouseId() {
        return warehouseId;
    }

    public void setWarehouseId(Long warehouseId) {
        this.warehouseId = warehouseId;
    }

    public Long getCompanyId() {
        return companyId;
    }

    public void setCompanyId(Long companyId) {
        this.companyId = companyId;
    }

    public Long getQcQuantityPerReceipt() {
        return qcQuantityPerReceipt;
    }

    public void setQcQuantityPerReceipt(Long qcQuantityPerReceipt) {
        this.qcQuantityPerReceipt = qcQuantityPerReceipt;
    }

    public Double getQcPercentage() {
        return qcPercentage;
    }

    public void setQcPercentage(Double qcPercentage) {
        this.qcPercentage = qcPercentage;
    }

    public Supplier getSupplier() {
        return supplier;
    }

    public void setSupplier(Supplier supplier) {
        this.supplier = supplier;
    }

    public Item getItem() {
        return item;
    }

    public void setItem(Item item) {
        this.item = item;
    }

    public Warehouse getWarehouse() {
        return warehouse;
    }

    public void setWarehouse(Warehouse warehouse) {
        this.warehouse = warehouse;
    }

    public Company getCompany() {
        return company;
    }

    public void setCompany(Company company) {
        this.company = company;
    }
}
