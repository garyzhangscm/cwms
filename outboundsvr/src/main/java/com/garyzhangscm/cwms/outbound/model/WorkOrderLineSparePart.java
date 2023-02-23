package com.garyzhangscm.cwms.outbound.model;

import java.util.ArrayList;
import java.util.List;

public class WorkOrderLineSparePart extends AuditibleEntity<String>{
    private Long id;


    private String name;
    private String description;

    private Long quantity;

    private Long inprocessQuantity;

    private List<WorkOrderLineSparePartDetail> workOrderLineSparePartDetails= new ArrayList<>();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public Long getInprocessQuantity() {
        return inprocessQuantity;
    }

    public void setInprocessQuantity(Long inprocessQuantity) {
        this.inprocessQuantity = inprocessQuantity;
    }
}
