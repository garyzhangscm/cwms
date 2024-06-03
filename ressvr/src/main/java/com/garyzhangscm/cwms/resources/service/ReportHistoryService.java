package com.garyzhangscm.cwms.resources.service;

import com.garyzhangscm.cwms.resources.clients.LabelaryRestemplateClient;
import com.garyzhangscm.cwms.resources.clients.LayoutServiceRestemplateClient;
import com.garyzhangscm.cwms.resources.clients.PrintingServiceRestemplateClient;
import com.garyzhangscm.cwms.resources.exception.ReportAccessPermissionException;
import com.garyzhangscm.cwms.resources.exception.ResourceNotFoundException;
import com.garyzhangscm.cwms.resources.exception.SystemFatalException;
import com.garyzhangscm.cwms.resources.model.*;
import com.garyzhangscm.cwms.resources.repository.ReportHistoryRepository;
import org.apache.commons.io.FilenameUtils;
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
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;


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

    @Autowired
    private LabelaryRestemplateClient labelaryRestemplateClient;


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
            Long companyId, Long warehouseId) {


        String printedUsername =
                userService.getCurrentUserName();
        ReportHistory reportHistory =
                new ReportHistory(reportMetaData, reportFileName,
                        printedUsername, companyId, warehouseId);
        return save(reportHistory);
    }

    public File getReportFile(Long companyId, Long warehouseId, String fileName) {
        if (!verifyReportResultFileAccess(fileName)) {
            throw ReportAccessPermissionException.raiseException(
                    "Current user doesn't have access to the report file"
            );
        }
        String fileUrl = reportResultFolder + "/" + companyId + "/" + warehouseId + "/" + fileName;

        logger.debug("Will return {} to the client",
                fileUrl);
        return new File(fileUrl);
    }

    public File getReportFile(Long companyId, Long warehouseId, String type,
                              String filename) {
        return getReportFile(companyId, warehouseId, type,
                filename, "");
    }
    public File getReportFile(Long companyId, Long warehouseId, String type,
                              String filename, String printerName) {
        return getReportFile(companyId, warehouseId, type, filename, printerName,
                false);
    }

    /**
     * return the report file, which is already filled with data.
     * if it is to preview and the report is actually a label, we will try to convert it to
     * PDF so the client can review it but for print purpose, we will still return .lbl file
     * so that zebra printer can accept the print job
     * @param companyId
     * @param warehouseId
     * @param type
     * @param filename
     * @param printerName
     * @param preview
     * @return
     */
    public File getReportFile(Long companyId, Long warehouseId, String type,
                              String filename, String printerName, Boolean preview) {
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

        // Report reportMetaData = reportService.findByType(companyId, warehouseId,
        //        ReportType.valueOf(type), null, false);

        // Report reportMetaData = reportService.findByType(companyId, warehouseId,
         //       ReportType.valueOf(type), printerName, false);

        String fileUrl = getReportResultFolder(companyId, warehouseId) + filename;

        logger.debug("Will return {} to the client",
                fileUrl);

        ReportType reportType = ReportType.valueOf(type);
        File file = new File(fileUrl);
        logger.debug("see if we will need to convert a label file to PDF");
        logger.debug("preview mode? {}", preview );
        logger.debug("file is label? {}", reportType.isLabel() );
        logger.debug("file {}'s extension is prn / lbl? {}",
                file.getAbsolutePath(),
                fileIsLabel(file));

        if (Boolean.TRUE.equals(preview) &&
                reportType.isLabel() &&
                fileIsLabel(file)) {
            logger.debug("OK, we will need to convert the label files to PDF and return");
                return convertLabelFileToPDF(file);
        }


        return  file;


    }

    private boolean fileIsLabel(File file) {
        return FilenameUtils.getExtension(file.getName()).equalsIgnoreCase("prn") ||
                FilenameUtils.getExtension(file.getName()).equalsIgnoreCase("lbl");
    }
    /**
     * Convert label file (ext with PRN) to PDF file
     * @param zplFile
     * @return
     */
    private File convertLabelFileToPDF(File zplFile) {
        // use labelary.com to convert from ZPL into PDF
        // May consider https://www.labelzoom.net/ as well
        return labelaryRestemplateClient.convertZPLToPDF(zplFile);


    }

    private String getReportResultFolder(Long companyId, Long warehouseId) {

        String folder;
        if (!reportResultFolder.endsWith("/")) {
            folder = reportResultFolder + "/";
        }
        else  {
            folder = reportResultFolder;
        }

        folder += companyId + "/" + warehouseId + "/";

        return folder;
    }

    public File getReportFile(Long reportHistoryId) {

        ReportHistory reportHistory = findById(reportHistoryId, false);

        String reportFileName
                = reportHistory.getFileName();

        return getReportFile(reportHistory.getCompanyId(), reportHistory.getWarehouseId(), reportFileName);
    }

    public boolean verifyReportResultFileAccess(String filename) {
        return true;
    }


    public void printReportInBatch(Long companyId, Long warehouseId,
                            String type, String filenames,
                            String findPrinterBy,
                            String printerName,
                            Integer copies) {
        String[] fileNameArray = filenames.split(",");

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

        for (String filename : fileNameArray) {

            File reportResultFile = getReportFile(companyId, warehouseId, type,
                    filename, printerName);

            logger.debug("We find a printer by criertia {} / {} / {} / {}, printer name passed in? {}, IT IS {}",
                    companyId, warehouseId, type, findPrinterBy, printerName, printer);
            logger.debug("and will printer copies: {} ", copies );

            printingServiceRestemplateClient.sendPrintingRequest(reportResultFile, ReportType.valueOf(type), printer, copies);

            // we will send the printing request to the remote printing service

            logger.debug("file printed!");
        }
    }
    public File printReport(Long companyId, Long warehouseId,
                            String type, String filename,
                            String findPrinterBy,
                            String printerName,
                            Integer copies)
        throws  IOException{


        File reportResultFile = getReportFile(companyId, warehouseId, type,
                filename, printerName);

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

    /**
     * Combine multiple labels into one label file so that we can print in one printer queue and make sure
     * all the related labels can be printed together
     * @param companyId
     * @param warehouseId
     * @return
     */
    public ReportHistory combineLabels(Long companyId, Long warehouseId, List<ReportHistory> reportHistories) throws IOException {

        StringBuilder labelsContent = new StringBuilder();
        for(ReportHistory reportHistory : reportHistories) {
            logger.debug("Start to get label file by reportHistory {}", reportHistory);
            if (!reportHistory.getType().isLabel()) {
                throw SystemFatalException.raiseException("fail to combine as the type " +
                        reportHistory.getType() + " is not a label");
            }

            File file = getReportFile(companyId, warehouseId, reportHistory.getType().name(), reportHistory.getFileName());

            logger.debug("get file {}", file.getAbsolutePath());
            String labelContent = new String(
                    Files.readAllBytes(
                            Paths.get(file.getAbsolutePath())));
            logger.debug("=========    label content =========\n{}", labelContent);
            labelsContent.append(labelContent);
        }
        int hashCode = reportHistories.hashCode();

        // let's combine all the file together
        String reportFileName = "LABEL_COMBINED" + "_" + System.currentTimeMillis() + "_" + hashCode
                + ".lbl";
        String reportResultAbsoluteFileName =
                getReportResultFolder(companyId, warehouseId)
                        + reportFileName;

        logger.debug("start to save the new combined file {}", reportResultAbsoluteFileName);

        // save the result to local file
        String reportResultFileName = reportService.writeResultFile(
                reportFileName, reportResultAbsoluteFileName, labelsContent.toString());


        // save the history information
        ReportHistory reportHistory =
                new ReportHistory();
        reportHistory.setCompanyId(companyId);
        reportHistory.setWarehouseId(warehouseId);
        reportHistory.setPrintedDate(ZonedDateTime.now());
        reportHistory.setPrintedUsername(userService.getCurrentUserName());
        reportHistory.setType(ReportType.COMBINED_LABELS);
        reportHistory.setFileName(reportResultFileName);
        reportHistory.setDescription(ReportType.COMBINED_LABELS.toString());
        reportHistory.setReportOrientation(ReportOrientation.PORTRAIT);

        return save(reportHistory);

    }
}
