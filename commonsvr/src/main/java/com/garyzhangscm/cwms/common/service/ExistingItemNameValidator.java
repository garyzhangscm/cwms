package com.garyzhangscm.cwms.common.service;

import com.garyzhangscm.cwms.common.clients.InventoryServiceRestemplateClient;
import com.garyzhangscm.cwms.common.model.ValidatorResult;
import com.garyzhangscm.cwms.common.model.ValidatorType;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ExistingItemNameValidator implements Validator {

    @Autowired
    InventoryServiceRestemplateClient inventoryServiceRestemplateClient;
    @Override
    public String validate(Long companyId, Long warehouseId, String value) {

        // make sure the bom number doesn't exists yet
        String newItemResult = inventoryServiceRestemplateClient.validateNewItemName(
                warehouseId, value
        );
        // Check if this is a new item. If this is a new item,
        // the above call should return an empty string.
        if (StringUtils.isBlank(newItemResult)) {
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
        return "item-name";
    }
}
