package com.garyzhangscm.cwms.common.service;

import com.garyzhangscm.cwms.common.model.Customer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class IntegrationService {

    @Autowired
    CustomerService customerService;

    public void save(Customer customer) {

        customerService.saveOrUpdate(customer);
    }
}
