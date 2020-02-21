package com.garyzhangscm.cwms.resources.model;

public class ApplicationInformation {
    String name = "CWMS";
    String description = "CWMS - Cloud based WMS";

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
}
