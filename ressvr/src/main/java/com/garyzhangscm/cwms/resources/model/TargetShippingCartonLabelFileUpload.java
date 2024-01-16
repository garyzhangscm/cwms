package com.garyzhangscm.cwms.resources.model;

public class TargetShippingCartonLabelFileUpload extends FileUploadType {

    public TargetShippingCartonLabelFileUpload(){
        super("target-shipping-carton-labels", "Target Shipping Carton Labels",
                "outbound/target-shipping-carton-labels/upload",
                "resource/assets/file-templates/target-shipping-carton-label.csv",
                "outbound/target-shipping-carton-labels/upload/progress",
                "outbound/target-shipping-carton-labels/upload/result");
        setupColumns();
    }

    private void setupColumns() {

        addColumn(new FileUploadTemplateColumn(
                "SSCC18", "SSCC18",
                String.class, 100, false
        ));

        addColumn(new FileUploadTemplateColumn(
                "partnerID", "PartnerID",
                String.class, 100, false
        ));
        addColumn(new FileUploadTemplateColumn(
                "docType", "DocType",
                String.class, 100, false
        ));
        addColumn(new FileUploadTemplateColumn(
                "poNumber", "PO Number",
                String.class, 100, false
        ));
        addColumn(new FileUploadTemplateColumn(
                "poDate", "PO Date",
                Long.class, 100, false
        ));

        addColumn(new FileUploadTemplateColumn(
                "shipToName", "Ship To Name",
                String.class, 100, true
        ));

        addColumn(new FileUploadTemplateColumn(
                "address1", "Address 1",
                String.class, 100, true
        ));
        addColumn(new FileUploadTemplateColumn(
                "cityStateZip", "City State Zip",
                String.class, 100, true
        ));
        addColumn(new FileUploadTemplateColumn(
                "zip420", "ZIP(420)",
                String.class, 50, true
        ));

        addColumn(new FileUploadTemplateColumn(
                "lineItemNumber", "line item number",
                String.class, 50, true
        ));
        addColumn(new FileUploadTemplateColumn(
                "UOM", "UOM",
                String.class, 100, true
        ));
        addColumn(new FileUploadTemplateColumn(
                "pieceCarton", "Pcs per Carton",
                Integer.class, 100, true
        ));
        addColumn(new FileUploadTemplateColumn(
                "itemNumber", "Item Number",
                String.class, 100, true
        ));
        addColumn(new FileUploadTemplateColumn(
                "customerSKU", "Customer SKU",
                String.class, 100, true
        ));
        addColumn(new FileUploadTemplateColumn(
                "UPC", "UPC",
                String.class, 100, true
        ));
        addColumn(new FileUploadTemplateColumn(
                "weight", "Weight",
                Double.class, 100, true
        ));
        addColumn(new FileUploadTemplateColumn(
                "shippedQuantity", "Shipped Qty",
                Integer.class, 100, true
        ));
        addColumn(new FileUploadTemplateColumn(
                "orderQuantity", "Order Qty",
                Integer.class, 100, true
        ));
        addColumn(new FileUploadTemplateColumn(
                "shipDate", "Ship Date",
                String.class, 100, true
        ));
        addColumn(new FileUploadTemplateColumn(
                "BOL", "BOL",
                String.class, 100, true
        ));
        addColumn(new FileUploadTemplateColumn(
                "SCAC", "SCAC",
                String.class, 100, true
        ));
        addColumn(new FileUploadTemplateColumn(
                "freightType", "Freight Type",
                String.class, 100, true
        ));
        addColumn(new FileUploadTemplateColumn(
                "shipId", "Ship ID",
                String.class, 100, true
        ));
        addColumn(new FileUploadTemplateColumn(
                "dpciDashed", "DPCI Dashed",
                String.class, 100, true
        ));
        addColumn(new FileUploadTemplateColumn(
                "GTIN", "GTIN",
                String.class, 100, true
        ));
    }




}
