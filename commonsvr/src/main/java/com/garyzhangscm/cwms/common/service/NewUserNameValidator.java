package com.garyzhangscm.cwms.common.service;

import com.garyzhangscm.cwms.common.clients.InventoryServiceRestemplateClient;
import com.garyzhangscm.cwms.common.clients.ResourceServiceRestemplateClient;
import com.garyzhangscm.cwms.common.model.ValidatorType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class NewUserNameValidator implements Validator {

    @Autowired
    ResourceServiceRestemplateClient resourceServiceRestemplateClient;

    @Override
    public String validate(Long companyId, Long warehouseId, String value) {

        // make sure the bom number doesn't exists yet
        return resourceServiceRestemplateClient.validateNewUsername(
                companyId, warehouseId, value
        );

    }

    @Override
    public ValidatorType getType() {
        return ValidatorType.VALIDATE_VALUE_NON_EXISTS;
    }

    @Override
    public String getVariable() {
        return "username";
    }
}
