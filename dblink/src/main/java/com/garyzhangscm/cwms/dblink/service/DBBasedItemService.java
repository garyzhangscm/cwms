package com.garyzhangscm.cwms.dblink.service;

import com.garyzhangscm.cwms.dblink.client.IntegrationServiceRestemplateClient;
import com.garyzhangscm.cwms.dblink.model.DBBasedItem;
import com.garyzhangscm.cwms.dblink.model.DBBasedWorkOrder;
import com.garyzhangscm.cwms.dblink.model.IntegrationStatus;
import com.garyzhangscm.cwms.dblink.repository.DBBasedItemRepository;
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
public class DBBasedItemService {

    private static final Logger logger = LoggerFactory.getLogger(DBBasedItemService.class);

    @Autowired
    DBBasedItemRepository dbBasedItemRepository;
    @Autowired
    IntegrationServiceRestemplateClient integrationServiceRestemplateClient;


    public List<DBBasedItem> findAll() {
        return dbBasedItemRepository.findAll();
    }

    private List<DBBasedItem> findPendingIntegration() {
        return dbBasedItemRepository.findAll(
                (Root<DBBasedItem> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) -> {
                    List<Predicate> predicates = new ArrayList<Predicate>();

                    predicates.add(criteriaBuilder.equal(root.get("status"), IntegrationStatus.PENDING));

                    Predicate[] p = new Predicate[predicates.size()];
                    return criteriaBuilder.and(predicates.toArray(p));
                }
        ).stream().limit(30).collect(Collectors.toList());
    }

    private void save(DBBasedItem dbBasedItem) {
        dbBasedItemRepository.save(dbBasedItem);
    }

    public void sendIntegrationData() {
        List<DBBasedItem> pendingDBBasedItem = findPendingIntegration();
        System.out.println("# find " +  pendingDBBasedItem.size() + " pendingDBBasedItem");
        pendingDBBasedItem.forEach(
                dbBasedItem -> {
                    System.out.println("# start to process item " + dbBasedItem.getName());
                    String result = "";
                    String errorMessage = "";
                    try {
                        result = integrationServiceRestemplateClient.sendIntegrationData("item", dbBasedItem);
                        System.out.println("# get result " + result);
                    }
                    catch (Exception ex) {
                        ex.printStackTrace();
                        result = "false";
                        errorMessage = ex.getMessage();
                    }
                    if (result.equalsIgnoreCase("success")) {
                        dbBasedItem.setStatus(IntegrationStatus.COMPLETED);
                    }
                    else {
                        dbBasedItem.setStatus(IntegrationStatus.ERROR);
                    }
                    save(dbBasedItem);
                }
        );
    }



}
