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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
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
                                String type) {
        return findAll(companyId, warehouseId, type, true);
    }
    public List<Report> findAll(Long companyId,
                                Long warehouseId,
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
                             ReportType type) {
        return findByType(companyId, warehouseId, type, true);
    }
    public Report findByType(Long companyId,
                             Long warehouseId,
                             ReportType type,
                             boolean includeDetails) {
        // check if we already have a customized report
        // 1. warehouse customized report
        // 2. company custmoized report
        if (Objects.nonNull(warehouseId)) {

            Report warehouseReport = reportRepository.findByWarehouseIdAndType(
                    warehouseId, type
            );
            if (Objects.nonNull(warehouseReport)) {
                if (includeDetails) {
                    loadDetail(warehouseReport);
                }
                return warehouseReport;
            }
        }
        if (Objects.nonNull(companyId)) {
            Report companyReport = reportRepository.findByCompanyIdAndType(
                    companyId, type
            );
            if (Objects.nonNull(companyReport)) {
                if (includeDetails) {
                    loadDetail(companyReport);
                }
                return companyReport;
            }
        }

        // we don't have any customized version, let's
        // return the standard version
        Report standardReport =  findByType(type);
        if (Objects.nonNull(standardReport) && includeDetails) {
            loadDetail(standardReport);
        }
        return standardReport;

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
                    report.getWarehouseId(), report.getType());
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
                                 String locale)
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
                        locale);
            }
            else {

                // generate report
                return generateReport(
                        warehouse.getCompany().getId(),
                        warehouseId,
                        type,
                        reportData,
                        locale);
            }
        }
        else {
            return null;
        }

    }

    private ReportHistory generateLabel(Long companyId, Long warehouseId, ReportType type,
                                        Report reportData, String locale) throws IOException {

        // Meta data without any content
        Report reportMetaData = findByType(companyId, warehouseId, type);

        if (Objects.isNull(reportMetaData)) {
            throw ReportFileMissingException.raiseException(
                    "Can't find report template for company /" + companyId +
                            ",  warehouse / " + warehouseId +
                            ", type / " + type);
        }


        logger.debug("Find report meta data by company: {}, warehouse: {}, type: {}",
                companyId, warehouseId, type);
        logger.debug(reportMetaData.toString());


        logger.debug("Start to get label file");
        String labelTemplate = loadLabelFile(reportMetaData);

        // for labels, we will use either parameters or data to fill the label template
        // if we have data filled in, then we will use the data to print multiple lables
        // into one file, otherwise, we will use the paramters to print only one label
        String labelContent =  processLabel(labelTemplate, reportData);
        logger.debug("will generate label file by content \n {}", labelContent);

        // save the result to local file
        String reportResultFileName = writeResultFile(reportMetaData, labelContent);


        // save the history information
        return saveReportHistory(reportMetaData, reportResultFileName, warehouseId);
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
         if (report.getData().size() == 0) {
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
            String value = parameter.getValue().toString();

            // see if we have the parameters in the template
            logger.debug("start to replace variable {}, with value {}",
                    parameterName, value);
            logger.debug("label template before replace: {}", labelContent);
            labelContent = labelContent.replaceAll("\\$" + parameterName + "\\$", value);
            logger.debug("label template after replace: {}", labelContent);
        }
        return labelContent;

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
                                 String locale)
            throws IOException, JRException {

        // Meta data without any content
        Report reportMetaData = findByType(companyId, warehouseId, type);

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
        String reportResultFileName = writeResultFile(reportMetaData, jasperPrint);


        // save the history information
        return saveReportHistory(reportMetaData, reportResultFileName, warehouseId);

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
    private String writeResultFile(Report reportMetaData, JasperPrint jasperPrint)
            throws JRException {
        String reportFileName =
                getReportResultFileName(reportMetaData);
        String reportResultAbsoluteFileName =
                getReportResultFolder(reportMetaData)
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
    private String writeResultFile(Report reportMetaData, String labelContent) throws IOException {
        String reportFileName =
                getReportResultFileName(reportMetaData);
        String reportResultAbsoluteFileName =
                getReportResultFolder(reportMetaData)
                        + reportFileName;

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
    private String getReportResultFolder(Report report) {

        String folder = reportResultFolder;
        if (!folder.endsWith("/")) {
            folder += "/";
        }

        if (Objects.nonNull(report.getCompanyId())) {
            folder += report.getCompanyId() + "/";
        }
        if (Objects.nonNull(report.getWarehouseId())) {
            folder += report.getWarehouseId() + "/";
        }

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
            Report reportMetaData, String reportFileName, Long warehouseId) {

        return reportHistoryService.saveReportHistory(
                    reportMetaData, reportFileName, warehouseId
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
        fileUrl += "/" + fileName;

        logger.debug("Will return {} to the client",
                fileUrl);
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
