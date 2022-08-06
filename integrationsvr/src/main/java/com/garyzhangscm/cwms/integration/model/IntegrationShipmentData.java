package com.garyzhangscm.cwms.integration.model;

import java.util.List;

public interface IntegrationShipmentData {
    public Long getId();

    public Long getCompanyId();
    public String getCompanyCode();
    public String getWarehouseName();
    public Long getWarehouseId();
    public String getNumber();
    public Long getCarrierId();

    public Long getCarrierServiceLevelId();

    public Long getShipToCustomerId();

    public String getShipToContactorFirstname();

    public String getShipToContactorLastname();

    public String getShipToAddressCountry();
    public String getShipToAddressState();
    public String getShipToAddressCounty();
    public String getShipToAddressCity();
    public String getShipToAddressDistrict();
    public String getShipToAddressLine1();
    public String getShipToAddressLine2();

    public String getShipToAddressPostcode();

    public Long getClientId();

    public List<? extends IntegrationShipmentLineData> getShipmentLines();

    public IntegrationStopData getStop();
    public IntegrationStatus getStatus();

    public String getErrorMessage();
}
