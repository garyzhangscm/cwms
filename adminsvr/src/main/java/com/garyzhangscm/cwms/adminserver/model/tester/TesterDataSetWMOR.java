package com.garyzhangscm.cwms.adminserver.model.tester;

import org.springframework.stereotype.Component;

@Component
public class TesterDataSetWMOR extends TesterDataSet{



    public TesterDataSetWMOR() {
        super("WMOR");
        initDataSet();
    }

    private void initDataSet() {
        initItemNames();

        initLocationNames();
    }

    private void initItemNames(){

    }
    private void initLocationNames(){

    }



}
