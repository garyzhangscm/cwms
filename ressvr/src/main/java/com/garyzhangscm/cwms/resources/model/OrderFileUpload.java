package com.garyzhangscm.cwms.resources.model;

public class OrderFileUpload extends FileUploadType {

    public OrderFileUpload(){
        super("orders", "Order",
                "outbound/orders/upload",
                "resource/assets/file-templates/orders.csv",
                "outbound/orders/upload/progress",
                "outbound/orders/upload/result");
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
                "line", "Line Number",
                String.class, 100, false
        ));
        addColumn(new FileUploadTemplateColumn(
                "item", "Item Number",
                String.class, 100, false
        ));
        addColumn(new FileUploadTemplateColumn(
                "expectedQuantity", "Order Quantity",
                Long.class, 100, false
        ));
        addColumn(new FileUploadTemplateColumn(
                "unitOfMeasure", "Unit of Measure",
                String.class, 20, true
        ));
        addColumn(new FileUploadTemplateColumn(
                "inventoryStatus", "Inventory Status",
                String.class, 100, true
        ));
        addColumn(new FileUploadTemplateColumn(
                "allocationStrategyType", "Allocation Strategy Type(Default to FIFO)",
                String.class, 100, true
        ));
        addColumn(new FileUploadTemplateColumn(
                "shipToCustomer", "Ship to Customer Number",
                String.class, 100, true
        ));
        addColumn(new FileUploadTemplateColumn(
                "billToCustomerSameAsShipToCustomer", "Bill to Customer is the same as Ship to Customer",
                String.class, 100, true
        ));
        addColumn(new FileUploadTemplateColumn(
                "billToCustomer", "Bill to Customer Number",
                String.class, 100, true
        ));
        addColumn(new FileUploadTemplateColumn(
                "shipToContactorFirstname", "Ship to Customer's Contactor, First Name",
                String.class, 100, true
        ));
        addColumn(new FileUploadTemplateColumn(
                "shipToContactorLastname", "Ship to Customer's Contactor, Last Name",
                String.class, 100, true
        ));
        addColumn(new FileUploadTemplateColumn(
                "shipToContactorPhoneNumber", "Ship to Customer's Contactor, Phone Number",
                String.class, 100, true
        ));
        addColumn(new FileUploadTemplateColumn(
                "shipToAddressCountry", "Ship to Customer Address - Country",
                String.class, 100, true
        ));
        addColumn(new FileUploadTemplateColumn(
                "shipToAddressState", "Ship to Customer Address - State",
                String.class, 100, true
        ));
        addColumn(new FileUploadTemplateColumn(
                "shipToAddressCounty", "Ship to Customer Address - County",
                String.class, 100, true
        ));
        addColumn(new FileUploadTemplateColumn(
                "shipToAddressCity", "Ship to Customer Address - City",
                String.class, 100, true
        ));
        addColumn(new FileUploadTemplateColumn(
                "shipToAddressDistrict", "Ship to Customer Address - District",
                String.class, 100, true
        ));
        addColumn(new FileUploadTemplateColumn(
                "shipToAddressLine1", "Ship to Customer Address - Line 1",
                String.class, 100, true
        ));
        addColumn(new FileUploadTemplateColumn(
                "shipToAddressLine2", "Ship to Customer Address - Line 2",
                String.class, 100, true
        ));
        addColumn(new FileUploadTemplateColumn(
                "shipToAddressPostcode", "Ship to Customer Address - Zip Code",
                String.class, 25, true
        ));
        addColumn(new FileUploadTemplateColumn(
                "billToAddressSameAsShipToAddress", "Is Bill to Address the Same as Ship To Address",
                String.class, 100, true
        ));
        addColumn(new FileUploadTemplateColumn(
                "billToContactorFirstname", "Bill to Customer's Contactor, First Name",
                String.class, 100, true
        ));
        addColumn(new FileUploadTemplateColumn(
                "billToContactorLastname", "Bill to Customer's Contactor, Last Name",
                String.class, 100, true
        ));
        addColumn(new FileUploadTemplateColumn(
                "billToAddressCountry", "Bill to Customer Address - Country",
                String.class, 100, true
        ));
        addColumn(new FileUploadTemplateColumn(
                "billToAddressState", "Bill to Customer Address - State",
                String.class, 100, true
        ));
        addColumn(new FileUploadTemplateColumn(
                "billToAddressCounty", "Bill to Customer Address - County",
                String.class, 100, true
        ));
        addColumn(new FileUploadTemplateColumn(
                "billToAddressCity", "Bill to Customer Address - City",
                String.class, 100, true
        ));
        addColumn(new FileUploadTemplateColumn(
                "billToAddressDistrict", "Bill to Customer Address - District",
                String.class, 100, true
        ));
        addColumn(new FileUploadTemplateColumn(
                "billToAddressLine1", "Bill to Customer Address - Line 1",
                String.class, 100, true
        ));
        addColumn(new FileUploadTemplateColumn(
                "billToAddressLine2", "Bill to Customer Address - Line 2",
                String.class, 100, true
        ));
        addColumn(new FileUploadTemplateColumn(
                "billToAddressPostcode", "Bill to Customer Address - Zip Code",
                String.class, 25, true
        ));

        addColumn(new FileUploadTemplateColumn(
                "color", "Color",
                String.class, 100, true
        ));
        addColumn(new FileUploadTemplateColumn(
                "productSize", "Product Size",
                String.class, 100, true
        ));
        addColumn(new FileUploadTemplateColumn(
                "style", "Style",
                String.class, 100, true
        ));
        addColumn(new FileUploadTemplateColumn(
                "hualeiProductId", "Hualei Product Id",
                String.class, 100, true
        ));
        addColumn(new FileUploadTemplateColumn(
                "autoRequestShippingLabel", "Auto Request Shipping Label",
                Boolean.class, 10, true
        ));
        addColumn(new FileUploadTemplateColumn(
                "parcelInsured", "If ship by Parcel, default to insure the package?",
                Boolean.class, 10, true
        ));
        addColumn(new FileUploadTemplateColumn(
                "parcelInsuredAmountPerUnit", "If ship by Parcel and insured, the insured amount per unit",
                Double.class, 10, true
        ));
        addColumn(new FileUploadTemplateColumn(
                "parcelSignatureRequired", "If ship by Parcel, default to Signature Required?",
                Boolean.class, 10, true
        ));

        addColumn(new FileUploadTemplateColumn(
                "inventoryAttribute1", "Inventory Attribute 1",
                String.class, 200, true
        ));
        addColumn(new FileUploadTemplateColumn(
                "inventoryAttribute2", "Inventory Attribute 2",
                String.class, 200, true
        ));
        addColumn(new FileUploadTemplateColumn(
                "inventoryAttribute3", "Inventory Attribute 3",
                String.class, 200, true
        ));
        addColumn(new FileUploadTemplateColumn(
                "inventoryAttribute4", "Inventory Attribute 4",
                String.class, 200, true
        ));
        addColumn(new FileUploadTemplateColumn(
                "inventoryAttribute5", "Inventory Attribute 5",
                String.class, 200, true
        ));

    }

}
