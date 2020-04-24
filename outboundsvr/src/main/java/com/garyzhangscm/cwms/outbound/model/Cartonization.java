package com.garyzhangscm.cwms.outbound.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.codehaus.jackson.annotate.JsonProperty;

import javax.persistence.*;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 *
 */
@Entity
@Table(name = "cartonization")
public class Cartonization implements Serializable {

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        @Column(name = "cartonization_id")
        @JsonProperty(value="id")
        private Long id;

    @Column(name = "number")
    private String number;

    @Column(name = "group_key_value")
    private String groupKeyValue;


    @Column(name = "warehouse_id")
    private Long warehouseId;

    @Transient
    private Warehouse warehouse;

    /**
     * All the picks being cartonized into one carton
     */
    @OneToMany(
            mappedBy = "cartonization",
            orphanRemoval = true,
            fetch = FetchType.LAZY)
    private List<Pick> picks = new ArrayList<>();


    @Column(name = "carton_status")
    @Enumerated(EnumType.STRING)
    private CartonizationStatus status = CartonizationStatus.OPEN;

    /**
     * master data of the carton
     */
    @ManyToOne
    @JoinColumn(name = "carton_id")
    private Carton carton;

    @ManyToOne
    @JoinColumn(name = "pick_list_id")
    @JsonIgnore
    private PickList pickList;

    public Double getTotalSpace() {
        return carton.getTotalSpace();
    }
    public Double getUsedSpace() {
        return picks.stream().map(Pick::getSize).mapToDouble(Double::doubleValue).sum();

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

    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    public List<Pick> getPicks() {
        return picks;
    }

    public void setPicks(List<Pick> picks) {
        this.picks = picks;
    }

    public Carton getCarton() {
        return carton;
    }

    public void setCarton(Carton carton) {
        this.carton = carton;
    }

    public String getGroupKeyValue() {
        return groupKeyValue;
    }

    public void setGroupKeyValue(String groupKeyValue) {
        this.groupKeyValue = groupKeyValue;
    }

    public CartonizationStatus getStatus() {
        return status;
    }

    public void setStatus(CartonizationStatus status) {
        this.status = status;
    }

    public PickList getPickList() {
        return pickList;
    }

    public void setPickList(PickList pickList) {
        this.pickList = pickList;
    }
}
