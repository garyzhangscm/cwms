package com.garyzhangscm.cwms.common.model;


import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
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
    public boolean isDateColumn(String columnName) {
        Class columnType = getColumnType(columnName);
        if (Objects.isNull(columnType)) {
            return false;
        }
        if (columnType.equals(ZonedDateTime.class) ||
                columnType.equals(LocalDate.class) ||
                columnType.equals(LocalDateTime.class)) {
            return true;
        }
        return false;
    }

    public Class getColumnType(String columnName) {
        FileUploadTemplateColumn matchedColumn =
                columns.stream().filter(
                        column -> column.getName().equalsIgnoreCase(columnName)
                ).findFirst().orElse(null);

        // if the column name doesn't exists, it may be the alias
        if (Objects.isNull(matchedColumn)) {


            for (Map.Entry<String, String>  columnsMappingEntry: columnsMapping.entrySet()) {
                if (columnsMappingEntry.getValue().equalsIgnoreCase(columnName)) {
                    // ok, we get the column, let's find the actual column
                    matchedColumn =
                            columns.stream().filter(
                                    column -> column.getName().equalsIgnoreCase(columnsMappingEntry.getKey())
                            ).findFirst().orElse(null);
                }
            }
        }
        if (Objects.isNull(matchedColumn)) {
            return null;
        }
        else {
            return matchedColumn.getType();
        }
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
