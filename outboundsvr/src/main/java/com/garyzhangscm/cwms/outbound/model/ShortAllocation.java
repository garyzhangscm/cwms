package com.garyzhangscm.cwms.outbound.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.codehaus.jackson.annotate.JsonProperty;

import javax.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "short_allocation")
public class ShortAllocation   extends AuditibleEntity<String> implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "short_allocation_id")
    @JsonProperty(value="id")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "shipment_line_id")
    @JsonIgnore
    private ShipmentLine shipmentLine;

    @Transient
    private String orderNumber;

    @Transient
    private String workOrderNumber;

    @Column(name = "warehouse_id")
    private Long warehouseId;

    @Transient
    private Warehouse warehouse;


    // re-allocate the short allocation based upon
    // the last allocation time, to make sure we
    // will only re-try after certain amount time
    // has been passed.
    @Column(name = "last_allocation_datetime")
    private LocalDateTime lastAllocationDatetime;

    @OneToMany(
            mappedBy = "shortAllocation",
            orphanRemoval = true,
            cascade = CascadeType.REMOVE
    )
    private List<Pick> picks;

    @Column(name = "item_id")
    private Long itemId;

    @Transient
    private Item item;


    @Column(name = "quantity")
    private Long quantity;

    @Column(name = "open_quantity")
    private Long openQuantity;

    @Column(name = "inprocess_quantity")
    private Long inprocessQuantity;

    @Column(name = "delivered_quantity")
    private Long deliveredQuantity;

    // How many times we have tried to
    // allocate this short allocation to
    // get an emergency replenishment
    @Column(name = "allocation_count")
    private Long allocationCount;


    @Column(name = "work_order_quantity")
    private Long workOrderQuantity;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private ShortAllocationStatus status;


    @Column(name = "work_order_line_id")
    private Long workOrderLineId;


    @Column(name="color")
    private String color;

    @Column(name="product_size")
    private String productSize;

    @Column(name="style")
    private String style;

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

    public ShipmentLine getShipmentLine() {
        return shipmentLine;
    }

    public void setShipmentLine(ShipmentLine shipmentLine) {
        this.shipmentLine = shipmentLine;
    }

    public Long getQuantity() {
        return quantity;
    }

    public void setQuantity(Long quantity) {
        this.quantity = quantity;
    }

    public String getOrderNumber() {
        return shipmentLine == null ? "" : shipmentLine.getOrderNumber();
    }

    public ShortAllocationStatus getStatus() {
        return status;
    }

    public void setStatus(ShortAllocationStatus status) {
        this.status = status;
    }

    public List<Pick> getPicks() {
        return picks;
    }

    public void setPicks(List<Pick> picks) {
        this.picks = picks;
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

    public Long getWorkOrderLineId() {
        return workOrderLineId;
    }

    public void setWorkOrderLineId(Long workOrderLineId) {
        this.workOrderLineId = workOrderLineId;
    }

    public Long getOpenQuantity() {
        return openQuantity;
    }

    public void setOpenQuantity(Long openQuantity) {
        this.openQuantity = openQuantity;
    }

    public Long getInprocessQuantity() {
        return inprocessQuantity;
    }

    public void setInprocessQuantity(Long inprocessQuantity) {
        this.inprocessQuantity = inprocessQuantity;
    }

    public Long getDeliveredQuantity() {
        return deliveredQuantity;
    }

    public void setDeliveredQuantity(Long deliveredQuantity) {
        this.deliveredQuantity = deliveredQuantity;
    }

    public Long getAllocationCount() {
        return allocationCount;
    }

    public void setAllocationCount(Long allocationCount) {
        this.allocationCount = allocationCount;
    }

    public LocalDateTime getLastAllocationDatetime() {
        return lastAllocationDatetime;
    }

    public void setLastAllocationDatetime(LocalDateTime lastAllocationDatetime) {
        this.lastAllocationDatetime = lastAllocationDatetime;
    }

    public void setOrderNumber(String orderNumber) {
        this.orderNumber = orderNumber;
    }

    public String getWorkOrderNumber() {
        return workOrderNumber;
    }

    public void setWorkOrderNumber(String workOrderNumber) {
        this.workOrderNumber = workOrderNumber;
    }

    public Long getWorkOrderQuantity() {
        return workOrderQuantity;
    }

    public void setWorkOrderQuantity(Long workOrderQuantity) {
        this.workOrderQuantity = workOrderQuantity;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public String getProductSize() {
        return productSize;
    }

    public void setProductSize(String productSize) {
        this.productSize = productSize;
    }

    public String getStyle() {
        return style;
    }

    public void setStyle(String style) {
        this.style = style;
    }
}
