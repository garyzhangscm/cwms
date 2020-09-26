package com.garyzhangscm.cwms.common.service;

import com.garyzhangscm.cwms.common.model.ValidatorType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ValidatorFactory {

    @Autowired
    List<Validator> validators;

    public Optional<Validator> getValidator(String variable, ValidatorType type) {
        return validators.stream()
                .filter(validator ->
                        validator.getVariable().equals(variable) && validator.getType().equals(type))
                .findFirst();
    }
}
