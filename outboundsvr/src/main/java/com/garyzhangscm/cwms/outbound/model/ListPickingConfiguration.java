package com.garyzhangscm.cwms.outbound.model;

import org.codehaus.jackson.annotate.JsonProperty;

import javax.persistence.*;

@Entity
@Table(name = "list_picking_configuration")
public class ListPickingConfiguration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "list_picking_configuration_id")
    @JsonProperty(value="id")
    private Long id;

    @Column(name = "sequence", unique = true)
    private Integer sequence;


    // criteria: warehouse / client / pick type

    @Column(name = "warehouse_id")
    private Long warehouseId;

    @Transient
    private Warehouse warehouse;

    @Column(name = "client_id")
    private Long clientId;

    @Transient
    private Client client;

    @Column(name = "pick_type")
    private PickType pickType;

    // Group Rule
    @Column(name = "group_rule")
    private ListPickingGroupRule groupRule;

    @Column(name = "enabled")
    private Boolean enabled;

    @Override
    public String toString() {
        return "ListPickingConfiguration{" +
                "id=" + id +
                ", sequence=" + sequence +
                ", warehouseId=" + warehouseId +
                ", warehouse=" + warehouse +
                ", clientId=" + clientId +
                ", client=" + client +
                ", pickType=" + pickType +
                ", groupRule=" + groupRule +
                ", enabled=" + enabled +
                '}';
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getSequence() {
        return sequence;
    }

    public void setSequence(Integer sequence) {
        this.sequence = sequence;
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

    public Long getClientId() {
        return clientId;
    }

    public void setClientId(Long clientId) {
        this.clientId = clientId;
    }

    public Client getClient() {
        return client;
    }

    public void setClient(Client client) {
        this.client = client;
    }

    public ListPickingGroupRule getGroupRule() {
        return groupRule;
    }

    public void setGroupRule(ListPickingGroupRule groupRule) {
        this.groupRule = groupRule;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public PickType getPickType() {
        return pickType;
    }

    public void setPickType(PickType pickType) {
        this.pickType = pickType;
    }

}
