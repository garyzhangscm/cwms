package com.garyzhangscm.cwms.resources.model;

public class TargetShippingCartonLabelFileUpload extends FileUploadType {

    public TargetShippingCartonLabelFileUpload(){
        super("target-shipping-carton-labels", "Target Shipping Carton Labels",
                "outbound/target-shipping-carton-labels/upload",
                "resource/assets/file-templates/target-shipping-carton-label.csv",
                "outbound/target-shipping-carton-labels/upload/progress",
                "outbound/target-shipping-carton-labels/upload/result");
        setupColumns();
        setupColumnsMap();
    }

    private void setupColumnsMap() {
        // column head from the CSV file(from column name)
        // PartnerID,Doc Type,PO,PO DATE, SHIP TO NAME, ADD 1, CITY STATE ZIP, ZIP (420), LINE ITEM #, UOM, PC_CTN, ITEM#, CUSTOMER SKU, UPC, WEIGHT, QTY SHIPPED,QTY ORDERED,SSCC-18,SHIP DATE, BOL, SCAC,FREIGHT TYPE(M = MOTOR A = AIR C = CONSOLIDATOR),Ship ID,DCPI DASHED,GTIN
        // column head needed(to column name)
        // partnerID,docType,poNumber,poDate,shipToName,address1,cityStateZip,zip420,lineItemNumber,UOM,pieceCarton,itemNumber,customerSKU,UPC,weight,shippedQuantity,orderQuantity,SSCC18,shipDate,BOL,SCAC,freightType,shipId,dpciDashed,GTIN

        addColumnMap("PartnerID","partnerID");
        addColumnMap("Doc Type","docType");
        addColumnMap("PO","poNumber");
        addColumnMap("PO DATE","poDate");
        addColumnMap("SHIP TO NAME","shipToName");
        addColumnMap("ADD 1","address1");
        addColumnMap("CITY STATE ZIP","cityStateZip");
        addColumnMap("ZIP (420)","zip420");
        addColumnMap("LINE ITEM #","lineItemNumber");
        addColumnMap("UOM","UOM");
        addColumnMap("PC_CTN","pieceCarton");
        addColumnMap("ITEM#","itemNumber");
        addColumnMap("CUSTOMER SKU","customerSKU");
        addColumnMap("UPC","UPC");
        addColumnMap("WEIGHT","weight");
        addColumnMap("QTY SHIPPED","shippedQuantity");
        addColumnMap("QTY ORDERED","orderQuantity");
        addColumnMap("SSCC-18","SSCC18");
        addColumnMap("SHIP DATE","shipDate");
        addColumnMap("BOL","BOL");
        addColumnMap("SCAC","SCAC");
        addColumnMap("FREIGHT TYPE(M = MOTOR A = AIR C = CONSOLIDATOR)","freightType");
        addColumnMap("Ship ID","shipId");
        addColumnMap("DCPI DASHED","dpciDashed");
        addColumnMap("GTIN","GTIN");
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
