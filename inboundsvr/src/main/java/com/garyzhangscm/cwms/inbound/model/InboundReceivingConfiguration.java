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

import org.codehaus.jackson.annotate.JsonProperty;

import javax.persistence.*;
import java.util.Objects;


@Entity
@Table(name = "inbound_receiving_configuration")
public class InboundReceivingConfiguration extends AuditibleEntity<String>{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "inbound_receiving_configuration_id")
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


    @Column(name = "item_family_id")
    private Long itemFamilyId;
    @Transient
    private ItemFamily itemFamily;


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


    // configuration
    // standard pallet size, used to calculate
    // how many pallet will be in each receipt
    @Column(name = "standard_pallet_size")
    private Double standardPalletSize;

    // use size over quantity to estimate the
    @Column(name = "estimate_pallet_count_by_size")
    private Boolean estimatePalletCountBySize;

    // in case the estimatePalletCountBySize is set to true
    // use receipt line's cubic meter(rcvlin.cubicMeter)
    //  to estimate how many pallet will be on the receipt.
    //  If the value is not available , or the value is set to false
    // use the item's package setup (case uom) to get the total
    // value and then use the standard pallet size value to get
    // an estimation of how many pallet will be in the receipt line
    @Column(name = "estimate_pallet_count_by_receipt_line_cubic_meter")
    private Boolean estimatePalletCountByReceiptLineCubicMeter;


    // before which status do we allow the user to change the receipt
    // if it is empty, then we are not allow the user to change the
    // receipt once it is created
    // we have different configuration for
    // upload files
    // integration
    // manually change from web UI
    @Column(name = "status_allow_receipt_change_when_upload_file")
    @Enumerated(EnumType.STRING)
    private ReceiptStatus statusAllowReceiptChangeWhenUploadFile;
    @Column(name = "status_allow_receipt_change_from_integration")
    @Enumerated(EnumType.STRING)
    private ReceiptStatus statusAllowReceiptChangeFromIntegration;
    @Column(name = "status_allow_receipt_change_from_web_ui")
    @Enumerated(EnumType.STRING)
    private ReceiptStatus statusAllowReceiptChangeFromWebUI;

    @Column(name = "use_receipt_check_in_time_as_in_warehouse_datetime")
    private Boolean useReceiptCheckInTimeAsInWarehouseDateTime;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        InboundReceivingConfiguration that = (InboundReceivingConfiguration) o;
        return Objects.equals(id, that.id) ||
                (Objects.equals(supplierId, that.supplierId)
                        && Objects.equals(itemId, that.itemId)
                        && Objects.equals(itemFamilyId, that.itemFamilyId)
                        && Objects.equals(warehouseId, that.warehouseId)
                        && Objects.equals(companyId, that.companyId));
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, supplierId, itemId,  warehouseId, companyId);
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

    public Supplier getSupplier() {
        return supplier;
    }

    public void setSupplier(Supplier supplier) {
        this.supplier = supplier;
    }

    public Long getItemFamilyId() {
        return itemFamilyId;
    }

    public void setItemFamilyId(Long itemFamilyId) {
        this.itemFamilyId = itemFamilyId;
    }

    public ItemFamily getItemFamily() {
        return itemFamily;
    }

    public void setItemFamily(ItemFamily itemFamily) {
        this.itemFamily = itemFamily;
    }

    public Long getItemId() {
        return itemId;
    }

    public void setItemId(Long itemId) {
        this.itemId = itemId;
    }

    public Item getItem() {
        return item;
    }

    public void setItem(Item item) {
        this.item = item;
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

    public Double getStandardPalletSize() {
        return standardPalletSize;
    }

    public void setStandardPalletSize(Double standardPalletSize) {
        this.standardPalletSize = standardPalletSize;
    }

    public Boolean getEstimatePalletCountBySize() {
        return estimatePalletCountBySize;
    }

    public void setEstimatePalletCountBySize(Boolean estimatePalletCountBySize) {
        this.estimatePalletCountBySize = estimatePalletCountBySize;
    }

    public Boolean getEstimatePalletCountByReceiptLineCubicMeter() {
        return estimatePalletCountByReceiptLineCubicMeter;
    }

    public void setEstimatePalletCountByReceiptLineCubicMeter(Boolean estimatePalletCountByReceiptLineCubicMeter) {
        this.estimatePalletCountByReceiptLineCubicMeter = estimatePalletCountByReceiptLineCubicMeter;
    }

    public ReceiptStatus getStatusAllowReceiptChangeWhenUploadFile() {
        return statusAllowReceiptChangeWhenUploadFile;
    }

    public void setStatusAllowReceiptChangeWhenUploadFile(ReceiptStatus statusAllowReceiptChangeWhenUploadFile) {
        this.statusAllowReceiptChangeWhenUploadFile = statusAllowReceiptChangeWhenUploadFile;
    }

    public Boolean getUseReceiptCheckInTimeAsInWarehouseDateTime() {
        return useReceiptCheckInTimeAsInWarehouseDateTime;
    }

    public void setUseReceiptCheckInTimeAsInWarehouseDateTime(Boolean useReceiptCheckInTimeAsInWarehouseDateTime) {
        this.useReceiptCheckInTimeAsInWarehouseDateTime = useReceiptCheckInTimeAsInWarehouseDateTime;
    }

    public ReceiptStatus getStatusAllowReceiptChangeFromIntegration() {
        return statusAllowReceiptChangeFromIntegration;
    }

    public void setStatusAllowReceiptChangeFromIntegration(ReceiptStatus statusAllowReceiptChangeFromIntegration) {
        this.statusAllowReceiptChangeFromIntegration = statusAllowReceiptChangeFromIntegration;
    }

    public ReceiptStatus getStatusAllowReceiptChangeFromWebUI() {
        return statusAllowReceiptChangeFromWebUI;
    }

    public void setStatusAllowReceiptChangeFromWebUI(ReceiptStatus statusAllowReceiptChangeFromWebUI) {
        this.statusAllowReceiptChangeFromWebUI = statusAllowReceiptChangeFromWebUI;
    }
}
