package com.garyzhangscm.cwms.resources.model;

import java.util.ArrayList;
import java.util.List;

public class FileUploadType {
    private String name;
    private String description;
    private String destinationUrl;
    private String templateFileUrl;
    private String trackingProgressUrl;

    private List<FileUploadTemplateColumn> columns = new ArrayList<>();

    public FileUploadType(){}

    public FileUploadType(String name,
                          String description,
                          String destinationUrl,
                          String templateFileUrl,
                          String trackingProgressUrl) {
        this.name = name;
        this.description = description;
        this.destinationUrl = destinationUrl;
        this.templateFileUrl = templateFileUrl;
        this.trackingProgressUrl = trackingProgressUrl;
    }

    public static List<FileUploadType> getAvailableFileUploadTypes() {

        List<FileUploadType> fileUploadTypes = new ArrayList<>();

        fileUploadTypes.add(new BillOfMaterialFileUpload());
        fileUploadTypes.add(new ItemFileUpload());
        fileUploadTypes.add(new ItemUnitOfMeasureFileUpload());
        fileUploadTypes.add(new InventoryFileUpload());
        fileUploadTypes.add(new OrderFileUpload());
        fileUploadTypes.add(new ReceiptFileUpload());
        fileUploadTypes.add(new ReceivingInventoryFileUpload());
        fileUploadTypes.add(new LocationFileUpload());


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

    public String getTrackingProgressUrl() {
        return trackingProgressUrl;
    }

    public void setTrackingProgressUrl(String trackingProgressUrl) {
        this.trackingProgressUrl = trackingProgressUrl;
    }
}
