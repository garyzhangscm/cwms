package com.garyzhangscm.cwms.integration.model;

import java.time.LocalDateTime;

public interface IntegrationClientData {

    public Long getId();


    public Long getCompanyId();
    public String getCompanyCode() ;
    public Long getWarehouseId();
    public String getWarehouseName() ;


    public String getName();



    public String getDescription();



    public String getContactorFirstname();



    public String getContactorLastname();


    public String getAddressCountry();



    public String getAddressCounty();

    public String getAddressState();



    public String getAddressCity();



    public String getAddressDistrict();



    public String getAddressLine1();



    public String getAddressLine2();



    public String getAddressPostcode();



    public IntegrationStatus getStatus();




}
