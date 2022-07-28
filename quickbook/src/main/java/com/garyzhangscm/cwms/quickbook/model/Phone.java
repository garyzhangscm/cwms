package com.garyzhangscm.cwms.quickbook.model;

import java.io.Serializable;

/**
 * Phone number
 */
public class Phone implements Serializable {

    private Long id;

    private String deviceType;
    private String countryCode;
    private String areaCode;
    private String extension;
    private String freeFormNumber;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getDeviceType() {
        return deviceType;
    }

    public void setDeviceType(String deviceType) {
        this.deviceType = deviceType;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public void setCountryCode(String countryCode) {
        this.countryCode = countryCode;
    }

    public String getAreaCode() {
        return areaCode;
    }

    public void setAreaCode(String areaCode) {
        this.areaCode = areaCode;
    }

    public String getExtension() {
        return extension;
    }

    public void setExtension(String extension) {
        this.extension = extension;
    }

    public String getFreeFormNumber() {
        return freeFormNumber;
    }

    public void setFreeFormNumber(String freeFormNumber) {
        this.freeFormNumber = freeFormNumber;
    }
}
