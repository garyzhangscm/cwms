package com.garyzhangscm.cwms.integration.model;

import java.util.List;

public interface IntegrationItemData extends IntegrationData{

    public String getName();
    public String getDescription();

    public Long getClientId();

    public IntegrationItemFamilyData getItemFamily();

    public List<? extends  IntegrationItemPackageTypeData> getItemPackageTypes() ;


    public Double getUnitCost() ;


    public Long getCompanyId();
    public String getCompanyCode() ;

    public Boolean getNonInventoryItem();


    public IntegrationStatus getStatus() ;


}
