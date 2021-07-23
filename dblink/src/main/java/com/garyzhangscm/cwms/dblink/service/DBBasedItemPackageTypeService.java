package com.garyzhangscm.cwms.dblink.service;

import com.garyzhangscm.cwms.dblink.client.IntegrationServiceRestemplateClient;
import com.garyzhangscm.cwms.dblink.model.DBBasedItem;
import com.garyzhangscm.cwms.dblink.model.DBBasedItemPackageType;
import com.garyzhangscm.cwms.dblink.model.IntegrationStatus;
import com.garyzhangscm.cwms.dblink.repository.DBBasedItemPackageTypeRepository;
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
public class DBBasedItemPackageTypeService {

    private static final Logger logger = LoggerFactory.getLogger(DBBasedItemPackageTypeService.class);

    @Autowired
    DBBasedItemPackageTypeRepository dbBasedItemPackageTypeRepository;
    @Autowired
    IntegrationServiceRestemplateClient integrationServiceRestemplateClient;


    public List<DBBasedItemPackageType> findAll() {
        return dbBasedItemPackageTypeRepository.findAll();
    }

    private List<DBBasedItemPackageType> findPendingIntegration() {
        return dbBasedItemPackageTypeRepository.findAll(
                (Root<DBBasedItemPackageType> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) -> {
                    List<Predicate> predicates = new ArrayList<Predicate>();

                    predicates.add(criteriaBuilder.equal(root.get("status"), IntegrationStatus.PENDING));

                    Predicate[] p = new Predicate[predicates.size()];
                    return criteriaBuilder.and(predicates.toArray(p));
                }
        ).stream().limit(30).collect(Collectors.toList());
    }

    private void save(DBBasedItemPackageType dbBasedItemPackageType) {
        dbBasedItemPackageTypeRepository.save(dbBasedItemPackageType);
    }

    public void sendIntegrationData() {
        List<DBBasedItemPackageType> pendingDBBasedItemPackageType = findPendingIntegration();
        System.out.println("# find " +  pendingDBBasedItemPackageType.size() + " pendingDBBasedItemPackageType");
        pendingDBBasedItemPackageType.forEach(
                dbBasedItemPackageType -> {
                    System.out.println("# start to process item package type " + dbBasedItemPackageType.getName());
                    String result = "";
                    String errorMessage = "";
                    try {
                        result = integrationServiceRestemplateClient.sendIntegrationData("item-package-type", dbBasedItemPackageType);
                        System.out.println("# get result " + result);
                    }
                    catch (Exception ex) {
                        ex.printStackTrace();
                        result = "false";
                        errorMessage = ex.getMessage();
                    }
                    if (result.equalsIgnoreCase("success")) {
                        dbBasedItemPackageType.setStatus(IntegrationStatus.COMPLETED);
                    }
                    else {
                        dbBasedItemPackageType.setStatus(IntegrationStatus.ERROR);
                    }
                    save(dbBasedItemPackageType);
                }
        );
    }



}
