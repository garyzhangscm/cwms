package com.garyzhangscm.cwms.adminserver.model.wms;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.codehaus.jackson.annotate.JsonProperty;

import javax.persistence.*;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Cartonization implements Serializable {

        private Long id;

    private String number;

    private String groupKeyValue;

    private Long warehouseId;

    private Warehouse warehouse;

    private List<Pick> picks = new ArrayList<>();


    private CartonizationStatus status = CartonizationStatus.OPEN;

    private Carton carton;

    private PickList pickList;


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
