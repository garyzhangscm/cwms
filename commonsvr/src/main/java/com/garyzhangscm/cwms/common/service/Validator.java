package com.garyzhangscm.cwms.common.service;

import com.garyzhangscm.cwms.common.model.ValidatorType;

public interface Validator {
    String validate(Long companyId, Long warehouseId, String value);

    ValidatorType getType();

    String getVariable();
}
