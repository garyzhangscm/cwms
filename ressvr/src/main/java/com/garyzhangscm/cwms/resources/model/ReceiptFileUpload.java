package com.garyzhangscm.cwms.resources.model;

public class ReceiptFileUpload extends FileUploadType {

    public ReceiptFileUpload(){
        super("receipts", "Receipt",
                "inbound/receipts/upload",
                "resource/assets/file-templates/receiptsclient.csv");
        setupColumns();
    }

    private void setupColumns() {


        addColumn(new FileUploadTemplateColumn(
                 "client", "Client name(3PL)",
                String.class, 100, false
        ));

        addColumn(new FileUploadTemplateColumn(
                "supplier", "Supplier",
                String.class, 100, true
        ));


        addColumn(new FileUploadTemplateColumn(
                "receipt", "Receipt Number",
                String.class, 100, true
        ));

        addColumn(new FileUploadTemplateColumn(
                "line", "Receipt Line Number",
                String.class, 100, true
        ));

        addColumn(new FileUploadTemplateColumn(
                "item", "Item Number",
                String.class, 100, true
        ));

        addColumn(new FileUploadTemplateColumn(
                "expectedQuantity", "Quantity",
                String.class, 100, true
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
    }




}
