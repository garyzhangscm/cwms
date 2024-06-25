package com.garyzhangscm.cwms.resources.model;

import java.time.ZonedDateTime;

public class ReceivingInventoryFileUpload extends FileUploadType {

    public ReceivingInventoryFileUpload(){
        super("receiving-inventories", "Receiving Inventory",
                "inbound/receipts/receiving-inventory/upload",
                "resource/assets/file-templates/receiving-inventories.csv",
                "inbound/receipts/receiving-inventory/upload/progress",
                "inbound/receipts/receiving-inventory/upload/result");
        setupColumns();
    }

    private void setupColumns() {

        addColumn(new FileUploadTemplateColumn(
                "client", "client",
                String.class, 100, true
        ));

        addColumn(new FileUploadTemplateColumn(
                "receipt", "Receipt Number",
                String.class, 100, false
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
                "attribute1", "Inventory Attribute 1",
                String.class, 200, true
        ));
        addColumn(new FileUploadTemplateColumn(
                "attribute2", "Inventory Attribute 2",
                String.class, 200, true
        ));
        addColumn(new FileUploadTemplateColumn(
                "attribute3", "Inventory Attribute 3",
                String.class, 200, true
        ));
        addColumn(new FileUploadTemplateColumn(
                "attribute4", "Inventory Attribute 4",
                String.class, 200, true
        ));
        addColumn(new FileUploadTemplateColumn(
                "attribute5", "Inventory  Attribute 5",
                String.class, 200, true
        ));
        addColumn(new FileUploadTemplateColumn(
                "inWarehouseDatetime", "In Warehouse Date Time",
                ZonedDateTime.class, 100, true
        ));

    }





}
