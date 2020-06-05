package com.garyzhangscm.cwms.integration.model;


import java.time.LocalDateTime;
import java.util.List;

public interface IntegrationOrderData {

    String getNumber();
    Long getShipToCustomerId();
    String getShipToCustomerName();

    Long getWarehouseId();

    String getWarehouseName();

    Long getBillToCustomerId();

    String getBillToCustomerName();

    String getShipToContactorFirstname() ;
    String getShipToContactorLastname();

    String getShipToAddressCountry();

    String getShipToAddressState();
    String getShipToAddressCounty();

    String getShipToAddressCity();

    String getShipToAddressDistrict();

    String getShipToAddressLine1();
    String getShipToAddressLine2();
    String getShipToAddressPostcode();

    String getBillToContactorFirstname();

    String getBillToContactorLastname();

    String getBillToAddressCountry();
    String getBillToAddressState();
    String getBillToAddressCounty();
    String getBillToAddressCity() ;
    String getBillToAddressDistrict() ;

    String getBillToAddressLine1() ;

    String getBillToAddressLine2();
    String getBillToAddressPostcode();
    Long getCarrierId() ;

    String getCarrierName();
    Long getCarrierServiceLevelId();

    String getCarrierServiceLevelName();
    Long getClientId() ;
    String getClientName();
    Long getStageLocationGroupId();

    String getStageLocationGroupName();
    List<? extends IntegrationOrderLineData> getOrderLines() ;


    public IntegrationStatus getStatus() ;

    public LocalDateTime getInsertTime() ;

    public LocalDateTime getLastUpdateTime();
}
