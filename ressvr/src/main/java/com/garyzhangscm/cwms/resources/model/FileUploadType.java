package com.garyzhangscm.cwms.resources.model;


import java.util.*;

public class FileUploadType {
    private String name;
    private String description;
    private String destinationUrl;
    private String templateFileUrl;
    private String trackingProgressUrl;
    private String resultUrl;

    // column map
    // key: from column name, the column name from the CSV file uploaded
    // value: to column name, the column name needed by the system
    private Map<String, String> columnsMapping = new HashMap<>();

    private List<FileUploadTemplateColumn> columns = new ArrayList<>();

    public FileUploadType(){}

    public FileUploadType(String name,
                          String description,
                          String destinationUrl,
                          String templateFileUrl,
                          String trackingProgressUrl,
                          String resultUrl) {
        this.name = name;
        this.description = description;
        this.destinationUrl = destinationUrl;
        this.templateFileUrl = templateFileUrl;
        this.trackingProgressUrl = trackingProgressUrl;
        this.resultUrl = resultUrl;
    }

    public static List<FileUploadType> getAvailableFileUploadTypes() {

        List<FileUploadType> fileUploadTypes = new ArrayList<>();

        fileUploadTypes.add(new BillOfMaterialFileUpload());
        fileUploadTypes.add(new ItemFileUpload());
        fileUploadTypes.add(new ItemUnitOfMeasureFileUpload());
        fileUploadTypes.add(new ItemBarcodeFileUpload());
        fileUploadTypes.add(new InventoryFileUpload());
        fileUploadTypes.add(new OrderFileUpload());
        fileUploadTypes.add(new ParcelPackageFileUpload());
        fileUploadTypes.add(new PutawayInventoryFileUpload());
        fileUploadTypes.add(new ReceiptFileUpload());
        fileUploadTypes.add(new ReceivingInventoryFileUpload());
        fileUploadTypes.add(new LocationFileUpload());
        fileUploadTypes.add(new WalmartShippingCartonLabelFileUpload());
        fileUploadTypes.add(new TargetShippingCartonLabelFileUpload());
        // fileUploadTypes.add(new ShippingTrailerAppointmentFileUpload());
        fileUploadTypes.add(new LoadFileUpload());
        fileUploadTypes.add(new EulogiaSupplierPackingSlipFileUpload());

        return fileUploadTypes;
    }


    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDestinationUrl() {
        return destinationUrl;
    }

    public void setDestinationUrl(String destinationUrl) {
        this.destinationUrl = destinationUrl;
    }

    public String getTemplateFileUrl() {
        return templateFileUrl;
    }

    public void setTemplateFileUrl(String templateFileUrl) {
        this.templateFileUrl = templateFileUrl;
    }

    public List<FileUploadTemplateColumn> getColumns() {
        return columns;
    }

    public void setColumns(List<FileUploadTemplateColumn> columns) {
        this.columns = columns;
    }

    public void addColumn(FileUploadTemplateColumn column) {
        this.columns.add(column);
    }

    public Map<String, String> getColumnsMapping() {
        return columnsMapping;
    }

    public void addColumnMap(String fromColumnName, String toColumnName) {
        columnsMapping.put(fromColumnName, toColumnName);
    }

    public void setColumnsMapping(Map<String, String> columnsMapping) {
        this.columnsMapping = columnsMapping;
    }

    public String getTrackingProgressUrl() {
        return trackingProgressUrl;
    }

    public void setTrackingProgressUrl(String trackingProgressUrl) {
        this.trackingProgressUrl = trackingProgressUrl;
    }

    public String getResultUrl() {
        return resultUrl;
    }

    public void setResultUrl(String resultUrl) {
        this.resultUrl = resultUrl;
    }
}
