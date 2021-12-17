package com.garyzhangscm.cwms.common.service;

import com.garyzhangscm.cwms.common.clients.InventoryServiceRestemplateClient;
import com.garyzhangscm.cwms.common.clients.OutbuondServiceRestemplateClient;
import com.garyzhangscm.cwms.common.model.ValidatorResult;
import com.garyzhangscm.cwms.common.model.ValidatorType;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ExistingOrderNumberValidator implements Validator {


    private static final Logger logger = LoggerFactory.getLogger(ExistingOrderNumberValidator.class);

    @Autowired
    private OutbuondServiceRestemplateClient outbuondServiceRestemplateClient;

    @Override
    public String validate(Long companyId, Long warehouseId, String value) {

        // newInventoryLockResult will have value if the lock name is not new
        // it will be empty if this is a new lock
        String newOrderNumberResult = outbuondServiceRestemplateClient.validateNewOrderNumber(
                warehouseId, value
        );
        // Check if this is an exist item. If this is a exist item,
        // the above call should return an empty string.
        if (StringUtils.isBlank(newOrderNumberResult)) {
            return ValidatorResult.VALUE_NON_EXISTS.toString();
        }
        else {
            return "";
        }

    }

    @Override
    public ValidatorType getType() {
        return ValidatorType.VALIDATE_VALUE_EXISTS;
    }

    @Override
    public String getVariable() {
        return "order-number";
    }
}
