package com.garyzhangscm.cwms.common.service;

import com.garyzhangscm.cwms.common.model.Client;
import com.garyzhangscm.cwms.common.model.Customer;
import com.garyzhangscm.cwms.common.model.Supplier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class IntegrationService {

    @Autowired
    CustomerService customerService;
    @Autowired
    ClientService clientService;
    @Autowired
    SupplierService supplierService;

    public void save(Customer customer) {

        customerService.saveOrUpdate(customer);
    }

    public void save(Client client) {

        clientService.saveOrUpdate(client);
    }

    public void save(Supplier supplier) {

        supplierService.saveOrUpdate(supplier);
    }
}
