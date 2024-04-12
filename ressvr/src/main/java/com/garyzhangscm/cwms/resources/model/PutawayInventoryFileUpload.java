package com.garyzhangscm.cwms.resources.model;

public class PutawayInventoryFileUpload extends FileUploadType {

    public PutawayInventoryFileUpload(){
        super("putaway-inventories", "Putaway Inventory",
                "inventory/inventories/putaway-inventory/upload",
                "resource/assets/file-templates/putaway-inventories.csv",
                "inventory/inventories/putaway-inventory/upload/progress",
                "inventory/inventories/putaway-inventory/upload/result");
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
                String.class, 100, false
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
                String.class, 100, false
        ));

        addColumn(new FileUploadTemplateColumn(
                "color", "Color",
                String.class, 100, false
        ));
        addColumn(new FileUploadTemplateColumn(
                "productSize", "Product Size",
                String.class, 100, false
        ));
        addColumn(new FileUploadTemplateColumn(
                "style", "Style",
                String.class, 100, false
        ));
        addColumn(new FileUploadTemplateColumn(
                "destinationLocation", "Destination Location",
                String.class, 100, false
        ));

        addColumn(new FileUploadTemplateColumn(
                "inventoryAttribute1", "Inventory Attribute 1",
                String.class, 200, true
        ));
        addColumn(new FileUploadTemplateColumn(
                "inventoryAttribute2", "Inventory Attribute 2",
                String.class, 200, true
        ));
        addColumn(new FileUploadTemplateColumn(
                "inventoryAttribute3", "Inventory Attribute 3",
                String.class, 200, true
        ));
        addColumn(new FileUploadTemplateColumn(
                "inventoryAttribute4", "Inventory Attribute 4",
                String.class, 200, true
        ));
        addColumn(new FileUploadTemplateColumn(
                "inventoryAttribute5", "Inventory  Attribute 5",
                String.class, 200, true
        ));

    }





}
