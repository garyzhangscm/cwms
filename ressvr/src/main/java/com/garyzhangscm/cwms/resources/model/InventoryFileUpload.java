package com.garyzhangscm.cwms.resources.model;


import org.exolab.castor.types.DateTime;

import java.time.ZonedDateTime;

public class InventoryFileUpload extends FileUploadType {

    public InventoryFileUpload(){
        super("inventory", "Inventory",
                "inventory/inventories/upload",
                "resource/assets/file-templates/inventories.csv",
                "inventory/inventories/upload/progress",
                "inventory/inventories/upload/result");
        setupColumns();
    }

    private void setupColumns() {

        addColumn(new FileUploadTemplateColumn(
                "client", "Client",
                String.class, 100, true
        ));


        addColumn(new FileUploadTemplateColumn(
                 "lpn", "LPN",
                String.class, 100, true
        ));

        addColumn(new FileUploadTemplateColumn(
                "location", "location",
                String.class, 100, false
        ));


        addColumn(new FileUploadTemplateColumn(
                "item", "Item Name",
                String.class, 100, false
        ));
        addColumn(new FileUploadTemplateColumn(
                "itemPackageType", "Item Package Type",
                String.class, 100, true
        ));


        addColumn(new FileUploadTemplateColumn(
                "quantity", "Quantity",
                Long.class, 100, false
        ));
        addColumn(new FileUploadTemplateColumn(
                "unitOfMeasure", "Unit Of Measure",
                String.class, 10, true
        ));
        addColumn(new FileUploadTemplateColumn(
                "inventoryStatus", "Inventory Status",
                String.class, 100, true
        ));
        addColumn(new FileUploadTemplateColumn(
                "fifoDate", "FIFO Date(yyyy-MM-dd)",
                ZonedDateTime.class, 100, true
        ));


        addColumn(new FileUploadTemplateColumn(
                "color", "Color",
                String.class, 100, true
        ));
        addColumn(new FileUploadTemplateColumn(
                "productSize", "Product Size",
                String.class, 100, true
        ));
        addColumn(new FileUploadTemplateColumn(
                "style", "Style",
                String.class, 100, true
        ));

        addColumn(new FileUploadTemplateColumn(
                "attribute1", "Attribute 1",
                String.class, 200, true
        ));
        addColumn(new FileUploadTemplateColumn(
                "attribute2", "Attribute 2",
                String.class, 200, true
        ));
        addColumn(new FileUploadTemplateColumn(
                "attribute3", "Attribute 3",
                String.class, 200, true
        ));
        addColumn(new FileUploadTemplateColumn(
                "attribute4", "Attribute 4",
                String.class, 200, true
        ));
        addColumn(new FileUploadTemplateColumn(
                "attribute5", "Attribute 5",
                String.class, 200, true
        ));


    }




}
