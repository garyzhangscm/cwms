package com.garyzhangscm.cwms.resources.model;

/**
 * upload supplier's packing slip. We will create the item, item unit of measure
 * and the receipt
 */
public class EulogiaSupplierPackingSlipFileUpload extends FileUploadType {

    public EulogiaSupplierPackingSlipFileUpload(){
        super("eulogia_customer_packing_slip", "Eulogia Customer Packing Slip",
                "inbound/receipts/eulogia_customer_packing_slip/upload",
                "resource/assets/file-templates/eulogia_customer_packing_slip.csv",
                "inbound/receipts/eulogia_customer_packing_slip/upload/progress",
                "inbound/receipts/eulogia_customer_packing_slip/upload/result");
        setupColumns();
    }

    private void setupColumns() {


        addColumn(new FileUploadTemplateColumn(
                 "client", "Client name(3PL)",
                String.class, 100, true
        ));

        addColumn(new FileUploadTemplateColumn(
                "supplier", "Supplier",
                String.class, 100, true
        ));


        addColumn(new FileUploadTemplateColumn(
                "receipt", "Receipt Number",
                String.class, 100, false
        ));

        addColumn(new FileUploadTemplateColumn(
                "line", "Receipt Line Number",
                String.class, 100, true
        ));

        addColumn(new FileUploadTemplateColumn(
                "item", "Item Number",
                String.class, 100, true
        ));

        addColumn(new FileUploadTemplateColumn(
                "expectedQuantity", "carton quantity",
                String.class, 100, false
        ));

        addColumn(new FileUploadTemplateColumn(
                "inventoryStatus", "Inventory Status",
                String.class, 100, true
        ));


        addColumn(new FileUploadTemplateColumn(
                "overReceivingQuantity", "Over Receiving Allowed By Quantity",
                String.class, 100, true
        ));

        addColumn(new FileUploadTemplateColumn(
                "overReceivingPercent", "Over Receiving Allowed By Percentage",
                String.class, 100, true
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
                "itemPackageType", "itemPackageType",
                String.class, 100, true
        ));

        addColumn(new FileUploadTemplateColumn(
                "cubicMeter", "cubicMeter",
                Double.class, 100, true
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


        addColumn(new FileUploadTemplateColumn(
                "unitPerPack", "Unit per Pack",
                String.class, 200, true
        ));
        addColumn(new FileUploadTemplateColumn(
                "packPerCarton", "Pack Per Carton",
                String.class, 200, true
        ));
        addColumn(new FileUploadTemplateColumn(
                "cartonWeight", "Carton Weight",
                String.class, 200, true
        ));
        addColumn(new FileUploadTemplateColumn(
                "cartonLength", "Carton Length",
                String.class, 200, true
        ));
        addColumn(new FileUploadTemplateColumn(
                "cartonWidth", "Carton Width",
                String.class, 200, true
        ));

    }




}
