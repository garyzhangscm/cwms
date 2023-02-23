package com.garyzhangscm.cwms.resources.model;

public class ItemFileUpload extends FileUploadType {

    public ItemFileUpload(){
        super("items", "Item",
                "inventory/items/upload",
                "resource/assets/file-templates/items.csv");
        setupColumns();
    }

    private void setupColumns() {



        addColumn(new FileUploadTemplateColumn(
                 "name", "Item Name",
                String.class, 100, false
        ));

        addColumn(new FileUploadTemplateColumn(
                "description", "Item Description",
                String.class, 100, true
        ));


        addColumn(new FileUploadTemplateColumn(
                "client", "Client name(3PL)",
                String.class, 100, true
        ));


        addColumn(new FileUploadTemplateColumn(
                "itemFamily", "Item Family name",
                String.class, 100, true
        ));
        addColumn(new FileUploadTemplateColumn(
                "unitCost", "Unit Cost",
                Integer.class, 100, true
        ));

        addColumn(new FileUploadTemplateColumn(
                "allowCartonization", "Allow Cartonization flag",
                Boolean.class, 100, false
        ));
        addColumn(new FileUploadTemplateColumn(
                "allowAllocationByLPN", "Allow Allocation By LPN flag",
                Boolean.class, 100, false
        ));
        addColumn(new FileUploadTemplateColumn(
                "allocationRoundUpStrategyType", "Allocation Round Up Strategy",
                String.class, 100, false
        ));
        addColumn(new FileUploadTemplateColumn(
                "allocationRoundUpStrategyValue", "Allocation Round Up Strategy Value",
                String.class, 100, true
        ));
        addColumn(new FileUploadTemplateColumn(
                "trackingVolumeFlag", "Tracking Volume flag",
                Boolean.class, 100, false
        ));
        addColumn(new FileUploadTemplateColumn(
                "trackingLotNumberFlag", "Tracking Lot Number flag",
                Boolean.class, 100, false
        ));
        addColumn(new FileUploadTemplateColumn(
                "trackingManufactureDateFlag", "Tracking Manufacture Date flag",
                Boolean.class, 100, false
        ));
        addColumn(new FileUploadTemplateColumn(
                "shelfLifeDays", "Shelf Life Days",
                Integer.class, 100, true
        ));
        addColumn(new FileUploadTemplateColumn(
                "trackingExpirationDateFlag", "Tracking Expiration Date flag",
                Integer.class, 100, false
        ));
        addColumn(new FileUploadTemplateColumn(
                "imageUrl", "Image URL",
                String.class, 100, true
        ));
        addColumn(new FileUploadTemplateColumn(
                "thumbnailUrl", "Thumbnail URL",
                String.class, 100, true
        ));


    }




}
