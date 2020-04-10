package com.garyzhangscm.cwms.integration.model;

import java.time.LocalDateTime;

public interface IntegrationSupplierData {

    public Long getId();

    public String getName();
    public String getDescription();
    public String getContactorFirstname() ;
    public String getContactorLastname() ;

    public String getAddressCountry() ;
    public String getAddressCounty();

    public String getAddressState();

    public String getAddressCity() ;

    public String getAddressDistrict() ;
    public String getAddressLine1() ;
    public String getAddressLine2() ;
    public String getAddressPostcode() ;


    public IntegrationStatus getStatus();
    public LocalDateTime getInsertTime();

    public LocalDateTime getLastUpdateTime();
}
