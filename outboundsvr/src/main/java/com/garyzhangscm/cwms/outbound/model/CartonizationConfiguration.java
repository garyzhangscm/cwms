package com.garyzhangscm.cwms.outbound.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import javax.persistence.*;
import java.util.List;

@Entity
@Table(name = "cartonization_configuration")
public class CartonizationConfiguration  extends AuditibleEntity<String> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "cartonization_configuration_id")
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
    @Enumerated(EnumType.STRING)
    private PickType pickType;

    // Group Rule
    @ElementCollection(targetClass=CartonizationGroupRule.class)
    @Enumerated(EnumType.STRING)
    @CollectionTable(name="cartonization_configuration_group_rule",
            joinColumns = @JoinColumn(name = "cartonization_configuration_id"))
    @Column(name="group_rule")
    private List<CartonizationGroupRule> groupRules;

    @Column(name = "enabled")
    private Boolean enabled;

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

    public PickType getPickType() {
        return pickType;
    }

    public void setPickType(PickType pickType) {
        this.pickType = pickType;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public List<CartonizationGroupRule> getGroupRules() {
        return groupRules;
    }

    public void setGroupRules(List<CartonizationGroupRule> groupRules) {
        this.groupRules = groupRules;
    }
}
