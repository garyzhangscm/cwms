package com.garyzhangscm.cwms.common.service;

import com.garyzhangscm.cwms.common.model.ValidatorType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.Optional;

@Service
public class ValidateService {

    @Autowired
    ValidatorFactory validatorFactory;

    public String validate(Long warehouseId, String variable, String validatorType, String value) {
        return validate(warehouseId, variable, ValidatorType.valueOf(validatorType), value);
    }

    /**
     * Validate the value for the variable. Return error code if there's any error
     * return empty string if no error
     * @param warehouseId
     * @param variable variable
     * @param type validator type
     * @param value value
     * @return
     */
    public String validate(Long warehouseId, String variable, ValidatorType type, String value) {
        Validator validator = validatorFactory.getValidator(variable, type).orElse(null);
        if (Objects.isNull(validator)) {
            // If there's no validator against this variable / type,
            // pass the validation
            return "";
        }
        return validator.validate(warehouseId, value);
    }
}
