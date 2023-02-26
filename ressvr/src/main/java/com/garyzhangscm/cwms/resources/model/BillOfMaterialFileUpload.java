package com.garyzhangscm.cwms.resources.model;

public class BillOfMaterialFileUpload extends FileUploadType {

    public BillOfMaterialFileUpload(){
        super("BOMs", "BOM",
                "workorder/bill-of-materials/upload",
                "resource/assets/file-templates/boms.csv",
                "",
                "");
        setupColumns();
    }

    private void setupColumns() {


        addColumn(new FileUploadTemplateColumn(
                "billOfMaterial", "BOM Number",
                String.class, 100, false
        ));
        addColumn(new FileUploadTemplateColumn(
                "bomItem", "BOM Item Number",
                String.class, 100, true
        ));
        addColumn(new FileUploadTemplateColumn(
                "bomExpectedQuantity", "BOM Expected Quantity",
                Double.class, 100, true
        ));
        addColumn(new FileUploadTemplateColumn(
                "number", "BOM Line Number",
                String.class, 100, false
        ));

        addColumn(new FileUploadTemplateColumn(
                "item", "BOM Line Item Number",
                String.class, 100, false
        ));
        addColumn(new FileUploadTemplateColumn(
                "inventoryStatus", "BOM Line Item Inventory Status",
                String.class, 100, false
        ));
        addColumn(new FileUploadTemplateColumn(
                "expectedQuantity", "BOM Line Item Expected Quantity",
                String.class, 100, false
        ));
    }




}
