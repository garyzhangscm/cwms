package com.garyzhangscm.cwms.resources.service;

import com.garyzhangscm.cwms.resources.clients.LayoutServiceRestemplateClient;
import com.garyzhangscm.cwms.resources.exception.ReportAccessPermissionException;
import com.garyzhangscm.cwms.resources.exception.ResourceNotFoundException;
import com.garyzhangscm.cwms.resources.model.*;
import com.garyzhangscm.cwms.resources.repository.ReportHistoryRepository;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.stereotype.Service;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.*;

@Service
public class ReportHistoryService {
    private static final Logger logger
            = LoggerFactory.getLogger(ReportHistoryService.class);

    private static final String REPORT_RESULT_FOLDER = "/usr/local/reports";

    @Autowired
    private ReportHistoryRepository reportHistoryRepository;
    @Autowired
    private LayoutServiceRestemplateClient layoutServiceRestemplateClient;
    @Autowired
    private UserService userService;



    public ReportHistory findById(Long id) {

        return findById(id, true);
    }

    public ReportHistory findById(Long id, boolean includeDetails) {
        ReportHistory reportHistory = reportHistoryRepository.findById(id)
                .orElseThrow(
                        () -> ResourceNotFoundException.raiseException(
                                "report history  not found by id: " + id));
        if (Objects.nonNull(reportHistory) && includeDetails) {
            loadDetail(reportHistory);
        }
        return reportHistory;
    }

    public List<ReportHistory> findAll(Long companyId,
                                Long warehouseId,
                                String name,
                                String type) {
        return findAll(companyId, warehouseId, name, type, true);
    }
    public List<ReportHistory> findAll(Long companyId,
                                Long warehouseId,
                                String name,
                                String type,
                                boolean includeDetails) {

        List<ReportHistory> reportHistories =  reportHistoryRepository.findAll(
                (Root<ReportHistory> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) -> {
                    List<Predicate> predicates = new ArrayList<Predicate>();

                    if (Objects.nonNull(companyId)) {
                        predicates.add(
                                criteriaBuilder.equal(
                                        root.get("companyId"), companyId));
                    }
                    if (Objects.nonNull(warehouseId)) {
                        predicates.add(
                                criteriaBuilder.equal(
                                        root.get("warehouseId"), warehouseId));
                    }



                    if (!StringUtils.isBlank(name)) {
                        predicates.add(criteriaBuilder.equal(root.get("name"), name));
                    }

                    if (!StringUtils.isBlank(type)) {
                        predicates.add(criteriaBuilder.equal(root.get("type"),
                                ReportType.valueOf(type)));
                    }



                    Predicate[] p = new Predicate[predicates.size()];
                    return criteriaBuilder.and(predicates.toArray(p));
                }
        );

        if (reportHistories.size() > 0 && includeDetails) {
            loadDetail(reportHistories);
        }
        return reportHistories;

    }




    public void loadDetail(List<ReportHistory> reportHistories) {
        for(ReportHistory reportHistory : reportHistories) {
            loadDetail(reportHistory);
        }
    }

    public void loadDetail(ReportHistory reportHistory) {


        if (reportHistory.getWarehouseId() != null) {
            reportHistory.setWarehouse(
                    layoutServiceRestemplateClient.getWarehouseById(
                            reportHistory.getWarehouseId()));
        }



    }

    public ReportHistory save(ReportHistory reportHistory) {

        return reportHistoryRepository.save(reportHistory);
    }

    public ReportHistory saveReportHistory(
            Report reportMetaData,
            String reportFileName,
            Long warehouseId) {


        String printedUsername =
                userService.getCurrentUserName();
        ReportHistory reportHistory =
                new ReportHistory(reportMetaData, reportFileName,
                        printedUsername, warehouseId);
        return save(reportHistory);
    }

    public File getReportFile(String fileName) {
        if (!verifyReportResultFileAccess(fileName)) {
            throw ReportAccessPermissionException.raiseException(
                    "Current user doesn't have access to the report file"
            );
        }
        String fileUrl = REPORT_RESULT_FOLDER + "/" + fileName;

        logger.debug("Will return {} to the client",
                fileUrl);
        return new File(fileUrl);
    }

    public File getReportFile(Long reportHistoryId) {

        String reportFileName
                = findById(reportHistoryId, false).getFileName();

        return getReportFile(reportFileName);
    }

    public boolean verifyReportResultFileAccess(String filename) {
        return true;
    }






}
