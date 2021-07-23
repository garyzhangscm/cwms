package com.garyzhangscm.cwms.dblink.service;

import com.garyzhangscm.cwms.dblink.client.IntegrationServiceRestemplateClient;
import com.garyzhangscm.cwms.dblink.model.DBBasedWorkOrder;
import com.garyzhangscm.cwms.dblink.model.IntegrationStatus;
import com.garyzhangscm.cwms.dblink.repository.DBBasedWorkOrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class DBBasedWorkOrderService {

    private static final Logger logger = LoggerFactory.getLogger(DBBasedWorkOrderService.class);

    @Autowired
    DBBasedWorkOrderRepository dbBasedWorkOrderRepository;
    @Autowired
    IntegrationServiceRestemplateClient integrationServiceRestemplateClient;


    public List<DBBasedWorkOrder> findAll() {
        return dbBasedWorkOrderRepository.findAll();
    }



    private List<DBBasedWorkOrder> findPendingIntegration() {
        return dbBasedWorkOrderRepository.findAll(
                (Root<DBBasedWorkOrder> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) -> {
                    List<Predicate> predicates = new ArrayList<Predicate>();

                    predicates.add(criteriaBuilder.equal(root.get("status"), IntegrationStatus.PENDING));

                    Predicate[] p = new Predicate[predicates.size()];
                    return criteriaBuilder.and(predicates.toArray(p));
                }
        ).stream().limit(30).collect(Collectors.toList());
    }

    private void save(DBBasedWorkOrder dbBasedWorkOrder) {
        dbBasedWorkOrderRepository.save(dbBasedWorkOrder);
    }

    public void sendIntegrationData() {
        List<DBBasedWorkOrder> pendingDBBasedWorkOrder = findPendingIntegration();
        System.out.println("# find " +  pendingDBBasedWorkOrder.size() + " pendingDBBasedWorkOrder");
        pendingDBBasedWorkOrder.forEach(
                dbBasedWorkOrder -> {
                    System.out.println("# start to process work order " + dbBasedWorkOrder.getNumber());
                    String result = "";
                    String errorMessage = "";
                     try {
                         result = integrationServiceRestemplateClient.sendIntegrationData("work-order", dbBasedWorkOrder);
                         System.out.println("# get result " + result);
                     }
                     catch (Exception ex) {
                         ex.printStackTrace();
                         result = "false";
                         errorMessage = ex.getMessage();
                     }
                     if (result.equalsIgnoreCase("success")) {
                         dbBasedWorkOrder.setStatus(IntegrationStatus.COMPLETED);
                     }
                     else {
                         dbBasedWorkOrder.setStatus(IntegrationStatus.ERROR);
                     }
                     save(dbBasedWorkOrder);
                }
        );
    }


}
