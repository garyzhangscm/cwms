package com.garyzhangscm.cwms.outbound.model;

import com.garyzhangscm.cwms.outbound.service.PickListService;
import org.codehaus.jackson.annotate.JsonProperty;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "pick_list")
public class PickList {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "pick_list_id")
    @JsonProperty(value="id")
    private Long id;

    @OneToMany(
            mappedBy = "pickList"
    )
    private List<Pick> picks = new ArrayList<>();

    @Column(name = "group_key")
    private String groupKey;

    @Column(name = "warehouse_id")
    private Long warehouseId;

    @Transient
    private Warehouse warehouse;


    @Column(name = "status")
    private PickListStatus status;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public List<Pick> getPicks() {
        return picks;
    }

    public void setPicks(List<Pick> picks) {
        this.picks = picks;
    }

    public String getGroupKey() {
        return groupKey;
    }

    public void setGroupKey(String groupKey) {
        this.groupKey = groupKey;
    }

    public PickListStatus getStatus() {
        return status;
    }

    public void setStatus(PickListStatus status) {
        this.status = status;
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
}
