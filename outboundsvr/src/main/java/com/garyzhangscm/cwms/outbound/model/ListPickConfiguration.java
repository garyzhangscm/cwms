package com.garyzhangscm.cwms.outbound.model;

import org.codehaus.jackson.annotate.JsonProperty;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "list_pick_configuration")
public class ListPickConfiguration extends AuditibleEntity<String> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "list_pick_configuration_id")
    @JsonProperty(value="id")
    private Long id;

    @Column(name = "sequence")
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


    @Column(name = "customer_id")
    private Long customerId;

    @Transient
    private Customer customer;

    @Column(name = "pick_type")
    @Enumerated(EnumType.STRING)
    private PickType pickType;


    @Column(name = "enabled")
    private Boolean enabled;

    @Column(name = "allow_add_to_existing_list")
    private Boolean allowAddToExistingList;

    @OneToMany(
            mappedBy = "listPickConfiguration",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.EAGER
    )
    private List<ListPickConfigurationGroupRule> groupRules = new ArrayList<>();


    @Column(name = "max_volume")
    private Double maxVolume = 0.0;
    @Column(name = "max_volume_unit")
    private String maxVolumeUnit;

    @Column(name = "max_weight")
    private Double maxWeight = 0.0;
    @Column(name = "max_weight_unit")
    private String maxWeightUnit;


    @Column(name = "max_pick_count")
    private Integer maxPickCount = 0;

    @Column(name = "max_quantity")
    private Long maxQuantity = 0l;

    @Column(name = "allow_lpn_pick")
    private Boolean allowLPNPick;

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


    public Long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
    }

    public Customer getCustomer() {
        return customer;
    }

    public void setCustomer(Customer customer) {
        this.customer = customer;
    }

    public List<ListPickConfigurationGroupRule> getGroupRules() {
        return groupRules;
    }

    public void setGroupRules(List<ListPickConfigurationGroupRule> groupRules) {
        this.groupRules = groupRules;
    }

    public Boolean getAllowLPNPick() {
        return allowLPNPick;
    }

    public void setAllowLPNPick(Boolean allowLPNPick) {
        this.allowLPNPick = allowLPNPick;
    }

    public Double getMaxVolume() {
        return maxVolume;
    }

    public void setMaxVolume(Double maxVolume) {
        this.maxVolume = maxVolume;
    }

    public Double getMaxWeight() {
        return maxWeight;
    }

    public void setMaxWeight(Double maxWeight) {
        this.maxWeight = maxWeight;
    }

    public Boolean getAllowAddToExistingList() {
        return allowAddToExistingList;
    }

    public void setAllowAddToExistingList(Boolean allowAddToExistingList) {
        this.allowAddToExistingList = allowAddToExistingList;
    }

    public Integer getMaxPickCount() {
        return maxPickCount;
    }

    public void setMaxPickCount(Integer maxPickCount) {
        this.maxPickCount = maxPickCount;
    }

    public Long getMaxQuantity() {
        return maxQuantity;
    }

    public void setMaxQuantity(Long maxQuantity) {
        this.maxQuantity = maxQuantity;
    }

    public String getMaxVolumeUnit() {
        return maxVolumeUnit;
    }

    public void setMaxVolumeUnit(String maxVolumeUnit) {
        this.maxVolumeUnit = maxVolumeUnit;
    }

    public String getMaxWeightUnit() {
        return maxWeightUnit;
    }

    public void setMaxWeightUnit(String maxWeightUnit) {
        this.maxWeightUnit = maxWeightUnit;
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
}
