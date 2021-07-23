package com.garyzhangscm.cwms.outbound.model;

public enum OrderCategory {
    SALES_ORDER(false),
    WAREHOUSE_TRANSFER_ORDER(true);

    private boolean autoGenerateReceipt;
    private OrderCategory(boolean autoGenerateReceipt) {
        this.autoGenerateReceipt = autoGenerateReceipt;
    }

    public boolean isAutoGenerateReceipt() {
        return autoGenerateReceipt;
    }
}
