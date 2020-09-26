package com.garyzhangscm.cwms.resources.model;

import java.lang.reflect.Type;

public class FileUploadTemplateColumn {

    private String name;
    private String description;

    private Class type;

    private Integer maxLength;

    private Boolean nullable;

    public FileUploadTemplateColumn() {}


    public FileUploadTemplateColumn(String name,
                                    String description,
                                    Class type,
                                    Integer maxLength,
                                    Boolean nullable) {
        this.name = name;
        this.description = description;
        this.type = type;
        this.maxLength = maxLength;
        this.nullable = nullable;
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

    public Class getType() {
        return type;
    }

    public void setType(Class type) {
        this.type = type;
    }

    public Integer getMaxLength() {
        return maxLength;
    }

    public void setMaxLength(Integer maxLength) {
        this.maxLength = maxLength;
    }

    public Boolean getNullable() {
        return nullable;
    }

    public void setNullable(Boolean nullable) {
        this.nullable = nullable;
    }
}
