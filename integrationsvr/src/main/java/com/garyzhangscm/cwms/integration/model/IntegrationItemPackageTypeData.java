package com.garyzhangscm.cwms.integration.model;

import java.time.LocalDateTime;
import java.util.List;

public interface IntegrationItemPackageTypeData {

    public Long getId() ;
    public String getName() ;
    public String getDescription();
    public Long getClientId();

    public Long getSupplierId() ;


    public DBBasedItem getItem() ;

    public List<DBBasedItemUnitOfMeasure> getItemUnitOfMeasures() ;


    public Long getWarehouseId();


    public IntegrationStatus getStatus() ;

    public LocalDateTime getInsertTime();


    public LocalDateTime getLastUpdateTime();
}
