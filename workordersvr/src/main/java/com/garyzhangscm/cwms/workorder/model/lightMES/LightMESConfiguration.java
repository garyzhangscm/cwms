package com.garyzhangscm.cwms.workorder.model.lightMES;

import com.garyzhangscm.cwms.workorder.model.AuditibleEntity;
import com.garyzhangscm.cwms.workorder.model.Warehouse;
import org.codehaus.jackson.annotate.JsonProperty;

import javax.persistence.*;

@Entity
@Table(name = "light_mes_configuration")
public class LightMESConfiguration extends AuditibleEntity<String> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "light_mes_configuration_id")
    @JsonProperty(value="id")
    private Long id;


    @Column(name = "warehouse_id")
    private Long warehouseId;

    @Transient
    private Warehouse warehouse;

    @Column(name = "protocol")
    private String protocol;

    @Column(name = "host")
    private String host;

    @Column(name = "port")
    private String port;

    @Column(name = "access_key_id")
    private String accessKeyId;
    @Column(name = "access_key_secret")
    private String accessKeySecret;


    @Column(name = "single_light_status_query_url")
    private String singleLightStatusQueryUrl;

    @Column(name = "batch_light_status_query_url")
    private String batchLightStatusQueryUrl;

    @Column(name = "single_light_pulse_query_url")
    private String singleLightPulseQueryUrl;


    @Column(name = "single_machine_detail_query_url")
    private String singleMachineDetailQueryUrl;

    @Column(name = "machine_list_query_url")
    private String machineListQueryUrl;

    @Column(name = "time_zone")
    private String timeZone = "UTC";

    // how many minutes to look back for the pulse to calculate
    // the cycle time
    @Column(name = "cycle_time_window")
    private Integer cycleTimeWindow;


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

    public Warehouse getWarehouse() {
        return warehouse;
    }

    public void setWarehouse(Warehouse warehouse) {
        this.warehouse = warehouse;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getPort() {
        return port;
    }

    public void setPort(String port) {
        this.port = port;
    }

    public String getSingleLightStatusQueryUrl() {
        return singleLightStatusQueryUrl;
    }

    public void setSingleLightStatusQueryUrl(String singleLightStatusQueryUrl) {
        this.singleLightStatusQueryUrl = singleLightStatusQueryUrl;
    }

    public String getBatchLightStatusQueryUrl() {
        return batchLightStatusQueryUrl;
    }

    public void setBatchLightStatusQueryUrl(String batchLightStatusQueryUrl) {
        this.batchLightStatusQueryUrl = batchLightStatusQueryUrl;
    }

    public String getSingleLightPulseQueryUrl() {
        return singleLightPulseQueryUrl;
    }

    public void setSingleLightPulseQueryUrl(String singleLightPulseQueryUrl) {
        this.singleLightPulseQueryUrl = singleLightPulseQueryUrl;
    }

    public String getSingleMachineDetailQueryUrl() {
        return singleMachineDetailQueryUrl;
    }

    public void setSingleMachineDetailQueryUrl(String singleMachineDetailQueryUrl) {
        this.singleMachineDetailQueryUrl = singleMachineDetailQueryUrl;
    }

    public String getMachineListQueryUrl() {
        return machineListQueryUrl;
    }

    public void setMachineListQueryUrl(String machineListQueryUrl) {
        this.machineListQueryUrl = machineListQueryUrl;
    }

    public String getAccessKeyId() {
        return accessKeyId;
    }

    public void setAccessKeyId(String accessKeyId) {
        this.accessKeyId = accessKeyId;
    }

    public String getAccessKeySecret() {
        return accessKeySecret;
    }

    public void setAccessKeySecret(String accessKeySecret) {
        this.accessKeySecret = accessKeySecret;
    }

    public String getTimeZone() {
        return timeZone;
    }

    public void setTimeZone(String timeZone) {
        this.timeZone = timeZone;
    }

    public Integer getCycleTimeWindow() {
        return cycleTimeWindow;
    }

    public void setCycleTimeWindow(Integer cycleTimeWindow) {
        this.cycleTimeWindow = cycleTimeWindow;
    }
}
