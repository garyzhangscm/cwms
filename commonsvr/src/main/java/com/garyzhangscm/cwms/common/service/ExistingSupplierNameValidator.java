package com.garyzhangscm.cwms.common.service;

import com.garyzhangscm.cwms.common.model.Customer;
import com.garyzhangscm.cwms.common.model.Supplier;
import com.garyzhangscm.cwms.common.model.ValidatorResult;
import com.garyzhangscm.cwms.common.model.ValidatorType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
public class ExistingSupplierNameValidator implements Validator {


    private static final Logger logger = LoggerFactory.getLogger(ExistingSupplierNameValidator.class);

    @Autowired
    SupplierService supplierService;

    @Override
    public String validate(Long companyId, Long warehouseId, String value) {

        logger.debug("Start to validate supplier against {} / {} / {}",
               companyId, warehouseId, value);
        // make sure the customer name already exists
        Supplier supplier = supplierService.findByName(
                companyId, warehouseId, value
        );
        // Check if this is a new customer. If this is a new customer,
        // the above call should return an empty string.
        if (Objects.isNull(supplier)) {
            logger.debug("The supplier with name  {} / {} / {} doesn't exists",
                    companyId, warehouseId, value);
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
        return "supplier-name";
    }
}
