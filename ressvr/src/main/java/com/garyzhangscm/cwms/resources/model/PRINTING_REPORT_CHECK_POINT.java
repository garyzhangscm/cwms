package com.garyzhangscm.cwms.resources.model;

public enum PRINTING_REPORT_CHECK_POINT {
    ORDER_COMPLETE("order"),
    WORK_ORDER_COMPLETE("work_order"),
    ORDER_ALLOCATION("work_order");

    private String tableList;
    private PRINTING_REPORT_CHECK_POINT(String tableList) {
        this.tableList = tableList;
    }

    public String getTableList() {
        return tableList;
    }
}
