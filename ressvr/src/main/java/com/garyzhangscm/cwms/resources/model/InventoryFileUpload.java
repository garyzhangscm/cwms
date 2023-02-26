package com.garyzhangscm.cwms.resources.model;
 

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
                String.class, 100, false
        ));


        addColumn(new FileUploadTemplateColumn(
                 "lpn", "LPN",
                String.class, 100, false
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


    }




}
