package com.garyzhangscm.cwms.adminserver.controller;

import com.garyzhangscm.cwms.adminserver.model.tester.TestScenarioSuit;
import com.garyzhangscm.cwms.adminserver.service.TestScenarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;


@RestController()
public class TesterController {

    @Autowired
    private TestScenarioService testScenarioService;


    @RequestMapping(value="/tester/runAll", method = RequestMethod.POST)
    public Boolean runAll(@RequestParam String warehouseName) {

        testScenarioService.run(warehouseName);
        return true;
    }

    @RequestMapping(value="/tester/suits", method = RequestMethod.GET)
    public TestScenarioSuit getTestSuits(@RequestParam String warehouseName) {

        return testScenarioService.getTestSuits(warehouseName);
    }


}
