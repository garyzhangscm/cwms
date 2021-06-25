package com.garyzhangscm.cwms.resources.model;

import java.util.HashMap;
import java.util.Map;

public class WebClientConfiguration {


    // whether display tab in the web client
    // key: tab name
    // value: display flag
    Map<String, Boolean> tabDisplayConfiguration = new HashMap<>();

    public Map<String, Boolean> getTabDisplayConfiguration() {
        return tabDisplayConfiguration;
    }

    public void setTabDisplayConfiguration(Map<String, Boolean> tabDisplayConfiguration) {
        this.tabDisplayConfiguration = tabDisplayConfiguration;
    }
}
