package com.garyzhangscm.cwms.resources.service;

import com.garyzhangscm.cwms.resources.clients.LayoutServiceRestemplateClient;
import com.garyzhangscm.cwms.resources.clients.PrintingServiceRestemplateClient;
import com.garyzhangscm.cwms.resources.exception.ReportAccessPermissionException;
import com.garyzhangscm.cwms.resources.exception.ResourceNotFoundException;
import com.garyzhangscm.cwms.resources.model.*;
import com.garyzhangscm.cwms.resources.repository.ReportHistoryRepository;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.awt.print.PageFormat;
import java.awt.print.Paper;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.io.File;
import java.io.IOException;
import java.util.*;

import java.awt.print.PrinterJob;
import java.io.File;

import javax.print.PrintService;
import javax.print.PrintServiceLookup;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.printing.PDFPageable;

@Service
public class ReportHistoryService {
    private static final Logger logger
            = LoggerFactory.getLogger(ReportHistoryService.class);

    @Value("${report.result.folder}")
    private String reportResultFolder;

    @Autowired
    private ReportHistoryRepository reportHistoryRepository;
    @Autowired
    private ReportPrinterConfigurationService reportPrinterConfigurationService;
    @Autowired
    private LayoutServiceRestemplateClient layoutServiceRestemplateClient;
    @Autowired
    private UserService userService;
    @Autowired
    private ReportService reportService;
    @Autowired
    private PrinterService printerService;
    @Autowired
    private PrintingServiceRestemplateClient printingServiceRestemplateClient;



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
                                String type) {
        return findAll(companyId, warehouseId, type, true);
    }
    public List<ReportHistory> findAll(Long companyId,
                                Long warehouseId,
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
        String fileUrl = reportResultFolder + "/" + fileName;

        logger.debug("Will return {} to the client",
                fileUrl);
        return new File(fileUrl);
    }

    public File getReportFile(Long companyId, Long warehouseId, String type, String filename) {
        if (!verifyReportResultFileAccess(filename)) {
            throw ReportAccessPermissionException.raiseException(
                    "Current user doesn't have access to the report file"
            );
        }

        // get the meta data of the report so we know where to get the
        // report result file
        // The directory depends on whether the report is specific
        // for the company or warehouse
        // 1. standard report: reportResultFolder
        // 2. company specific report: reportResultFolder/companyId
        // 3. warehouse specific report: reportResultFolder/companyId/warehouseId
        Report reportMetaData = reportService.findByType(companyId, warehouseId,
                ReportType.valueOf(type), null, false);

        String fileUrl = getReportResultFolder(reportMetaData) + filename;

        logger.debug("Will return {} to the client",
                fileUrl);
        return new File(fileUrl);
    }

    private String getReportResultFolder(Report report) {

        String folder;
        if (!reportResultFolder.endsWith("/")) {
            folder = reportResultFolder + "/";
        }
        else  {
            folder = reportResultFolder;
        }
        if (Objects.nonNull(report.getCompanyId())) {
            folder += report.getCompanyId() + "/";
        }
        if (Objects.nonNull(report.getWarehouseId())) {
            folder += report.getWarehouseId() + "/";
        }

        return folder;
    }

    public File getReportFile(Long reportHistoryId) {

        String reportFileName
                = findById(reportHistoryId, false).getFileName();

        return getReportFile(reportFileName);
    }

    public boolean verifyReportResultFileAccess(String filename) {
        return true;
    }


    public File printReport(Long companyId, Long warehouseId,
                            String type, String filename,
                            String findPrinterBy,
                            String printerName,
                            Integer copies)
        throws  IOException{


        File reportResultFile = getReportFile(companyId, warehouseId, type, filename);

        // String printer = printerService.getPrinter(companyId, warehouseId, ReportType.valueOf(type), findPrinterBy, printerName);

        // default the printer to the one that passed in. If the user didn't pass in
        // then we will use the printer defined by the configuration.
        // if there's no printer defined by the configuration, then we will set printer to null
        // which will tell the system to use the default printer
        String printer = printerName;

        ReportPrinterConfiguration reportPrinterConfiguration =
                reportPrinterConfigurationService.findByWarehouseIdAndReportTypeAndCriteriaValue(
                        warehouseId, ReportType.valueOf(type), findPrinterBy);

        // printer name is not passed in , let's get from the configuration
        if (Strings.isBlank(printerName) && Objects.nonNull(reportPrinterConfiguration)) {

            printer = reportPrinterConfiguration.getPrinterName();
        }
        // if copies is not passed in the user, get from the configuration or
        // default to 1 copy
        if (Objects.isNull(copies)) {
            copies = Objects.nonNull(reportPrinterConfiguration) ?
                    reportPrinterConfiguration.getCopies() :
                    1;
        }

        logger.debug("We find a printer by criertia {} / {} / {} / {}, printer name passed in? {}, IT IS {}",
                companyId, warehouseId, type, findPrinterBy, printerName, printer);
        logger.debug("and will printer copies: {} ", copies );

        printingServiceRestemplateClient.sendPrintingRequest(reportResultFile, ReportType.valueOf(type), printer, copies);

        // we will send the printing request to the remote printing service

        logger.debug("file printed!");
        return reportResultFile;
    }

}
