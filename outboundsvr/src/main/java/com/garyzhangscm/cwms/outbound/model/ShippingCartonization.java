package com.garyzhangscm.cwms.outbound.model;


import org.codehaus.jackson.annotate.JsonProperty;

import javax.persistence.*;
import java.io.Serializable;


/**
 *
 */
@Entity
@Table(name = "shipping_cartonization")
public class ShippingCartonization implements Serializable {

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        @Column(name = "shipping_cartonization_id")
        @JsonProperty(value="id")
        private Long id;

    @Column(name = "number")
    private String number;

    @Column(name = "warehouse_id")
    private Long warehouseId;

    @Transient
    private Warehouse warehouse;

    /**
     * master data of the carton
     */
    @ManyToOne
    @JoinColumn(name = "carton_id")
    private Carton carton;

    public ShippingCartonization(){}

    public ShippingCartonization(String number, Long warehouseId, Carton carton) {
        this.number = number;
        this.warehouseId = warehouseId;
        this.carton = carton;
    }

    public ShippingCartonization(String number, Long warehouseId) {
        this.number = number;
        this.warehouseId = warehouseId;
    }

    @Override
    public String toString() {
        return "ShippingCartonization{" +
                "id=" + id +
                ", number='" + number + '\'' +
                ", warehouseId=" + warehouseId +
                ", warehouse=" + warehouse +
                ", carton=" + carton +
                '}';
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
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

    public Carton getCarton() {
        return carton;
    }

    public void setCarton(Carton carton) {
        this.carton = carton;
    }
}
