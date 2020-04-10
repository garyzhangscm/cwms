package com.garyzhangscm.cwms.integration.model;

import java.time.LocalDateTime;
import java.util.List;

public interface IntegrationItemData {

    public Long getId();

    public String getName();
    public String getDescription();

    public Long getClientId();

    public DBBasedItemFamily getItemFamily();

    public List<DBBasedItemPackageType> getItemPackageTypes() ;


    public double getUnitCost() ;


    public Long getWarehouseId() ;

    public String getClientName() ;

    public String getWarehouseName() ;


    public IntegrationStatus getStatus() ;

    public LocalDateTime getInsertTime() ;

    public LocalDateTime getLastUpdateTime();
}
