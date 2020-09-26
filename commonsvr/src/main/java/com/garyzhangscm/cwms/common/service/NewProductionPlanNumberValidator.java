package com.garyzhangscm.cwms.common.service;

import com.garyzhangscm.cwms.common.clients.WorkOrderServiceRestemplateClient;
import com.garyzhangscm.cwms.common.model.ValidatorType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class NewProductionPlanNumberValidator implements Validator {

    @Autowired
    WorkOrderServiceRestemplateClient workOrderServiceRestemplateClient;
    @Override
    public String validate(Long warehouseId, String value) {

        // make sure the bom number doesn't exists yet
        return workOrderServiceRestemplateClient.validatNewProductionPlanNumber(
                warehouseId, value
        );

    }

    @Override
    public ValidatorType getType() {
        return ValidatorType.VALIDATE_VALUE_NON_EXISTS;
    }

    @Override
    public String getVariable() {
        return "production-plan";
    }
}
