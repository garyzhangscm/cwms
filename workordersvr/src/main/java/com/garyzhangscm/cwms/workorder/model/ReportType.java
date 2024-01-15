package com.garyzhangscm.cwms.workorder.model;
public enum ReportType {
    ORDER_PICK_SHEET(false),
    RECEIVING_DOCUMENT(false),
    PUTAWAY_DOCUMENT(false),
    PICK_LIST_SHEET(false),
    DELIVERY_NOTE(false),
    PACKING_SLIP(false),
    CYCLE_COUNT_SHEET(false),
    AUDIT_COUNT_SHEET(false),
    LPN_REPORT(false),
    WORK_ORDER_PICK_SHEET(false),
    PRODUCTION_LINE_ASSIGNMENT_REPORT(false),
    BILL_OF_LADING(false),
    LPN_LABEL(true),
    PRODUCTION_LINE_ASSIGNMENT_LABEL(true),
    COMBINED_LABELS(true);

    private boolean isLabelFlag;

    private ReportType(boolean isLabelFlag) {
        this.isLabelFlag = isLabelFlag;
    }

    public boolean isLabel() {
        return  isLabelFlag;
    }
}
