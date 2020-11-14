package com.garyzhangscm.cwms.resources.model;

public class ApplicationInformation {
    String name = "CWMS";
    String description = "CWMS - Cloud based WMS";
    String version = "v0.1";

    private static ApplicationInformation applicationInformation = new ApplicationInformation();

    private ApplicationInformation(){}

    public static ApplicationInformation getApplicationInformation() {


        return applicationInformation;
    }
    public String getName() {
        return name;
    }


    public String getDescription() {
        return description;
    }


    public String getVersion() {
        return version;
    }

}
