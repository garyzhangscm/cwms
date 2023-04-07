package com.garyzhangscm.cwms.resources.model;

public class ItemUnitOfMeasureFileUpload extends FileUploadType {

    public ItemUnitOfMeasureFileUpload(){
        super("itemUnitOfMeasure", "Item Unit of Measure",
                "inventory/item-unit-of-measures/upload",
                "resource/assets/file-templates/item-unit-of-measures.csv",
                "inventory/item-unit-of-measures/upload/progress",
                "inventory/item-unit-of-measures/upload/result");
        setupColumns();
    }

    private void setupColumns() {

        addColumn(new FileUploadTemplateColumn(
                "client", "Client name(3PL)",
                String.class, 100, true
        ));

        addColumn(new FileUploadTemplateColumn(
                "item", "Item Name",
                String.class, 100, false
        ));


        addColumn(new FileUploadTemplateColumn(
                "itemDescription", "Item Description(only needed for new item)",
                String.class, 100, true
        ));

        addColumn(new FileUploadTemplateColumn(
                "itemFamily", "Item Family Name(only needed for new item)",
                String.class, 100, true
        ));

        addColumn(new FileUploadTemplateColumn(
                "receivingRateByUnit", "Receiving Rate By Unit",
                Double.class, 100, true
        ));
        addColumn(new FileUploadTemplateColumn(
                "shippingRateByUnit", "Shipping Rate By Unit",
                Double.class, 100, true
        ));

        addColumn(new FileUploadTemplateColumn(
                "handlingRateByUnit", "Handling Fee By Unit",
                Double.class, 100, true
        ));



        addColumn(new FileUploadTemplateColumn(
                "trackingColorFlag", "Tracking Color?(only needed for new item)",
                String.class, 100, true
        ));

        addColumn(new FileUploadTemplateColumn(
                "defaultColor", "Default Color(only needed for new item)",
                String.class, 100, true
        ));

        addColumn(new FileUploadTemplateColumn(
                "trackingProductSizeFlag", "Tracking Product Size?(only needed for new item)",
                String.class, 100, true
        ));

        addColumn(new FileUploadTemplateColumn(
                "defaultProductSize", "Default Product Size(only needed for new item)",
                String.class, 100, true
        ));

        addColumn(new FileUploadTemplateColumn(
                "trackingStyleFlag", "Tracking Style?(only needed for new item)",
                String.class, 100, true
        ));

        addColumn(new FileUploadTemplateColumn(
                "defaultStyle", "Default Style(only needed for new item)",
                String.class, 100, true
        ));


        addColumn(new FileUploadTemplateColumn(
                 "itemPackageType", "Item Package Type Name",
                String.class, 100, false
        ));

        // only necessary if the item package type doesn't exist yet
        addColumn(new FileUploadTemplateColumn(
                "itemPackageTypeDescription", "Item Package Type Description(only needed for new item package type)",
                String.class, 100, true
        ));

        // only necessary if the item package type doesn't exist yet
        addColumn(new FileUploadTemplateColumn(
                "defaultItemPackageType", "Default Item Package Type",
                String.class, 100, true
        ));


        addColumn(new FileUploadTemplateColumn(
                "unitOfMeasure", "Unit of Measure code",
                String.class, 100, false
        ));

        addColumn(new FileUploadTemplateColumn(
                "quantity", "quantity",
                Long.class, 100, false
        ));
        addColumn(new FileUploadTemplateColumn(
                "weight", "weight",
                Double.class, 100, false
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
                "defaultForInboundReceiving", "default for inbound receiving",
                Boolean.class, 100, false
        ));
        addColumn(new FileUploadTemplateColumn(
                "defaultForWorkOrderReceiving", "default for work order receiving",
                Boolean.class, 100, false
        ));
        addColumn(new FileUploadTemplateColumn(
                "trackingLpn", "tracking LPN",
                Boolean.class, 100, false
        ));
        addColumn(new FileUploadTemplateColumn(
                "caseFlag", "case flag",
                Boolean.class, 100, false
        ));
        addColumn(new FileUploadTemplateColumn(
                "defaultForDisplay", "Default for Display",
                Boolean.class, 100, false
        ));



    }




}
