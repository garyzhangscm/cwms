package com.garyzhangscm.cwms.resources.model;

public class ItemUnitOfMeasureFileUpload extends FileUploadType {

    public ItemUnitOfMeasureFileUpload(){
        super("itemUnitOfMeasure", "item unit of measure upload",
                "inventory/item-unit-of-measures/upload",
                "resource/assets/file-templates/item-unit-of-measures.csv");
        setupColumns();
    }

    private void setupColumns() {

        addColumn(new FileUploadTemplateColumn(
                "company", "Company Code",
                String.class, 100, false
        ));


        addColumn(new FileUploadTemplateColumn(
                "warehouse", "Warehouse Name",
                String.class, 100, true
        ));

        addColumn(new FileUploadTemplateColumn(
                "client", "Client name(3PL)",
                String.class, 100, true
        ));

        addColumn(new FileUploadTemplateColumn(
                "item", "Item Name",
                String.class, 100, false
        ));

        addColumn(new FileUploadTemplateColumn(
                 "itemPackageType", "Item Package Type Name",
                String.class, 100, false
        ));

        // only necessary if the item package type doesn't exist yet
        addColumn(new FileUploadTemplateColumn(
                "itemPackageTypeDescription", "Item Package Type Description",
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



    }




}