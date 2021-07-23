package com.garyzhangscm.cwms.dblink.model;

public enum IntegrationStatus {
    PREPARING,  // host preparing the integration data
    ATTACHED,   // attached to the parent so we will process this integration data along with the parent data
    PENDING,    // we can process this data, along with any child that attached to it
    INPROCESS,  // we are processing the data
    ERROR,      // we have error processing the data
    COMPLETED   // we complete processing the data, long with any child that attached to it.
}
