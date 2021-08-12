package com.garyzhangscm.cwms.workorder.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;

public class ProductionLineAllocationRequest {

    Long workOrderId;
    Long productionLineId;
    String productionLineName;
    Long totalQuantity; // total assigned quantity, read from production line assignment
    Long openQuantity;  // total open quantity, read from production line assignment
    Long allocatingQuantity; // quantity to be allocated
    Boolean allocateByLine; // whether we allocate by work order line or by whole work order
    List<ProductionLineAllocationRequestLine> lines = new ArrayList<>();


    @Override
    public String toString() {
        try {
            return new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(this);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return null;
    }

    public Long getWorkOrderId() {
        return workOrderId;
    }

    public void setWorkOrderId(Long workOrderId) {
        this.workOrderId = workOrderId;
    }

    public Long getProductionLineId() {
        return productionLineId;
    }

    public void setProductionLineId(Long productionLineId) {
        this.productionLineId = productionLineId;
    }

    public String getProductionLineName() {
        return productionLineName;
    }

    public void setProductionLineName(String productionLineName) {
        this.productionLineName = productionLineName;
    }

    public Long getTotalQuantity() {
        return totalQuantity;
    }

    public void setTotalQuantity(Long totalQuantity) {
        this.totalQuantity = totalQuantity;
    }

    public Long getOpenQuantity() {
        return openQuantity;
    }

    public void setOpenQuantity(Long openQuantity) {
        this.openQuantity = openQuantity;
    }

    public Long getAllocatingQuantity() {
        return allocatingQuantity;
    }

    public void setAllocatingQuantity(Long allocatingQuantity) {
        this.allocatingQuantity = allocatingQuantity;
    }

    public Boolean getAllocateByLine() {
        return allocateByLine;
    }

    public void setAllocateByLine(Boolean allocateByLine) {
        this.allocateByLine = allocateByLine;
    }

    public List<ProductionLineAllocationRequestLine> getLines() {
        return lines;
    }

    public void setLines(List<ProductionLineAllocationRequestLine> lines) {
        this.lines = lines;
    }
}
