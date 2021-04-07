package com.garyzhangscm.cwms.resources.service;

import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import com.garyzhangscm.cwms.resources.clients.LayoutServiceRestemplateClient;
import com.garyzhangscm.cwms.resources.exception.ResourceNotFoundException;
import com.garyzhangscm.cwms.resources.model.*;
import com.garyzhangscm.cwms.resources.repository.ReportRepository;
import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import org.apache.commons.lang.StringUtils;
import org.apache.tomcat.util.http.fileupload.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

@Service
public class ReportService implements TestDataInitiableService{
    private static final Logger logger = LoggerFactory.getLogger(ReportService.class);


    // folder for source code of the report
    private static final String REPORT_CODE_FOLDER = "reports\\meta";

    // temporary folder to save the result(in format of html or pdf)
    // of the report.
    private static final String REPORT_RESULT_FOLDER = "/usr/local/reports";
    @Autowired
    private ReportRepository reportRepository;
    @Autowired
    private ReportHistoryService reportHistoryService;
    @Autowired
    private LayoutServiceRestemplateClient layoutServiceRestemplateClient;


    @Autowired
    private UserService userService;


    @Autowired
    private FileService fileService;
    @Value("${fileupload.test-data.reports:reports}")
    String testDataFile;


    public Report findById(Long id) {
        return findById(id, true);
    }

    public Report findById(Long id, boolean includeDetails) {
        Report report = reportRepository.findById(id)
                .orElseThrow(
                        () -> ResourceNotFoundException.raiseException(
                                "role  not found by id: " + id));
        if (Objects.nonNull(report) && includeDetails) {
            loadDetail(report);
        }
        return report;
    }

    public List<Report> findAll(Long companyId,
                                Long warehouseId,
                                String name,
                                String type) {
        return findAll(companyId, warehouseId, name, type, true);
    }
    public List<Report> findAll(Long companyId,
                                Long warehouseId,
                                String name,
                                String type,
                                boolean includeDetails) {

        List<Report> reports =  reportRepository.findAll(
                (Root<Report> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) -> {
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

        if (reports.size() > 0 && includeDetails) {
            loadDetail(reports);
        }
        return reports;

    }

    public Report findByName(Long companyId,
                             Long warehouseId,
                             String name) {
        return findByName(companyId, warehouseId, name, true);
    }
    public Report findByName(Long companyId,
                             Long warehouseId,
                             String name,
                             boolean includeDetails) {
        // check if we already have a customized report
        // 1. warehouse customized report
        // 2. company custmoized report
        Report warehouseReport = reportRepository.findByWarehouseIdAndName(
                warehouseId, name
        );
        if (Objects.nonNull(warehouseReport)) {
            if (includeDetails) {
                loadDetail(warehouseReport);
            }
            return warehouseReport;
        }
        Report companyReport = reportRepository.findByCompanyIdAndName(
                    companyId, name
            );
        if (Objects.nonNull(companyReport)) {
            if (includeDetails) {
                loadDetail(companyReport);
            }
            return companyReport;
        }

        // we don't have any customized version, let's
        // return the standard version
        Report standardReport =  findByName(name);
        if (Objects.nonNull(standardReport) && includeDetails) {
            loadDetail(standardReport);
        }
        return standardReport;

    }

    public  Report findByName(String name) {
        return findByName(name, true);
    }
    /**
     * Get the standard version of the report
     * @param name
     * @param  includeDetails whether includes the detail information
     * @return
     */
    public  Report findByName(String name, boolean includeDetails) {

        Report standardReport =  reportRepository.findStandardReportByName(name);

        if (Objects.nonNull(standardReport) && includeDetails) {
            loadDetail(standardReport);
        }
        return standardReport;
    }


    public void loadDetail(List<Report> reports) {
        for(Report report : reports) {
            loadDetail(report);
        }
    }

    public void loadDetail(Report report) {

        // Load company and warehouse information
        if (report.getCompanyId() != null) {
            report.setCompany(layoutServiceRestemplateClient.getCompanyById(report.getCompanyId()));
        }

        if (report.getWarehouseId() != null) {
            report.setWarehouse(layoutServiceRestemplateClient.getWarehouseById(report.getWarehouseId()));
        }



    }

    public Report save(Report report) {
        return reportRepository.save(report);
    }
    public Report saveOrUpdate(Report report) {
        if (Objects.isNull(report.getId())) {
            // get the matched report. Not this may not be a exact match
            // For example, when we pass in a warehouse customized report
            // but there's no such report, then the findByName may return
            // a company customized report, or a standard report
            Report matchedReport = findByName(report.getCompanyId(),
                    report.getWarehouseId(), report.getName());
            // Let's see if this is a exact match
            if (report.equals(matchedReport)) {
                report.setId(matchedReport.getId());
            }
        }
        return save(report);
    }


    public List<ReportCSVWrapper> loadData(InputStream inputStream)
            throws IOException {

        CsvSchema schema = CsvSchema.builder().
                addColumn("company").
                addColumn("warehouse").
                addColumn("name").
                addColumn("description").
                addColumn("type").
                addColumn("fileName").
                addColumn("orientation").
                build().withHeader();

        return fileService.loadData(inputStream, schema, ReportCSVWrapper.class);
    }

    public void initTestData(Long companyId, String warehouseName) {
        try {
            String companyCode = companyId == null ?
                    "" : layoutServiceRestemplateClient
                           .getCompanyById(companyId).getCode();

            String testDataFileName = StringUtils.isBlank(warehouseName) ?
                    testDataFile + ".csv" :
                    testDataFile + "-" + companyCode + "-" + warehouseName + ".csv";

            InputStream inputStream = new ClassPathResource(testDataFileName).getInputStream();
            List<ReportCSVWrapper> reportCSVWrappers = loadData(inputStream);
            reportCSVWrappers.stream().forEach(
                    reportCSVWrapper -> saveOrUpdate(convertFromCSVWrapper(reportCSVWrapper)));
        } catch (IOException ex) {
            logger.debug("Exception while load test data: {}", ex.getMessage());
        }
    }

    private Report convertFromCSVWrapper(ReportCSVWrapper reportCSVWrapper) {
        logger.debug("Start to init report");
        logger.debug("====   reportCSVWrapper  ====");
        logger.debug(reportCSVWrapper.toString());
        Report report = new Report();
        BeanUtils.copyProperties(reportCSVWrapper, report);
        report.setType(ReportType.valueOf(reportCSVWrapper.getType()));
        report.setReportOrientation(ReportOrientation.valueOf(
                reportCSVWrapper.getOrientation()
        ));

        // if company code is passed in, create customized report
        // for the company only
        if (StringUtils.isNotBlank(reportCSVWrapper.getCompany())) {

            Company company = layoutServiceRestemplateClient.getCompanyByCode(
                    reportCSVWrapper.getCompany()
            );

            if (Objects.nonNull(company)) {
                report.setCompanyId(company.getId());
                if (StringUtils.isNotBlank(reportCSVWrapper.getWarehouse())) {
                    // warehouse name is passed in, setup the warehouse as well

                    Warehouse warehouse =
                            layoutServiceRestemplateClient.getWarehouseByName(
                                    reportCSVWrapper.getCompany(),
                                    reportCSVWrapper.getWarehouse());
                    if (Objects.nonNull(warehouse)) {
                        report.setWarehouseId(warehouse.getId());
                    }
                }
            }
        }

        logger.debug("====   report  ====");
        return report;


    }


    public ReportHistory generateReport(Long warehouseId,
                                 String name,
                                 Report reportData,
                                 String locale)
            throws IOException, JRException {
        Warehouse warehouse
                = layoutServiceRestemplateClient.getWarehouseById(warehouseId);
        if (Objects.nonNull(warehouse)) {
            return generateReport(
                    warehouse.getCompany().getId(),
                    warehouseId,
                    name,
                    reportData,
                    locale
            );
        }
        else {
            return null;
        }

    }
    public ReportHistory generateReport(Long companyId,
                                 Long warehouseId,
                                 String name,
                                 Report reportData,
                                 String locale)
            throws IOException, JRException {

        Report reportMetaData = findByName(companyId, warehouseId, name);

        Locale reportLocale = Locale.forLanguageTag(locale);
        if (Objects.isNull(reportLocale)) {
            // default to us english
            // full language list
            // https://github.com/libyal/libfwnt/wiki/Language-Code-identifiers
            reportLocale = Locale.US;
        }
        logger.debug("Locale passed in: {}, will use locale: {}",
                locale, reportLocale.getDisplayName());
        reportData.addParameter(JRParameter.REPORT_LOCALE, reportLocale);


        logger.debug("Find report meta data by company: {}, warehouse: {}, name: {}",
                companyId, warehouseId, name);
        logger.debug(reportMetaData.toString());

        logger.debug("Start to get report file");
        // Note: Since this project is based off spring boot,
        // it is not possible to read any file. But we are able
        // to get inputStream and pass the stream into the
        // JasperCompileManager
        String reportUrl = getReportUrl(reportMetaData);
        InputStream reportStream = getReportStream(reportUrl);
        // File reportFile = getReportFile(reportMetaData);

        logger.debug("Report file stream returned!");

        JasperReport jasperReport =
                JasperCompileManager.compileReport(reportStream);

        logger.debug("Report file compiled!");


        JRBeanCollectionDataSource dataSource
                = new JRBeanCollectionDataSource(reportData.getData());


        logger.debug("Report datasource loaded!");

        reportData.getParameters().put(
                JRParameter.REPORT_LOCALE, reportLocale);

        JasperPrint jasperPrint = JasperFillManager.fillReport(
                jasperReport, reportData.getParameters(), dataSource
        );

        logger.debug("Report filled!");

        // save the result to local file
        String reportFileName =
                getReportResultFileName(companyId, reportMetaData);
        String reportResultAbsoluteFileName =
                REPORT_RESULT_FOLDER + "/"
                        + reportFileName;

        logger.debug("start to write report into {}!",
                reportResultAbsoluteFileName);

        switch (reportMetaData.getType()) {
            /**
            case HTML:
                JasperExportManager.exportReportToHtmlFile(
                        jasperPrint, reportResultAbsoluteFileName
                );
                break;
             **/
            default:
                JasperExportManager.exportReportToPdfFile(
                        jasperPrint, reportResultAbsoluteFileName
                );
                break;
        }


        // save the history information
        return saveReportHistory(reportMetaData, reportFileName, warehouseId);

    }

    private ReportHistory saveReportHistory(
            Report reportMetaData, String reportFileName, Long warehouseId) {

        return reportHistoryService.saveReportHistory(
                    reportMetaData, reportFileName, warehouseId
                    );
        // logger.debug("Report History saved: {}", reportHistory);
    }

    private String getReportResultFileName(Long companyId, Report report)
            throws IOException {

        String reportResultFilePostfix =
                String.format("%04d", (int)(Math.random()*1000));

        return report.getName() + "_" + System.currentTimeMillis() + "_" + reportResultFilePostfix
                + "." + report.getType();

    }



    public InputStream getReportStream(String url) throws IOException {

        return new ClassPathResource(url).getInputStream();

    }

    private String getReportUrl(Report report) {

        String folder = getReportFolder(report);

        String url = folder + "\\" + report.getFileName();

        logger.debug("will try to find report by url: {}", url);
        return url;

    }

    private String getReportFolder(Report report) {

        String folder = REPORT_CODE_FOLDER;

        if (Objects.nonNull(report.getCompanyId())) {
            folder += "\\" + report.getCompanyId();
        }
        if (Objects.nonNull(report.getWarehouseId())) {
            folder += "\\" + report.getWarehouseId();
        }

        return folder;
    }


    public Report changeReport(Report report) {

        return saveOrUpdate(report);
    }

    public boolean verifyReportAccess(Long companyId, Report report) {
        User currentUser = userService.getCurrentUser(companyId);
        if (Objects.isNull(currentUser)) {
            // THe user has to log in in order to
            // access the report
            return false;
        }
        if (Objects.nonNull(report.getCompanyId()) &&
               !companyId.equals(report.getCompanyId())) {
            // the report belongs to another company
            return false;
        }
        if (Objects.nonNull(report.getWarehouseId())) {
            Warehouse warehouse = layoutServiceRestemplateClient.getWarehouseById(
                    report.getWarehouseId()
            );
            //TODO:
            // check whether the user has access to the warehouse
        }
        // TODO: Check whether the user has access to the report

        return true;
    }
}
