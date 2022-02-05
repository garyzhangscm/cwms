package com.garyzhangscm.cwms.outbound.model;

import org.codehaus.jackson.annotate.JsonProperty;

import javax.persistence.*;
import java.io.Serializable;

/**
 *
 */
@Entity
@Table(name = "carton")
public class Carton  extends AuditibleEntity<String> implements Serializable {

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        @Column(name = "carton_id")
        @JsonProperty(value="id")
        private Long id;

        @Column(name = "name")
        private String name;



    @Column(name = "warehouse_id")
    private Long warehouseId;

    @Transient
    private Warehouse warehouse;

    @Column(name = "length")
    private Double length;

    @Column(name = "width")
    private Double width;
    @Column(name = "height")
    private Double height;
    /**
     * Percentage of fill rate
     */
    @Column(name = "fill_rate")
    private Double fillRate;

    @Column(name = "enabled")
    private Boolean enabled;

    @Column(name = "shipping_carton_flag")
    private Boolean shippingCartonFlag;

    @Column(name = "picking_carton_flag")
    private Boolean pickingCartonFlag;

    public Double getTotalSpace() {
        return length * width * height * fillRate / 100;
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

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Double getLength() {
        return length;
    }

    public void setLength(Double length) {
        this.length = length;
    }

    public Double getWidth() {
        return width;
    }

    public void setWidth(Double width) {
        this.width = width;
    }

    public Double getHeight() {
        return height;
    }

    public void setHeight(Double height) {
        this.height = height;
    }

    public Double getFillRate() {
        return fillRate;
    }

    public void setFillRate(Double fillRate) {
        this.fillRate = fillRate;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public Boolean getShippingCartonFlag() {
        return shippingCartonFlag;
    }

    public void setShippingCartonFlag(Boolean shippingCartonFlag) {
        this.shippingCartonFlag = shippingCartonFlag;
    }

    public Boolean getPickingCartonFlag() {
        return pickingCartonFlag;
    }

    public void setPickingCartonFlag(Boolean pickingCartonFlag) {
        this.pickingCartonFlag = pickingCartonFlag;
    }
}
