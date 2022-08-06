package com.garyzhangscm.cwms.common.service;

import com.garyzhangscm.cwms.common.model.Client;
import com.garyzhangscm.cwms.common.model.Customer;
import com.garyzhangscm.cwms.common.model.Supplier;
import com.garyzhangscm.cwms.common.model.TrailerAppointment;
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
    @Autowired
    TrailerAppointmentService trailerAppointmentService;

    public void save(Customer customer) {

        customerService.saveOrUpdate(customer);
    }

    public void save(Client client) {

        clientService.saveOrUpdate(client);
    }

    public void save(Supplier supplier) {

        supplierService.saveOrUpdate(supplier);
    }

    public void save(TrailerAppointment trailerAppointment, long integrationId) {

        trailerAppointmentService.processIntegration(trailerAppointment, integrationId);
    }
}
