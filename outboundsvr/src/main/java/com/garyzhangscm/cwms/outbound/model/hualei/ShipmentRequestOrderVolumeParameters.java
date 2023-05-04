package com.garyzhangscm.cwms.outbound.model.hualei;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.garyzhangscm.cwms.outbound.model.AuditibleEntity;

import javax.persistence.*;
import java.io.Serializable;

@Entity
@Table(name = "hualei_shipment_request_order_volume_parameters")
public class ShipmentRequestOrderVolumeParameters  extends AuditibleEntity<String> implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "hualei_shipment_request_order_volume_parameters_id")
    @JsonProperty(value="id")
    private Long id;

    @Column(name = "warehouse_id")
    private Long warehouseId;


    @Column(name = "box_no")
    @JsonProperty("box_no")
    private String boxNo;

    @Column(name = "child_no")
    @JsonProperty("child_no")
    private String childNo;

    @Column(name = "volume_height")
    @JsonProperty("volume_height")
    private Double volumeHeight;

    @Column(name = "volume_length")
    @JsonProperty("volume_length")
    private Double volumeLength;

    @Column(name = "volume_width")
    @JsonProperty("volume_width")
    private Double volumeWidth;

    @Column(name = "length_unit")
    @JsonProperty("length_unit")
    private String lengthUnit;

    @Column(name = "volume_weight")
    @JsonProperty("volume_weight")
    private Double volumeWeight;

    @Column(name = "weight_unit")
    @JsonProperty("weight_unit")
    private String weightUnit;

    @ManyToOne
    @JoinColumn(name="hualei_shipment_request_parameters_id")
    @JsonIgnore
    private ShipmentRequestParameters shipmentRequestParameters;

    @Override
    public String toString() {
        try {
            return new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(this);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return null;
    }

    public ShipmentRequestParameters getShipmentRequestParameters() {
        return shipmentRequestParameters;
    }

    public void setShipmentRequestParameters(ShipmentRequestParameters shipmentRequestParameters) {
        this.shipmentRequestParameters = shipmentRequestParameters;
    }

    public String getBoxNo() {
        return boxNo;
    }

    public void setBoxNo(String boxNo) {
        this.boxNo = boxNo;
    }

    public String getChildNo() {
        return childNo;
    }

    public void setChildNo(String childNo) {
        this.childNo = childNo;
    }

    public Double getVolumeHeight() {
        return volumeHeight;
    }

    public void setVolumeHeight(Double volumeHeight) {
        this.volumeHeight = volumeHeight;
    }

    public Double getVolumeLength() {
        return volumeLength;
    }

    public void setVolumeLength(Double volumeLength) {
        this.volumeLength = volumeLength;
    }

    public Double getVolumeWidth() {
        return volumeWidth;
    }

    public void setVolumeWidth(Double volumeWidth) {
        this.volumeWidth = volumeWidth;
    }

    public Double getVolumeWeight() {
        return volumeWeight;
    }

    public void setVolumeWeight(Double volumeWeight) {
        this.volumeWeight = volumeWeight;
    }

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

    public String getLengthUnit() {
        return lengthUnit;
    }

    public void setLengthUnit(String lengthUnit) {
        this.lengthUnit = lengthUnit;
    }

    public String getWeightUnit() {
        return weightUnit;
    }

    public void setWeightUnit(String weightUnit) {
        this.weightUnit = weightUnit;
    }
}
