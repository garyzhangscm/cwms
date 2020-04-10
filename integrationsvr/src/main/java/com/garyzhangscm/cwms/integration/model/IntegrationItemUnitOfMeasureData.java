package com.garyzhangscm.cwms.integration.model;

import java.time.LocalDateTime;

public interface IntegrationItemUnitOfMeasureData {

    public Long getId();
    public Long getUnitOfMeasureId();

    public Integer getQuantity() ;
    public Double getWeight() ;
    public Double getLength() ;

    public Double getWidth() ;
    public Double getHeight() ;
    public DBBasedItemPackageType getItemPackageType();
    public Long getWarehouseId();

    public String getUnitOfMeasureName();
    public String getWarehouseName() ;
    public IntegrationStatus getStatus() ;
    public LocalDateTime getInsertTime() ;

    public LocalDateTime getLastUpdateTime() ;
    public Long getItemId() ;

    public String getItemName();
    public Long getItemPackageTypeId() ;
    public String getItemPackageTypeName() ;
}
