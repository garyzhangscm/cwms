package com.garyzhangscm.cwms.workorder.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.codehaus.jackson.annotate.JsonProperty;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "work_order_line_spare_part")
public class WorkOrderLineSparePart extends AuditibleEntity<String>{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "work_order_line_spare_part_id")
    @JsonProperty(value="id")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "work_order_line_id")
    @JsonIgnore
    private WorkOrderLine workOrderLine;

    @Column(name = "name")
    private String name;
    @Column(name = "description")
    private String description;

    @Column(name="quantity")
    private Long quantity;

    @OneToMany(
            mappedBy = "workOrderLineSparePart",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    private List<WorkOrderLineSparePartDetail> workOrderLineSparePartDetails= new ArrayList<>();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public WorkOrderLine getWorkOrderLine() {
        return workOrderLine;
    }

    public void setWorkOrderLine(WorkOrderLine workOrderLine) {
        this.workOrderLine = workOrderLine;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Long getQuantity() {
        return quantity;
    }

    public void setQuantity(Long quantity) {
        this.quantity = quantity;
    }

    public List<WorkOrderLineSparePartDetail> getWorkOrderLineSparePartDetails() {
        return workOrderLineSparePartDetails;
    }

    public void setWorkOrderLineSparePartDetails(List<WorkOrderLineSparePartDetail> workOrderLineSparePartDetails) {
        this.workOrderLineSparePartDetails = workOrderLineSparePartDetails;
    }
}
