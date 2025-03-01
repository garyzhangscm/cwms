package com.garyzhangscm.cwms.workorder.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import javax.persistence.*;

@Entity
@Table(name = "silo_device_api_call_history")
public class SiloDeviceAPICallHistory extends AuditibleEntity<String>{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "silo_device_api_call_history_id")
    @JsonProperty(value="id")
    private Long id;

    @Column(name = "warehouse_id")
    private Long warehouseId;

    @Column(name = "web_api_call_timestamp")
    private Long webAPICallTimeStamp;


    @Column(name = "location_name")
    private String locationName;

    @Column(name = "name")
    private String name;

    @Column(name = "device_id")
    private Integer deviceId;

    @Column(name = "material")
    private String material;

    @Column(name = "item_name")
    private String itemName;

    @Column(name = "distance")
    private Double distance;

    @Column(name = "timestamp")
    private Long timeStamp;

    @Column(name = "status_code")
    private String statusCode;

    @Column(name = "token")
    private String token;

    public SiloDeviceAPICallHistory(){}
    public SiloDeviceAPICallHistory(Long warehouseId, Long webAPICallTimeStamp) {
        this(warehouseId, webAPICallTimeStamp, "",
                "", null, "", null, null, "", "");
    }
    public SiloDeviceAPICallHistory(Long warehouseId, Long webAPICallTimeStamp, String locationName,
                                    String name, Integer deviceId, String material,
                                    Double distance, Long timeStamp, String statusCode,
                                    String token) {
        this.warehouseId = warehouseId;
        this.webAPICallTimeStamp = webAPICallTimeStamp;
        this.locationName = locationName;
        this.name = name;
        this.deviceId = deviceId;
        this.material = material;
        this.distance = distance;
        this.timeStamp = timeStamp;
        this.statusCode = statusCode;
        this.token = token;
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

    public String getLocationName() {
        return locationName;
    }

    public void setLocationName(String locationName) {
        this.locationName = locationName;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(Integer deviceId) {
        this.deviceId = deviceId;
    }

    public String getMaterial() {
        return material;
    }

    public void setMaterial(String material) {
        this.material = material;
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public Double getDistance() {
        return distance;
    }

    public void setDistance(Double distance) {
        this.distance = distance;
    }

    public Long getTimeStamp() {
        return timeStamp;
    }

    public void setTimeStamp(Long timeStamp) {
        this.timeStamp = timeStamp;
    }

    public String getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(String statusCode) {
        this.statusCode = statusCode;
    }

    public Long getWebAPICallTimeStamp() {
        return webAPICallTimeStamp;
    }

    public void setWebAPICallTimeStamp(Long webAPICallTimeStamp) {
        this.webAPICallTimeStamp = webAPICallTimeStamp;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }
}
