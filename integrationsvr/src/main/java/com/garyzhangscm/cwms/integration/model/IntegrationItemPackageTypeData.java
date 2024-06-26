package com.garyzhangscm.cwms.integration.model;

import java.util.List;

public interface IntegrationItemPackageTypeData {

    public Long getId() ;
    public String getName() ;
    public String getDescription();
    public Long getClientId();

    public Long getSupplierId() ;


    public DBBasedItem getItem() ;

    public List<DBBasedItemUnitOfMeasure> getItemUnitOfMeasures() ;


    public Long getCompanyId();
    public String getCompanyCode() ;
    public Long getWarehouseId();
    public String getWarehouseName() ;


    public IntegrationStatus getStatus() ;


}
