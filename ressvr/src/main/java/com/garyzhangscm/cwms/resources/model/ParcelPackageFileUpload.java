package com.garyzhangscm.cwms.resources.model;

public class ParcelPackageFileUpload extends FileUploadType {

    public ParcelPackageFileUpload(){
        super("parcel-packages", "Parcel Packages",
                "outbound/parcel/packages/upload",
                "resource/assets/file-templates/parcel-packages.csv",
                "outbound/parcel/packages/upload/progress",
                "outbound/parcel/packages/upload/result");
        setupColumns();
    }

    private void setupColumns() {

        addColumn(new FileUploadTemplateColumn(
                "client", "Client",
                String.class, 100, true
        ));

        addColumn(new FileUploadTemplateColumn(
                "order", "Order Number",
                String.class, 100, false
        ));
        addColumn(new FileUploadTemplateColumn(
                "trackingCode", "Tracking Number",
                String.class, 100, false
        ));

        addColumn(new FileUploadTemplateColumn(
                "length", "length",
                Double.class, 100, false
        ));
        addColumn(new FileUploadTemplateColumn(
                "width", "width",
                Double.class, 100, false
        ));
        addColumn(new FileUploadTemplateColumn(
                "height", "height",
                Double.class, 100, false
        ));
        addColumn(new FileUploadTemplateColumn(
                "weight", "weight",
                Double.class, 100, false
        ));
        addColumn(new FileUploadTemplateColumn(
                "carrier", "carrier",
                String.class, 100, false
        ));
        addColumn(new FileUploadTemplateColumn(
                "carrierServiceLevel", "Carrier Service Level",
                String.class, 100, true
        ));
        addColumn(new FileUploadTemplateColumn(
                "deliveryDays", "deliveryDays",
                Integer.class, 100, true
        ));
        addColumn(new FileUploadTemplateColumn(
                "rate", "rate",
                Double.class, 100, true
        ));
        addColumn(new FileUploadTemplateColumn(
                "insurance", "insurance",
                Double.class, 100, true
        ));
    }




}
