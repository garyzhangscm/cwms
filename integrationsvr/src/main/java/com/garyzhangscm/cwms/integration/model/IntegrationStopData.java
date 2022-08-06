package com.garyzhangscm.cwms.integration.model;

import java.util.List;

public interface IntegrationStopData {
    public Long getId();


    public Long getCompanyId();

    public String getCompanyCode();

    public String getWarehouseName();


    public Long getWarehouseId();

    public String getNumber();


    public Long getSequence();

    public String getContactorFirstname();

    public String getContactorLastname();
    public String getAddressCountry();
    public String getAddressState();
    public String getAddressCounty();

    public String getAddressCity();

    public String getAddressDistrict();

    public String getAddressLine1();

    public String getAddressLine2();
    public String getAddressPostcode();
    public IntegrationStatus getStatus();

    public String getErrorMessage();

    public List<? extends IntegrationShipmentData> getShipments();
    public IntegrationTrailerAppointmentData getTrailerAppointment();
}
