package com.garyzhangscm.cwms.adminserver.model;

public enum BillableCategory {
    STORAGE_FEE_BY_NET_VOLUME(false),
    STORAGE_FEE_BY_GROSS_VOLUME(false),
    STORAGE_FEE_BY_LOCATION_COUNT(true),
    STORAGE_FEE_BY_PALLET_COUNT(true),
    STORAGE_FEE_BY_CASE_COUNT(true),
    HANDLING_FEE(false),
    ADDON_FEE(false),
    TRANSACTIONS(false),
    RECEIPT_LINE_PROCESS_FEE(false),
    RECEIPT_PROCESS_FEE(false),
    RECEIVING_CHARGE_BY_QUANTITY(false),
    RECEIVING_CHARGE_BY_VOLUME(false),
    ORDER_LINE_PROCESS_FEE(false),
    ORDER_PROCESS_FEE(false) ,
    SHIPPING_CHARGE_BY_QUANTITY(false),
    SHIPPING_CHARGE_BY_VOLUME(false),
    OTHERS(false),
    STORAGE_FEE_BY_AGING_PALLET(false);


    private boolean rateByQuantity;

    private BillableCategory(boolean rateByQuantity) {
        this.rateByQuantity = rateByQuantity;
    }

    public boolean isRateByQuantity() {
        return  rateByQuantity;
    }
}
