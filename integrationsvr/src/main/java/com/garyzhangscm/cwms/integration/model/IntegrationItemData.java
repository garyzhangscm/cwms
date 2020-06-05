package com.garyzhangscm.cwms.integration.model;

import java.time.LocalDateTime;
import java.util.List;

public interface IntegrationItemData extends IntegrationData{

    public String getName();
    public String getDescription();

    public Long getClientId();

    public IntegrationItemFamilyData getItemFamily();

    public List<? extends  IntegrationItemPackageTypeData> getItemPackageTypes() ;


    public double getUnitCost() ;




    public IntegrationStatus getStatus() ;

    public LocalDateTime getInsertTime() ;

    public LocalDateTime getLastUpdateTime();
}
