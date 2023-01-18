package com.garyzhangscm.cwms.workorder.model;


public class ProductionLineAttribute {

    private ProductionLine productionLine;


    private String name;

    private String value;

    public ProductionLineAttribute(){}

    public ProductionLineAttribute(ProductionLine productionLine, String name, String value) {
        this.productionLine = productionLine;
        this.name = name;
        this.value = value;
    }

    public ProductionLine getProductionLine() {
        return productionLine;
    }

    public void setProductionLine(ProductionLine productionLine) {
        this.productionLine = productionLine;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
