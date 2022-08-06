package com.garyzhangscm.cwms.integration.model;

import java.util.List;

public interface IntegrationTrailerAppointmentData {


    public Long getWarehouseId();
    public Long getCompanyId();

    public String getNumber();

    public String getDescription();

    public Long getId();
    public String getCompanyCode();

    public String getWarehouseName();

    public TrailerAppointmentType getType();

    public IntegrationStatus getStatus();

    public String getErrorMessage();

    public List<? extends  IntegrationStopData> getStops();


}
