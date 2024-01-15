package com.garyzhangscm.cwms.resources.service;

import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import com.garyzhangscm.cwms.resources.clients.LayoutServiceRestemplateClient;
import com.garyzhangscm.cwms.resources.exception.ReportAccessPermissionException;
import com.garyzhangscm.cwms.resources.exception.ReportFileMissingException;
import com.garyzhangscm.cwms.resources.exception.ResourceNotFoundException;
import com.garyzhangscm.cwms.resources.model.*;
import com.garyzhangscm.cwms.resources.repository.ReportRepository;
import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import net.sf.jasperreports.engine.util.JRLoader;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.persistence.criteria.*;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ReportService implements TestDataInitiableService{
    private static final Logger logger
            = LoggerFactory.getLogger(ReportService.class);


    @Value("${report.template.folder}")
    private String reportTemplateFolder;

    @Value("${report.customizedTemplate.folder}")
    private String reportCustomizedTemplateFolder;

    // WHen the user upload a customized report
    // template, we will save it  in a temporary folder
    // The file will be moved to the final folder
    // when the user save the customized template
    @Value("${report.template.tempFolder}")
    private String tempReportTemplateFolder;
    @Value("${report.result.folder}")
    private String reportResultFolder;


    @Autowired
    private ReportRepository reportRepository;
    @Autowired
    private ReportHistoryService reportHistoryService;
    @Autowired
    private LayoutServiceRestemplateClient layoutServiceRestemplateClient;

    @Autowired
    private PrinterService printerService;

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
                                String type,
                                String printerType) {
        return findAll(companyId, warehouseId, type, printerType,true);
    }
    public List<Report> findAll(Long companyId,
                                Long warehouseId,
                                String type,
                                String printerType,
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

                    if (!StringUtils.isBlank(type)) {
                        predicates.add(criteriaBuilder.equal(root.get("type"),
                                ReportType.valueOf(type)));
                    }

                    if (!StringUtils.isBlank(printerType)) {
                        Join<Report, PrinterType> joinPrinterType= root.join("reportType", JoinType.INNER);
                        predicates.add(criteriaBuilder.equal(joinPrinterType.get("name"), printerType));
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

    public List<Report> findAllStandardReports(

    ) {
        return findAllStandardReports(true);
    }

    public List<Report> findAllStandardReports(
            boolean includeDetails
    ) {
        List<Report> standardReports =
                reportRepository.findAllStandardReports();

        if (standardReports.size() > 0 && includeDetails) {
            loadDetail(standardReports);
        }
        return standardReports;

    }

    public Report findByType(Long companyId,
                             Long warehouseId,
                             ReportType type,
                             String printerName) {
        return  findByType(companyId, warehouseId, type, printerName, true);
    }
    public Report findByType(Long companyId,
                             Long warehouseId,
                             ReportType type,
                             String printerName,
                             boolean includeDetails) {
        PrinterType printerType = null;
        logger.debug("Start to find the right report by company id {}, warehouse id {}, report type {}, printer name {}",
                companyId, warehouseId, type, printerName);
        if(Strings.isNotBlank(printerName)) {
            Printer printer = printerService.findByName(warehouseId, printerName);
            logger.debug("We found printer by name {}? {}",
                    printerName, Objects.nonNull(printer) ? "Yes" : "N/A");
            if (Objects.nonNull(printer)) {
                logger.debug(">> the printer {}'s type is {}",
                        printer.getName(),
                        Objects.isNull(printer.getPrinterType()) ? "N/A" : printer.getPrinterType().getName());
                printerType = printer.getPrinterType();
            }
        }
        return findByType(companyId, warehouseId, type, printerType, includeDetails);
    }

    public Report findByType(Long companyId,
                             Long warehouseId,
                             ReportType type,
                             PrinterType printerType) {
        return findByType(companyId, warehouseId, type, printerType, true);
    }
    public Report findByType(Long companyId,
                             Long warehouseId,
                             ReportType type,
                             PrinterType printerType,
                             boolean includeDetails) {

        logger.debug("Start to find the right report by company id {}, warehouse id {}, report type {}, printer type {}",
                companyId, warehouseId, type,
                Objects.isNull(printerType) ? "N/A" : printerType.getName());

        // we will get all the report of certain type first
        // then we will filter out the report and get the most specific
        // report according to the parameters of company id, warehouse id
        // and printer name(from printer name, we can get the printer type,
        // with the printer type, we can get the specific report with the
        // type)
        List<Report> reports = findAll(null, null, type.name(), null, includeDetails);

        Report mostSpecificReport = getMostSpecificReport(reports, companyId, warehouseId,
                type, printerType);

        return mostSpecificReport;

    }

    private Report getMostSpecificReport(List<Report> reports,
                                         Long companyId,
                                         Long warehouseId,
                                         ReportType type,
                                         String printerName) {
        PrinterType printerType = null;
        if (Strings.isNotBlank(printerName)) {
            Printer printer = printerService.findByName(warehouseId, printerName);
            printerType = printer.getPrinterType();
        }
        return getMostSpecificReport(reports, companyId, warehouseId,
                type, printerType);

    }
    private Report getMostSpecificReport(List<Report> reports,
                                         Long companyId,
                                         Long warehouseId,
                                         ReportType type,
                                         PrinterType printerType) {

        logger.debug("Start to get most specific report by company id {}, warehouse id {}, report type {}, printer type {}, from {} reports",
                companyId, warehouseId, type,
                Objects.isNull(printerType) ? "N/A" : printerType.getName(),
                reports.size());

        List<Report> sortedReports = sortAndFilterReport(
                reports, companyId, warehouseId, type, printerType
        );
        if (sortedReports.isEmpty()) {
            return null;
        }
        return sortedReports.get(0);
    }

    /**
     * Sort the reports from most specific to most general based on the report's
     * attribute:
     * 1. printer type
     * 2. warehouse
     * 3. company
     * @param reports
     * @return
     */
    private List<Report> sortAndFilterReport(List<Report> reports,
                                             Long companyId,
                                             Long warehouseId,
                                             ReportType type,
                                             PrinterType printerType) {
        List<Report> filterReports = reports.stream().filter(
                report -> {
                    // not the right type
                    if (Objects.nonNull(type) && !type.equals(report.getType())) {
                        logger.debug("Current report {}'s type is {}, doesn't match with the required type {}",
                                report.getId(), report.getType(),
                                type);
                        return false;
                    }

                    // =========   Company   ====================
                    // if company ID is passed in, then only return the report
                    // with the specific company, or the one without company(global)
                    if (Objects.isNull(companyId) && Objects.nonNull(report.getCompanyId())) {
                        // we need a global default report but the current report is assigned
                        // to certain company
                        logger.debug("required company is null but the report belongs to company {}",
                                report.getCompanyId());
                        return false;
                    }
                    if (Objects.nonNull(companyId) && Objects.nonNull(report.getCompanyId())
                        && !companyId.equals(report.getCompanyId())) {
                        // we need a company specific report or a default report but the
                        // current report is assigned to another company
                        logger.debug("required company is {} but the report belongs to company {}",
                                companyId, report.getCompanyId());
                        return false;
                    }
                    // =========   Warehouse   ====================
                    // if warehouse ID is passed in, then only return the report
                    // with the specific warehouse, or the one without warehouse(global)
                    if (Objects.isNull(warehouseId) && Objects.nonNull(report.getWarehouseId())) {
                        // we need a global default report but the current report is assigned
                        // to certain warehouse
                        logger.debug("required warehouse is null but the report belongs to warehouse {}",
                                report.getWarehouseId());
                        return false;
                    }
                    if (Objects.nonNull(warehouseId) && Objects.nonNull(report.getWarehouseId())
                            && !warehouseId.equals(report.getWarehouseId())) {
                        // we need a warehouse specific report or a default report but the
                        // current report is assigned to another warehouse
                        logger.debug("required warehouse is {} but the report belongs to warehouse {}",
                                warehouseId, report.getWarehouseId());
                        return false;
                    }

                    // =========   Printer type   ====================
                    // if printer name is passed in, we will get the printer type from it
                    // if there's no printer type define for it, then we will only return the
                    // report that not specific to any printer type. Otherwise, we will only
                    // return the report that is defined for the specific printer
                    if (Objects.isNull(printerType) && Objects.nonNull(report.getPrinterType())) {
                        // we need a global default report but the current report is assigned
                        // to certain printer type
                        logger.debug("required printer name is null but the report belongs to printer type {}",
                                 report.getPrinterType());
                        return false;
                    }
                    if (Objects.nonNull(printerType) && Objects.nonNull(report.getPrinterType()) &&
                            !printerType.equals(report.getPrinterType())) {
                        logger.debug("required printer type is {} but the report belongs to printer type {}",
                                printerType.getName(), report.getPrinterType().getName());
                        return false;

                    }

                    logger.debug("The report with id {}, type {}, company {}, warehouse {}" +
                            ", printer type {} passed all the validation against the requirement:" +
                            " company id {}, warehouse id {}, type {}, printer type {}",
                            report.getId(), report.getType(),
                            Objects.isNull(report.getCompanyId()) ? "N/A" : report.getCompanyId(),
                            Objects.isNull(report.getWarehouseId()) ? "N/A" : report.getWarehouseId(),
                            Objects.isNull(report.getPrinterType()) ? "N/A" : report.getPrinterType(),
                            Objects.isNull(companyId) ? "N/A" : companyId,
                            Objects.isNull(warehouseId) ? "N/A" : warehouseId,
                            Objects.isNull(type) ? "N/A" : type,
                            Objects.isNull(printerType) ? "N/A" : printerType.getName());

                    return true;
                }
        ).collect(Collectors.toList());

        // let's sort from most specific to most general
        logger.debug("=======   Before sort  the report =======");
        logger.debug(filterReports.toString());
        filterReports.sort((report1, report2) -> {

            if (Objects.nonNull(report1.getPrinterType()) &&
                    Objects.isNull(report2.getPrinterType())) {
                return -1;
            }
            if (Objects.isNull(report1.getPrinterType()) &&
                    Objects.nonNull(report2.getPrinterType())) {
                return 1;
            }

            if (Objects.nonNull(report1.getWarehouseId()) &&
                    Objects.isNull(report2.getWarehouseId())) {
                return -1;
            }
            if (Objects.isNull(report1.getWarehouseId()) &&
                    Objects.nonNull(report2.getWarehouseId())) {
                return 1;
            }

            if (Objects.nonNull(report1.getCompanyId()) &&
                    Objects.isNull(report2.getCompanyId())) {
                return -1;
            }
            if (Objects.isNull(report1.getCompanyId()) &&
                    Objects.nonNull(report2.getCompanyId())) {
                return 1;
            }

            return 1;


        });

        logger.debug("=======   After sort  the report =======");
        logger.debug(filterReports.toString());

        return filterReports;
    }

    public  Report findByType(ReportType type) {
        return findByType(type, true);
    }
    /**
     * Get the standard version of the report
     * @param type
     * @param  includeDetails whether includes the detail information
     * @return
     */
    public  Report findByType(ReportType type, boolean includeDetails) {

        Report standardReport =  reportRepository.findStandardReportByType(type);

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
            Report matchedReport = findByType(report.getCompanyId(),
                    report.getWarehouseId(), report.getType(), report.getPrinterType(), false);
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
                addColumn("type").
                addColumn("description").
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

            InputStream inputStream =
                    new ClassPathResource(testDataFileName).getInputStream();
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
                                 ReportType type,
                                 Report reportData,
                                 String locale, String printerName)
            throws IOException, JRException {
        Warehouse warehouse
                = layoutServiceRestemplateClient.getWarehouseById(warehouseId);
        if (Objects.nonNull(warehouse)) {
            if (type.isLabel()) {
                // generate label(only support zebra
                return generateLabel(
                        warehouse.getCompany().getId(),
                        warehouseId,
                        type,
                        reportData,
                        locale, printerName);
            }
            else {

                // generate report
                return generateReport(
                        warehouse.getCompany().getId(),
                        warehouseId,
                        type,
                        reportData,
                        locale, printerName);
            }
        }
        else {
            return null;
        }

    }

    private ReportHistory generateLabel(Long companyId, Long warehouseId, ReportType type,
                                        Report reportData, String locale, String printerName) throws IOException {

        // Meta data without any content
        Report reportMetaData = findByType(companyId, warehouseId, type, printerName);

        if (Objects.isNull(reportMetaData)) {
            throw ReportFileMissingException.raiseException(
                    "Can't find report template for company /" + companyId +
                            ",  warehouse / " + warehouseId +
                            ", type / " + type);
        }


        logger.debug("Find report meta data by company: {}, warehouse: {}, type: {}, printerName: {}",
                companyId, warehouseId, type, printerName);
        logger.debug(reportMetaData.toString());


        logger.debug("Start to get label file");
        String labelTemplate = loadLabelFile(reportMetaData);

        // for labels, we will use either parameters or data to fill the label template
        // if we have data filled in, then we will use the data to print multiple lables
        // into one file, otherwise, we will use the paramters to print only one label
        String labelContent =  processLabel(labelTemplate, reportData);
        logger.debug("will generate label file by content \n {}", labelContent);

        // save the result to local file
        String reportResultFileName = writeResultFile(companyId, warehouseId, reportMetaData, labelContent);


        // save the history information
        return saveReportHistory(reportMetaData, reportResultFileName, companyId, warehouseId);
    }
    /**
     * Generate label from the template
     * if we have data, then we will generate multiple labels from the data, otherwise,
     * we will generate one label from the parameter
     * @param labelTemplate
     * @param report
     * @return
     */
    private String processLabel(String labelTemplate, Report report) {
         if (Objects.isNull(report.getData()) ||
                 report.getData().size() == 0) {
             logger.debug("Will get the label content out of parameters");
             return processLabel(labelTemplate, report.getParameters());
         }

        logger.debug("Will get the label content out of data");
         return report.getData()
                 .stream()
                 .map(labelParameters -> processLabel(labelTemplate, (Map<String, Object>)labelParameters))
                 .collect(Collectors.joining());

    }


    /**
     * Generate label from the template and the parameters
     * @param labelTemplate
     * @param parameters
     * @return
     */
    private String processLabel(String labelTemplate, Map<String, Object> parameters) {
        String labelContent = labelTemplate;
        for(Map.Entry<String, Object> parameter : parameters.entrySet()) {
            String parameterName = parameter.getKey();
            String value = Objects.isNull(parameter.getValue()) ?
                "" : parameter.getValue().toString();

            // see if we have the parameters in the template
            logger.debug("start to replace variable {}, with value {}",
                    parameterName, value);
            logger.debug("label template before replace: {}", labelContent);
            labelContent = labelContent.replaceAll("\\$" + parameterName + "\\$", value);
            logger.debug("label template after replace: {}", labelContent);
        }
        // for any place holder in the label file that has no value
        // passed in , let's just clear the place holder($parameter_name$)
        labelContent = processEmptyValueForLabel(labelContent);

        logger.debug("label template after clear all place holder: {}", labelContent);
        return labelContent;

    }

    /**
     * Remove all place holder that has no value passed in , in the
     * label file
     * @param labelContent
     * @return
     */
    private String processEmptyValueForLabel(String labelContent) {
        return labelContent.replaceAll("\\$\\b\\S+?\\b\\$", "");
    }


    private String loadLabelFile(Report report) throws IOException {

        String folder = getReportFolder(report);
        String fileName = report.getFileName();
        String fullFilePath = folder + "/" + fileName;
        // check if the file exists
        logger.debug("Load label template from {}", fullFilePath);

        Path path = Path.of(fullFilePath);

        return Files.readString(path);

    }

    public ReportHistory generateReport(Long companyId,
                                         Long warehouseId,
                                         ReportType type,
                                         Report reportData,
                                         String locale,
                                        String printerName)
            throws IOException, JRException {

        // Meta data without any content
        Report reportMetaData = findByType(companyId, warehouseId, type, printerName);

        if (Objects.isNull(reportMetaData)) {
            throw ReportFileMissingException.raiseException(
                    "Can't find report template for company /" + companyId +
                    ",  warehouse / " + warehouseId +
                    ", type / " + type);
        }
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


        logger.debug("Find report meta data by company: {}, warehouse: {}, type: {}",
                companyId, warehouseId, type);
        logger.debug(reportMetaData.toString());

        logger.debug("Start to get report file");
        // Note: Since this project is based off spring boot,
        // it is not possible to read any file. But we are able
        // to get inputStream and pass the stream into the
        // JasperCompileManager
        JasperReport jasperReport = getJasperReport(reportMetaData);


        JRBeanCollectionDataSource dataSource
                = new JRBeanCollectionDataSource(reportData.getData());


        logger.debug("Report datasource loaded!");

        reportData.getParameters().put(
                JRParameter.REPORT_LOCALE, reportLocale);

        logger.debug("####   Report   Data  ######");
        logger.debug(reportData.toString());

        // get custmoized resource boundle
        // it should be in the same folder as the report template folder

        String reportBundleUrl = getReportBundleUrl(reportMetaData);
        logger.debug("Start to get resource bundle with base name {}",
                reportBundleUrl);
        ResourceBundle resourceBundle = ResourceBundle.getBundle(reportBundleUrl,
                reportLocale);
        logger.debug("=====  resource bundle loaded, key count:{}  ====",
                resourceBundle.keySet().size());
        for(String key: resourceBundle.keySet()) {
            logger.debug(">> key: {}, value: {}",
                    key, resourceBundle.getString(key));
        }
        reportData.addParameter("REPORT_RESOURCE_BUNDLE", resourceBundle);


        JasperPrint jasperPrint = JasperFillManager.fillReport(
                jasperReport, reportData.getParameters(), dataSource
        );

        logger.debug("Report filled!");

        // save the result to local file
        String reportResultFileName = writeResultFile(companyId, warehouseId, reportMetaData, jasperPrint);


        // save the history information
        return saveReportHistory(reportMetaData, reportResultFileName, companyId, warehouseId);

    }

    private JasperReport getJasperReport(Report reportMetaData)
            throws IOException, JRException {


        // see if a pre-compiled(.jasper) file exists
        JasperReport jasperReport;
        String reportUrl = getReportUrl(reportMetaData, "jasper");
        File reportFile = new File(reportUrl);
        if (reportFile.exists()) {
            // load from the .jasper file
            logger.debug("Load from jasper file: {}", reportUrl);
            jasperReport  = (JasperReport)JRLoader.loadObject(reportFile);
            return jasperReport;
        }

        // if .jasper file doesn't exist, we will need to compile the jrxml file
        reportUrl = getReportUrl(reportMetaData, "jrxml");
        reportFile = new File(reportUrl);
        if (reportFile.exists()) {
            // load from the .jasper file

            logger.debug("Load from jrxml file: {}", reportUrl);
            InputStream reportStream = getReportStream(reportUrl);


            logger.debug("Report file stream returned!");

            // if the file is a pre-compiled,
            jasperReport =
                    JasperCompileManager.compileReport(reportStream);

            logger.debug("Report file compiled!");
            return jasperReport;
        }
        else {
            throw ReportFileMissingException.raiseException(
                    "Can't find the report file for " + reportMetaData.getFileName());
        }


    }

    /**
     * Generate the report result (normally PDF) and save it locally. So we can print
     * or view the file later now
     * @param reportMetaData report meta data, we will get the file name from here
     * @param jasperPrint Jasper print object that contains the report's content
     * @return
     * @throws JRException
     */
    private String writeResultFile(Long companyId, Long warehouseId, Report reportMetaData, JasperPrint jasperPrint)
            throws JRException {
        String reportFileName =
                getReportResultFileName(reportMetaData);
        String reportResultAbsoluteFileName =
                getReportResultFolder(companyId, warehouseId)
                        + reportFileName;

        File reportResultFile = new File(reportResultAbsoluteFileName);

        // remove the file if it already exists
        reportResultFile.deleteOnExit();
        if (!reportResultFile.getParentFile().exists()) {
            reportResultFile.getParentFile().mkdirs();
        }

        logger.debug("start to write report into {} !",
                reportResultAbsoluteFileName);


        // Now we only support printing to PDF file
        JasperExportManager.exportReportToPdfFile(
                jasperPrint, reportResultAbsoluteFileName
        );

        return reportFileName;

    }

    /**
     * Write result file for label. Now we only support zebra printer so the
     * label content will be in the format of zpl
     * @param reportMetaData label meta data, we will use this determine the file name
     * @param labelContent label's content
     * @return
     */
    private String writeResultFile(Long companyId, Long warehouseId, Report reportMetaData, String labelContent) throws IOException {
        String reportFileName =
                getReportResultFileName(reportMetaData);
        String reportResultAbsoluteFileName =
                getReportResultFolder(companyId, warehouseId)
                        + reportFileName;

        return writeResultFile( reportFileName,
                reportResultAbsoluteFileName, labelContent);

    }

    public String writeResultFile(String reportFileName,
                                   String reportResultAbsoluteFileName, String labelContent) throws IOException {


        File reportResultFile = new File(reportResultAbsoluteFileName);

        // remove the file if it already exists
        reportResultFile.deleteOnExit();
        if (!reportResultFile.getParentFile().exists()) {
            reportResultFile.getParentFile().mkdirs();
        }

        logger.debug("start to write label into {} !",
                reportResultAbsoluteFileName);


        BufferedWriter writer = new BufferedWriter(new FileWriter(reportResultAbsoluteFileName));
        writer.write(labelContent);

        writer.close();

        return reportFileName;

    }
    private String getReportResultFolder(Long companyId, Long warehouseId) {

        String folder = reportResultFolder;
        if (!folder.endsWith("/")) {
            folder += "/";
        }

        folder += companyId + "/" + warehouseId + "/";

        return folder;
    }

    private String getReportBundleUrl(Report report) {

        String folder = "";

        if (Objects.nonNull(report.getCompanyId())) {
            folder += report.getCompanyId() + "/";
        }
        if (Objects.nonNull(report.getWarehouseId())) {
            folder += report.getWarehouseId() + "/";
        }


        String url = folder  + report.getFileName().replaceFirst("[.][^.]+$", "");;

        logger.debug("will try to find report bundle by url: {}", url);
        return url;


    }

    private ReportHistory saveReportHistory(
            Report reportMetaData, String reportFileName, Long companyId, Long warehouseId) {

        return reportHistoryService.saveReportHistory(
                    reportMetaData, reportFileName, companyId, warehouseId
                    );
        // logger.debug("Report History saved: {}", reportHistory);
    }

    private String getReportResultFileName(Report report) {

        String reportResultFilePostfix =
                String.format("%04d", (int)(Math.random()*1000));

        // for label, we will save the result into lbl file
        // otherwise, we will save the result into pdf file
        if (report.getType().isLabel()) {

            return report.getType() + "_" + System.currentTimeMillis() + "_" + reportResultFilePostfix
                    + ".lbl";
        }
        else {

            return report.getType() + "_" + System.currentTimeMillis() + "_" + reportResultFilePostfix
                    + ".PDF";
        }

    }



    public InputStream getReportStream(String url) throws IOException {

        return new FileInputStream(url);

    }

    private String getReportUrl(Report report, String fileExtension) {

        String folder = getReportFolder(report);

        String url = folder + "/"
                + report.getFileName() + "." + fileExtension;

        logger.debug("will try to find report by url: {}", url);
        return url;

    }



    private String getReportFolder(Report report) {

        String folder = reportTemplateFolder;

        if (Objects.nonNull(report.getCompanyId())) {
            folder += "/" + report.getCompanyId();
        }
        if (Objects.nonNull(report.getWarehouseId())) {
            folder += "/" + report.getWarehouseId();
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

    public String uploadReportTemplate(
            Long warehouseId, MultipartFile file) throws IOException {


        String filePath = getTemporaryReportTemplateFilePath(warehouseId);
        logger.debug("Save file to {}{}",
                filePath, file.getOriginalFilename());

        File savedFile =
                fileService.saveFile(
                        file, filePath, file.getOriginalFilename());

        logger.debug("File saved, path: {}",
                savedFile.getAbsolutePath());
        return file.getOriginalFilename();
    }

    private String getTemporaryReportTemplateFilePath(Long warehouseId) {

        String username = userService.getCurrentUserName();
        return getTemporaryReportTemplateFilePath(warehouseId, username);
    }

    private String getTemporaryReportTemplateFilePath(Long warehouseId, String username) {


        if (!tempReportTemplateFolder.endsWith("/")) {
            return tempReportTemplateFolder + "/" + warehouseId + "/" + username + "/";
        }
        else  {

            return tempReportTemplateFolder + warehouseId + "/" + username + "/";
        }
    }

    public File getTemporaryReportTemplate(Long warehouseId, String username, String fileName) {

        String fileUrl = getTemporaryReportTemplateFilePath(warehouseId, username) + fileName;

        logger.debug("Will return {} to the client",
                fileUrl);
        return new File(fileUrl);
    }

    public File getReportTemplate(Long companyId, Long warehouseId, String fileName) {

        String fileUrl = reportTemplateFolder;
        if (Objects.nonNull(companyId)) {
            fileUrl += "/" + companyId;
        }
        if (Objects.nonNull(warehouseId)) {
            fileUrl += "/" + warehouseId;
        }

        // file name needs to be either ends with jrxml / jasper for report
        // or prn for label
        fileUrl += "/" + fileName;
        if (fileUrl.endsWith(".prn") || fileUrl.endsWith(".jrxml") ||
            fileUrl.endsWith("jasper")) {

            logger.debug("Will return {} to the client",
                    fileUrl);
            return new File(fileUrl);
        }
        else if (Files.exists(Path.of(fileUrl + ".prn"))) {

            return new File(fileUrl + ".prn");
        }
        else if (Files.exists(Path.of(fileUrl + ".jrxml"))) {

            return new File(fileUrl + ".jrxml");
        }
        else if (Files.exists(Path.of(fileUrl + ".jasper"))) {

            return new File(fileUrl + ".jasper");
        }
        return new File(fileUrl);
    }

    public Report addReport(Long warehouseId, String username, Boolean companySpecific,
                            Boolean warehouseSpecific, Report report) throws IOException {
        copyUploadedTemplateFile(warehouseId, username, companySpecific,
                                 warehouseSpecific, report);
        copyUploadedPropertiesFiles(warehouseId, username, companySpecific,
                warehouseSpecific, report);


        if (companySpecific || warehouseSpecific) {
            copyUploadedCustomizedTemplateFile(warehouseId, username, companySpecific,
                    warehouseSpecific, report);
            copyUploadedCustomizedPropertiesFiles(warehouseId, username, companySpecific,
                    warehouseSpecific, report);
        }

        // clear the company id and warehouse id if
        // the report is not specific to any company or warehouse
        if (!companySpecific) {
            report.setCompanyId(null);
        }
        if (!warehouseSpecific) {
            report.setWarehouseId(null);
        }

        // remove the postfix of the file
        if (report.getFileName().endsWith(".jrxml")) {
            report.setFileName(
                    report.getFileName().substring(0, report.getFileName().length() - 6)
            );
        }



        return saveOrUpdate(report);

    }

    private void copyUploadedPropertiesFiles(Long warehouseId, String username,
                                             Boolean companySpecific, Boolean warehouseSpecific, Report report) throws IOException {

        // we will always assume that the properties files has the same name
        // as the template file

        logger.debug("Start to copy properties file");
        String filenameWithoutExtension = report.getFileName().replaceFirst("[.][^.]+$", "");

        // get all properties files that start with same name and ends with .properties
        File dir = new File(getTemporaryReportTemplateFilePath(warehouseId, username));


        FileFilter fileFilter = new WildcardFileFilter(filenameWithoutExtension + "*.properties");
        File[] files = dir.listFiles(fileFilter);
        logger.debug("get {} properties files under folder {}",
                files.length, dir.getAbsolutePath());
        for (int i = 0; i < files.length; i++) {
            String sourcePropertiesFilePath = getTemporaryReportTemplateFilePath(warehouseId, username)
                    + files[i].getName();

            String destinationPropertiesFilePath = getReportTemplateFile(companySpecific, warehouseSpecific, report)
                    + files[i].getName();
            logger.debug("Copy template file from {} to {}", sourcePropertiesFilePath, destinationPropertiesFilePath);
            fileService.copyFile(sourcePropertiesFilePath, destinationPropertiesFilePath);

            // remote the original file
            // new File(sourcePropertiesFilePath).deleteOnExit();
        }

    }

    private void copyUploadedCustomizedPropertiesFiles(Long warehouseId, String username,
                                             Boolean companySpecific, Boolean warehouseSpecific, Report report) throws IOException {

        // we will always assume that the properties files has the same name
        // as the template file

        logger.debug("Start to copy properties file");
        String filenameWithoutExtension = report.getFileName().replaceFirst("[.][^.]+$", "");

        // get all properties files that start with same name and ends with .properties
        File dir = new File(getTemporaryReportTemplateFilePath(warehouseId, username));


        FileFilter fileFilter = new WildcardFileFilter(filenameWithoutExtension + "*.properties");
        File[] files = dir.listFiles(fileFilter);
        logger.debug("get {} properties files under folder {}",
                files.length, dir.getAbsolutePath());
        for (int i = 0; i < files.length; i++) {
            String sourcePropertiesFilePath = getTemporaryReportTemplateFilePath(warehouseId, username)
                    + files[i].getName();

            String destinationPropertiesFilePath = getReportCustomizedTemplateFile(companySpecific, warehouseSpecific, report)
                    + files[i].getName();
            logger.debug("Copy template file from {} to {}", sourcePropertiesFilePath, destinationPropertiesFilePath);
            fileService.copyFile(sourcePropertiesFilePath, destinationPropertiesFilePath);

            // remote the original file
            // new File(sourcePropertiesFilePath).deleteOnExit();
        }

    }

    private void copyUploadedTemplateFile(Long warehouseId, String username,
                                          Boolean companySpecific, Boolean warehouseSpecific, Report report) throws IOException {
        logger.debug("Start to copy template file");
        // Copy the file from temporary folder into template folder
        String sourceTemplateFilePath = getTemporaryReportTemplateFilePath(warehouseId, username)
                + report.getFileName();

        String destinationTemplateFilePath = getReportTemplateFile(companySpecific, warehouseSpecific, report)
                + report.getFileName();

        logger.debug("Copy template file from {} to {}", sourceTemplateFilePath, destinationTemplateFilePath);

        fileService.copyFile(sourceTemplateFilePath, destinationTemplateFilePath);

        // remote the original file
        // new File(sourceTemplateFilePath).deleteOnExit();

    }
    private void copyUploadedCustomizedTemplateFile(Long warehouseId, String username,
                                          Boolean companySpecific, Boolean warehouseSpecific, Report report) throws IOException {
        logger.debug("Start to copy template file");
        // Copy the file from temporary folder into template folder
        String sourceTemplateFilePath = getTemporaryReportTemplateFilePath(warehouseId, username)
                + report.getFileName();

        String destinationTemplateFilePath = getReportCustomizedTemplateFile(companySpecific, warehouseSpecific, report)
                + report.getFileName();

        logger.debug("Copy template file from {} to {}", sourceTemplateFilePath, destinationTemplateFilePath);

        fileService.copyFile(sourceTemplateFilePath, destinationTemplateFilePath);

        // remote the original file
        // new File(sourceTemplateFilePath).deleteOnExit();

    }

    /**
     * The template file will be save in the reportTemplateFolder. If the report template is
     * customized for certain company / warehouse, then it will be saved in the sub-folder
     * identified by company id and/or warehouse id
     * so
     * - standard report: reportTemplateFolder
     * - company customized report: reportTemplateFolder / companyId
     * - warehouse customized report: reportTemplateFolder / companyId / warehouseId
     * @param companySpecific
     * @param warehouseSpecific
     * @param report
     * @return
     */
    private String getReportTemplateFile(Boolean companySpecific, Boolean warehouseSpecific, Report report) {

        String filepath;
        if (!reportTemplateFolder.endsWith("/")) {
            filepath = reportTemplateFolder + "/";
        }
        else  {

            filepath = reportTemplateFolder;
        }
        if (companySpecific || warehouseSpecific) {
            filepath += report.getCompanyId() + "/";
        }
        if (warehouseSpecific) {
            filepath += report.getWarehouseId() + "/";
        }

        return filepath;
    }
    private String getReportCustomizedTemplateFile(Boolean companySpecific, Boolean warehouseSpecific, Report report) {

        String filepath;
        if (!reportCustomizedTemplateFolder.endsWith("/")) {
            filepath = reportCustomizedTemplateFolder + "/";
        }
        else  {

            filepath = reportCustomizedTemplateFolder;
        }
        if (companySpecific || warehouseSpecific) {
            filepath += report.getCompanyId() + "/";
        }
        if (warehouseSpecific) {
            filepath += report.getWarehouseId() + "/";
        }

        return filepath;
    }

    public void delete(Long id) {
        reportRepository.deleteById(id);
    }


    public void removeReport(Long id) {
        // we will not allow the user to remove
        // the default report
        Report report = findById(id);
        if (Objects.isNull(report.getCompanyId()) &&
                Objects.isNull(report.getWarehouseId())) {
            throw ReportAccessPermissionException.raiseException("Can't remove standard report, please override with your own report template");
        }

        delete(id);
    }
}
