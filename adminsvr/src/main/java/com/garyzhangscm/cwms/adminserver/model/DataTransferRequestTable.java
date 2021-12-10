package com.garyzhangscm.cwms.adminserver.model;

public enum DataTransferRequestTable {
    WAREHOUSE,
    POLICY,
    SYSTEM_CONTROLLED_NUMBER,
    UNIT_OF_MEASURE,
    LOCATION_GROUP,
    LOCATION,
    CUSTOMER,
    SUPPLIER,
    INVENTORY_STATUS,
    ITEM_FAMILY,
    ITEM,        // Include item / item package type / item unit of measure
    INVENTORY_CONFIGURATION,
    INVENTORY,
    RECEIPT,
    OUTBOUND_ORDER,   // include outbound order and picks and short allocations and etc.
    CYCLE_COUNT,   // include cycle count / audit count / cycle count result / audit count result

}
