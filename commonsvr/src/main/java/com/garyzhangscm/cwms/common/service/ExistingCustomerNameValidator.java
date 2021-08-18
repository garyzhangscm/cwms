package com.garyzhangscm.cwms.common.service;

import com.garyzhangscm.cwms.common.clients.InventoryServiceRestemplateClient;
import com.garyzhangscm.cwms.common.model.Customer;
import com.garyzhangscm.cwms.common.model.ValidatorResult;
import com.garyzhangscm.cwms.common.model.ValidatorType;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service
public class ExistingCustomerNameValidator implements Validator {


    private static final Logger logger = LoggerFactory.getLogger(ExistingCustomerNameValidator.class);

    @Autowired
    CustomerService customerService;

    @Override
    public String validate(Long companyId, Long warehouseId, String value) {

        logger.debug("Start to validate customer against {} / {}",
                warehouseId, value);
        // make sure the customer name already exists
        Customer customer = customerService.findByName(
                warehouseId, value
        );
        // Check if this is a new customer. If this is a new customer,
        // the above call should return an empty string.
        if (Objects.isNull(customer)) {
            logger.debug("The customer with name {} / {} doesn't exists",
                    warehouseId, value);
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
        return "customer-name";
    }
}
