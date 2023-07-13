package com.garyzhangscm.cwms.resources.model;

public class WalmartShippingCartonLabelFileUpload extends FileUploadType {

    public WalmartShippingCartonLabelFileUpload(){
        super("walmart-shipping-carton-labels", "Walmart Shipping Carton Labels",
                "outbound/walmart-shipping-carton-labels/upload",
                "resource/assets/file-templates/walmart-shipping-carton-label.csv",
                "outbound/walmart-shipping-carton-labels/upload/progress",
                "outbound/walmart-shipping-carton-labels/upload/result");
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
                "type", "Type",
                String.class, 20, false
        ));

        addColumn(new FileUploadTemplateColumn(
                "dept", "Dept",
                String.class, 100, false
        ));
        addColumn(new FileUploadTemplateColumn(
                "vendorId", "Vendor ID",
                String.class, 100, false
        ));
        addColumn(new FileUploadTemplateColumn(
                "shipTo", "Ship To",
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
                "DC", "DC",
                String.class, 100, true
        ));
        addColumn(new FileUploadTemplateColumn(
                "GLN", "GLN",
                String.class, 100, true
        ));

        addColumn(new FileUploadTemplateColumn(
                "itemNumber", "Item Number",
                String.class, 100, true
        ));
        addColumn(new FileUploadTemplateColumn(
                "WMIT", "WMIT",
                String.class, 100, true
        ));
        addColumn(new FileUploadTemplateColumn(
                "UPC", "UPC",
                String.class, 100, true
        ));
        addColumn(new FileUploadTemplateColumn(
                "UK", "UK",
                String.class, 100, true
        ));
        addColumn(new FileUploadTemplateColumn(
                "GTIN14", "GTIN14",
                String.class, 100, true
        ));
        addColumn(new FileUploadTemplateColumn(
                "GTIN14", "GTIN14",
                String.class, 100, true
        ));
        addColumn(new FileUploadTemplateColumn(
                "weight", "Weight",
                Double.class, 100, true
        ));
        addColumn(new FileUploadTemplateColumn(
                "description", "Description",
                String.class, 100, true
        ));
        addColumn(new FileUploadTemplateColumn(
                "orderQuantity", "Order Qty",
                Integer.class, 100, true
        ));
        addColumn(new FileUploadTemplateColumn(
                "UOM", "UOM",
                String.class, 100, true
        ));
        addColumn(new FileUploadTemplateColumn(
                "pieceCarton", "Pcs Carton",
                Integer.class, 100, true
        ));
        addColumn(new FileUploadTemplateColumn(
                "cartonQuantity", "Carton Qty",
                Integer.class, 100, true
        ));

        addColumn(new FileUploadTemplateColumn(
                "shipDate", "Ship Date",
                String.class, 100, true
        ));
        addColumn(new FileUploadTemplateColumn(
                "etaDate", "ETA Date",
                String.class, 100, true
        ));
        addColumn(new FileUploadTemplateColumn(
                "SCAC", "SCAC",
                String.class, 100, true
        ));
        addColumn(new FileUploadTemplateColumn(
                "transportationMethod", "Transportation Method - K(BackHaul) - M(Motor) - U (Parcel Service) - LT(Less than Trailer Load)",
                String.class, 100, true
        ));
        addColumn(new FileUploadTemplateColumn(
                "BOL", "BOL",
                String.class, 100, true
        ));
        addColumn(new FileUploadTemplateColumn(
                "trackingNumber", "Tracking Number",
                String.class, 100, true
        ));

        addColumn(new FileUploadTemplateColumn(
                "invoiceNumber", "Invoice Number",
                String.class, 100, true
        ));
        addColumn(new FileUploadTemplateColumn(
                "FOB", "FOB - CC(COLLECT) - PP(PREPAID)",
                String.class, 100, true
        ));
        addColumn(new FileUploadTemplateColumn(
                "loadId", "LoadID",
                String.class, 100, false
        ));
        addColumn(new FileUploadTemplateColumn(
                "appointmentNumber", "Appointment Number - Not required currently",
                String.class, 100, true
        ));
        addColumn(new FileUploadTemplateColumn(
                "carrierNumber", "Carrier Number/PRO",
                String.class, 100, true
        ));
        addColumn(new FileUploadTemplateColumn(
                "equipmentType", "EquipmentType- TL(Not Otherwise Specified)",
                String.class, 100, true
        ));
        addColumn(new FileUploadTemplateColumn(
                "equipmentInitial", "Equipment Initial",
                String.class, 100, true
        ));
        addColumn(new FileUploadTemplateColumn(
                "equipmentNumber", "Equpiment Number",
                String.class, 100, true
        ));
        addColumn(new FileUploadTemplateColumn(
                "sealNumber", "Seal Number(Required for TL)",
                String.class, 100, true
        ));
        addColumn(new FileUploadTemplateColumn(
                "fobMira", "FOB-MIRA",
                String.class, 100, true
        ));
        addColumn(new FileUploadTemplateColumn(
                "lomaColtonFayeteville", "LOMA-COLTON-FAYETEVILLE",
                String.class, 100, true
        ));
    }




}
