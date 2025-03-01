package com.garyzhangscm.cwms.workorder.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

/**
 *
 */
@Entity
@Table(name = "work_order_qc_rule_configuration")
public class WorkOrderQCRuleConfiguration extends AuditibleEntity<String>{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "work_order_qc_rule_configuration_id")
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


    @Column(name = "item_family_id")
    private Long itemFamilyId;
    @Transient
    private ItemFamily itemFamily;


    @Column(name = "item_id")
    private Long itemId;
    @Transient
    private Item item;

    @ManyToOne
    @JoinColumn(name="production_line_id")
    ProductionLine productionLine;

    @ManyToOne
    @JoinColumn(name="work_order_id")
    WorkOrder workOrder;


    // match the bto work order for certain customer
    // and/or order
    @Column(name = "outbound_order_id")
    private Long outboundOrderId;
    @Transient
    private Order outboundOrder;

    @Column(name = "customer_id")
    private Long customerId;
    @Transient
    private Customer customer;

    @OneToMany(
            mappedBy = "workOrderQCRuleConfiguration",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    List<WorkOrderQCRuleConfigurationRule> workOrderQCRuleConfigurationRules = new ArrayList<>();


    // how many actual product we will need to qc
    // used by 'qc by sample'
    @Column(name = "qc_quantity")
    private Long qcQuantity = 0l;


    // following fields will be used by 'QC by work order'
    @Column(name = "qc_quantity_per_work_order")
    private Long qcQuantityPerWorkOrder;

    @Column(name = "qc_percentage_per_work_order")
    private Double qcPercentagePerWorkOrder;

    @Column(name = "from_inventory_status_id")
    private Long fromInventoryStatusId;
    @Transient
    private InventoryStatus fromInventoryStatus;

    @Column(name = "to_inventory_status_id")
    private Long toInventoryStatusId;
    @Transient
    private InventoryStatus toInventoryStatus;

    // inventory lock that will be placed on the
    // inventory that's needs QC
    @Column(name = "inventory_lock_id")
    private Long inventoryLockId;
    @Transient
    private InventoryLock inventoryLock;

    // inventory lock that will be placed on the
    // future inventory that doesn't needs QC
    // but may need to be locked until the QC is completed
    @Column(name = "future_inventory_lock_id")
    private Long futureInventoryLockId;
    @Transient
    private InventoryLock futureInventoryLock;



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

    public ProductionLine getProductionLine() {
        return productionLine;
    }

    public void setProductionLine(ProductionLine productionLine) {
        this.productionLine = productionLine;
    }

    public WorkOrder getWorkOrder() {
        return workOrder;
    }

    public void setWorkOrder(WorkOrder workOrder) {
        this.workOrder = workOrder;
    }

    public List<WorkOrderQCRuleConfigurationRule> getWorkOrderQCRuleConfigurationRules() {
        return workOrderQCRuleConfigurationRules;
    }

    public void setWorkOrderQCRuleConfigurationRules(List<WorkOrderQCRuleConfigurationRule> workOrderQCRuleConfigurationRules) {
        this.workOrderQCRuleConfigurationRules = workOrderQCRuleConfigurationRules;
    }

    public Long getQcQuantity() {
        return qcQuantity;
    }

    public void setQcQuantity(Long qcQuantity) {
        this.qcQuantity = qcQuantity;
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

    public Long getOutboundOrderId() {
        return outboundOrderId;
    }

    public void setOutboundOrderId(Long outboundOrderId) {
        this.outboundOrderId = outboundOrderId;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
    }

    public Long getQcQuantityPerWorkOrder() {
        return qcQuantityPerWorkOrder;
    }

    public void setQcQuantityPerWorkOrder(Long qcQuantityPerWorkOrder) {
        this.qcQuantityPerWorkOrder = qcQuantityPerWorkOrder;
    }

    public Double getQcPercentagePerWorkOrder() {
        return qcPercentagePerWorkOrder;
    }

    public void setQcPercentagePerWorkOrder(Double qcPercentagePerWorkOrder) {
        this.qcPercentagePerWorkOrder = qcPercentagePerWorkOrder;
    }

    public Long getFromInventoryStatusId() {
        return fromInventoryStatusId;
    }

    public void setFromInventoryStatusId(Long fromInventoryStatusId) {
        this.fromInventoryStatusId = fromInventoryStatusId;
    }

    public InventoryStatus getFromInventoryStatus() {
        return fromInventoryStatus;
    }

    public void setFromInventoryStatus(InventoryStatus fromInventoryStatus) {
        this.fromInventoryStatus = fromInventoryStatus;
    }

    public Long getToInventoryStatusId() {
        return toInventoryStatusId;
    }

    public void setToInventoryStatusId(Long toInventoryStatusId) {
        this.toInventoryStatusId = toInventoryStatusId;
    }

    public InventoryStatus getToInventoryStatus() {
        return toInventoryStatus;
    }

    public void setToInventoryStatus(InventoryStatus toInventoryStatus) {
        this.toInventoryStatus = toInventoryStatus;
    }

    public Long getInventoryLockId() {
        return inventoryLockId;
    }

    public void setInventoryLockId(Long inventoryLockId) {
        this.inventoryLockId = inventoryLockId;
    }

    public InventoryLock getInventoryLock() {
        return inventoryLock;
    }

    public void setInventoryLock(InventoryLock inventoryLock) {
        this.inventoryLock = inventoryLock;
    }

    public Long getFutureInventoryLockId() {
        return futureInventoryLockId;
    }

    public void setFutureInventoryLockId(Long futureInventoryLockId) {
        this.futureInventoryLockId = futureInventoryLockId;
    }

    public InventoryLock getFutureInventoryLock() {
        return futureInventoryLock;
    }

    public void setFutureInventoryLock(InventoryLock futureInventoryLock) {
        this.futureInventoryLock = futureInventoryLock;
    }

    public Order getOutboundOrder() {
        return outboundOrder;
    }

    public void setOutboundOrder(Order outboundOrder) {
        this.outboundOrder = outboundOrder;
    }

    public Customer getCustomer() {
        return customer;
    }

    public void setCustomer(Customer customer) {
        this.customer = customer;
    }
}
