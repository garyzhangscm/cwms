package com.garyzhangscm.cwms.integration.model;


import java.util.List;

public interface IntegrationOrderData {

    Long getId();
    String getNumber();
    Long getShipToCustomerId();
    String getShipToCustomerName();
    String getCategory();
    Long getTransferReceiptWarehouseId();
    String getTransferReceiptWarehouseName();

    public Long getCompanyId();
    public String getCompanyCode() ;
    public Long getWarehouseId();
    public String getWarehouseName() ;

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
    String getShipToAddressLine3();
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
    String getBillToAddressLine3();
    String getBillToAddressPostcode();
    Long getCarrierId() ;

    String getCarrierName();

    String getPoNumber();

    Long getCarrierServiceLevelId();

    String getCarrierServiceLevelName();
    Long getClientId() ;
    String getClientName();
    Long getStageLocationGroupId();

    String getStageLocationGroupName();
    List<? extends IntegrationOrderLineData> getOrderLines() ;
    public Boolean getAllowForManualPick();

    public IntegrationStatus getStatus() ;
    public String getErrorMessage();

}
