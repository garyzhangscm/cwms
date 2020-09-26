package com.garyzhangscm.cwms.resources.model;

import java.util.ArrayList;
import java.util.List;

public class LocationFileUpload extends FileUploadType {

    public LocationFileUpload(){
        super("locations", "warehouse locations",
                "layout/locations/upload",
                "resource/assets/file-templates/locations.csv");
        setupColumns();
    }

    private void setupColumns() {
        addColumn(new FileUploadTemplateColumn(
                "warehouse", "Warehouse Name",
                String.class, 100, false
        ));


        addColumn(new FileUploadTemplateColumn(
                 "name", "Location Name",
                String.class, 100, false
        ));


        addColumn(new FileUploadTemplateColumn(
                "aisle", "aisle",
                String.class, 100, false
        ));


        addColumn(new FileUploadTemplateColumn(
                "x", "Coordinate X",
                Double.class, 100, false
        ));


        addColumn(new FileUploadTemplateColumn(
                "y", "Coordinate Y",
                Double.class, 100, false
        ));


        addColumn(new FileUploadTemplateColumn(
                "z", "Coordinate Z",
                Double.class, 100, false
        ));


        addColumn(new FileUploadTemplateColumn(
                "length", "Location Length",
                Double.class, 100, false
        ));


        addColumn(new FileUploadTemplateColumn(
                "width", "Location Width",
                Double.class, 100, false
        ));


        addColumn(new FileUploadTemplateColumn(
                "height", "Location Height",
                Double.class, 100, false
        ));


        addColumn(new FileUploadTemplateColumn(
                "pickSequence", "Pick Sequence",
                Integer.class, 100, false
        ));


        addColumn(new FileUploadTemplateColumn(
                "putawaySequence", "Putaway Sequence",
                Integer.class, 100, false
        ));


        addColumn(new FileUploadTemplateColumn(
                "countSequence", "Count Sequence",
                Integer.class, 100, false
        ));


        addColumn(new FileUploadTemplateColumn(
                "capacity", "Location Capacity",
                Double.class, 100, false
        ));


        addColumn(new FileUploadTemplateColumn(
                "fillPercentage", "Location Fill rate, 0 ~ 100",
                Integer.class, 100, false
        ));


        addColumn(new FileUploadTemplateColumn(
                "locationGroup", "Location Group Name",
                String.class, 100, false
        ));


        addColumn(new FileUploadTemplateColumn(
                "enabled",  "Location is enabled or not",
                Boolean.class, 100, false
        ));





    }




}
