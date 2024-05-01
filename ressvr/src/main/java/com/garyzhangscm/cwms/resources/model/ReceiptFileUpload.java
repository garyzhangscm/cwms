package com.garyzhangscm.cwms.resources.model;

public class ReceiptFileUpload extends FileUploadType {

    public ReceiptFileUpload(){
        super("receipts", "Receipt",
                "inbound/receipts/upload",
                "resource/assets/file-templates/receipts.csv",
                "inbound/receipts/upload/progress",
                "inbound/receipts/upload/result");
        setupColumns();
    }

    private void setupColumns() {


        addColumn(new FileUploadTemplateColumn(
                 "client", "Client name(3PL)",
                String.class, 100, true
        ));

        addColumn(new FileUploadTemplateColumn(
                "supplier", "Supplier",
                String.class, 100, true
        ));


        addColumn(new FileUploadTemplateColumn(
                "receipt", "Receipt Number",
                String.class, 100, false
        ));

        addColumn(new FileUploadTemplateColumn(
                "line", "Receipt Line Number",
                String.class, 100, false
        ));

        addColumn(new FileUploadTemplateColumn(
                "item", "Item Number",
                String.class, 100, false
        ));

        addColumn(new FileUploadTemplateColumn(
                "expectedQuantity", "Quantity",
                String.class, 100, false
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
                "overReceivingQuantity", "Over Receiving Allowed By Quantity",
                String.class, 100, true
        ));

        addColumn(new FileUploadTemplateColumn(
                "overReceivingPercent", "Over Receiving Allowed By Percentage",
                String.class, 100, true
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
                "inventoryAttribute5", "Inventory Attribute 5",
                String.class, 200, true
        ));

    }




}
