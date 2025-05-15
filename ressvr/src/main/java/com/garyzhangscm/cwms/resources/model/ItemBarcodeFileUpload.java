package com.garyzhangscm.cwms.resources.model;

public class ItemBarcodeFileUpload extends FileUploadType {

    public ItemBarcodeFileUpload(){
        super("item-barcodes", "Item Barcode",
                "inventory/item-barcodes/upload",
                "resource/assets/file-templates/item-barcodes.csv",
                "inventory/item-barcodes/upload/progress",
                "inventory/item-barcodes/upload/result");
        setupColumns();
    }

    private void setupColumns() {



        addColumn(new FileUploadTemplateColumn(
                "client", "Client name(3PL)",
                String.class, 100, true
        ));

        addColumn(new FileUploadTemplateColumn(
                 "name", "Item Name",
                String.class, 100, false
        ));

        addColumn(new FileUploadTemplateColumn(
                "code", "Barcode",
                String.class, 100, false
        ));


        addColumn(new FileUploadTemplateColumn(
                "type", "Item Barcode Type",
                String.class, 100, true
        ));
    }




}
