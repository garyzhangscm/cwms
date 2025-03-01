package com.garyzhangscm.cwms.workorder.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import com.fasterxml.jackson.annotation.JsonProperty;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 *
 */
@Entity
@Table(name = "material_requirements_planning")
public class MaterialRequirementsPlanning extends AuditibleEntity<String>{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "material_requirements_planning_id")
    @JsonProperty(value="id")
    private Long id;

    @Column(name = "warehouse_id")
    private Long warehouseId;

    @Column(name = "number")
    private String number;

    @Column(name = "description")
    private String description;

    @Column(name = "item_id")
    private Long itemId;

    @Transient
    private Item item;

    // overall required quantity, calculated from the MPS
    @Column(name = "total_required_quantity")
    private Long totalRequiredQuantity;

    // setup if the MRP is on certain production lines
    @ManyToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @JoinTable(name = "mrp_production_line",
            joinColumns = @JoinColumn(name = "material_requirements_planning_id"),
            inverseJoinColumns = @JoinColumn(name = "production_line_id"))
    private List<ProductionLine> productionLines = new ArrayList<>();

    // MPS line on the production line
    @ManyToOne
    @JoinColumn(name = "master_production_schedule_id")
    private MasterProductionSchedule masterProductionSchedule;


    @Column(name = "cutoff_date")
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private LocalDateTime cutoffDate;

    @OneToMany(
            mappedBy = "materialRequirementsPlanning",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    List<MaterialRequirementsPlanningLine> materialRequirementsPlanningLines = new ArrayList<>();


    public Long getItemId() {
        return itemId;
    }

    public void setItemId(Long itemId) {
        this.itemId = itemId;
    }

    public Item getItem() {
        return item;
    }

    public void setItem(Item item) {
        this.item = item;
    }

    public Long getTotalRequiredQuantity() {
        return totalRequiredQuantity;
    }

    public void setTotalRequiredQuantity(Long totalRequiredQuantity) {
        this.totalRequiredQuantity = totalRequiredQuantity;
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

    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<ProductionLine> getProductionLines() {
        return productionLines;
    }

    public void setProductionLines(List<ProductionLine> productionLines) {
        this.productionLines = productionLines;
    }
    public void addProductionLine(ProductionLine productionLine) {
        this.productionLines.add(productionLine);
    }

    public MasterProductionSchedule getMasterProductionSchedule() {
        return masterProductionSchedule;
    }

    public void setMasterProductionSchedule(MasterProductionSchedule masterProductionSchedule) {
        this.masterProductionSchedule = masterProductionSchedule;
    }

    public LocalDateTime getCutoffDate() {
        return cutoffDate;
    }

    public void setCutoffDate(LocalDateTime cutoffDate) {
        this.cutoffDate = cutoffDate;
    }

    public List<MaterialRequirementsPlanningLine> getMaterialRequirementsPlanningLines() {
        return materialRequirementsPlanningLines;
    }

    public void setMaterialRequirementsPlanningLines(List<MaterialRequirementsPlanningLine> materialRequirementsPlanningLines) {
        this.materialRequirementsPlanningLines = materialRequirementsPlanningLines;
    }
}
