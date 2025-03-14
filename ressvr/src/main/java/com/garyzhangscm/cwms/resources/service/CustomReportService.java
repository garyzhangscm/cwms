/**
 * Copyright 2019
 *
 * @author gzhang
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.garyzhangscm.cwms.resources.service;

import com.garyzhangscm.cwms.resources.exception.MissingInformationException;
import com.garyzhangscm.cwms.resources.exception.ResourceNotFoundException;
import com.garyzhangscm.cwms.resources.model.*;
import com.garyzhangscm.cwms.resources.repository.CustomReportRepository;
import org.apache.commons.lang3.tuple.Triple;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.stereotype.Service;

import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.transaction.Transactional;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class CustomReportService {
    private static final Logger logger = LoggerFactory.getLogger(CustomReportService.class);
    @Autowired
    private CustomReportRepository customReportRepository;

    @Autowired
    private NamedParameterJdbcTemplate jdbcTemplate;

    @Autowired
    private CustomReportExecutionHistoryService customReportExecutionHistoryService;

    private Map<Long, CustomReportExecutionHistory> inProcessCustomReport = new ConcurrentHashMap<>();

    @Autowired
    private EntityManager entityManager;


    @Value("${customReport.result.folder:}")
    private String customReportResultFolder;


    public CustomReport findById(Long id) {
        return customReportRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.raiseException("Customer Report not found by id: " + id));
    }


    public List<CustomReport> findAll(Long companyId,
                                      Long warehouseId,
                                      String name) {

        return customReportRepository.findAll(
                (Root<CustomReport> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) -> {
                    List<Predicate> predicates = new ArrayList<Predicate>();

                    predicates.add(criteriaBuilder.equal(root.get("companyId"), companyId));
                    if (Objects.nonNull(warehouseId)) {
                        predicates.add(criteriaBuilder.equal(root.get("warehouseId"), warehouseId));

                    }
                    if (Strings.isNotBlank(name)) {
                        predicates.add(criteriaBuilder.equal(root.get("name"), name));

                    }


                    Predicate[] p = new Predicate[predicates.size()];
                    return criteriaBuilder.and(predicates.toArray(p));
                },
                Sort.by(Sort.Direction.DESC, "createdTime")

        );
    }

    public CustomReport findByName(Long companyId,
                                   Long warehouseId,
                                   String name) {

        return customReportRepository.findByCompanyIdAndWarehouseIdAndName(
                companyId, warehouseId, name
        );
    }

    public CustomReport save(CustomReport customReport) {
        return customReportRepository.save(customReport);
    }

    public CustomReport saveOrUpdate(CustomReport customReport) {

        if (Objects.isNull(customReport.getId()) &&
            Objects.nonNull(findByName(
                    customReport.getCompanyId(), customReport.getWarehouseId(),
                    customReport.getName()
            ))) {
            customReport.setId(
                    findByName(
                            customReport.getCompanyId(),
                            customReport.getWarehouseId(),
                            customReport.getName()
                    ).getId()
            );
        }
        return save(customReport);
    }


    public CustomReport addCustomReport(CustomReport customReport) {
        for (CustomReportParameter customReportParameter : customReport.getCustomReportParameters()) {
            customReportParameter.setCustomReport(customReport);
        }
        customReport.setQuery(
                customReport.getQuery().replace("\n", "  ")
        );

        logger.debug("start to add custom report \n{}", customReport);
        return saveOrUpdate(customReport);

    }

    public CustomReport changeCustomReport(Long id, CustomReport customReport) {
        customReport.setId(id);
        for (CustomReportParameter customReportParameter : customReport.getCustomReportParameters()) {
            customReportParameter.setCustomReport(customReport);
        }
        customReport.setQuery(
                customReport.getQuery().replace("\n", "  ")
        );
        return saveOrUpdate(customReport);

    }

    public void delete(Long id) {
        customReportRepository.deleteById(id);

    }

    /***
     * get the parameterized query and the actual query for the custom report
     * @param companyId
     * @param warehouseId
     * @param customReport
     * @return
     */
    public Triple<String, String, Map<String, String>> getQuery(Long companyId,
                                                Long warehouseId,
                                                CustomReport customReport) {


        Map<String, String> paramMap = new HashMap<>();
        // we will run the query string
        // and show the actual query string for tracibility
        StringBuilder queryString = new StringBuilder()
                .append(customReport.getQuery()).append(" where 1 = 1");

        StringBuilder actualQueryString = new StringBuilder()
                .append(customReport.getQuery()).append(" where 1 = 1");

        if (Strings.isNotBlank(customReport.getCompanyIdFieldName())) {
            queryString.append(" and ").append(customReport.getCompanyIdFieldName())
                    .append(" = ").append(companyId);
            paramMap.put(customReport.getCompanyIdFieldName(), companyId.toString());
            actualQueryString.append(" and ").append(customReport.getCompanyIdFieldName())
                    .append(" = ").append(companyId);
        }

        if (Boolean.TRUE.equals(customReport.getRunAtCompanyLevel())) {
            // raise error message if the custom is setup to run at the company level but
            // there's no company id field name setup
            if (Strings.isBlank(customReport.getCompanyIdFieldName())) {
                throw MissingInformationException.raiseException("the report is setup to run at the " +
                        " company level but there's no company ID field configured");
            }
        }
        else if (Strings.isNotBlank(customReport.getWarehouseIdFieldName())) {

            queryString.append(" and ").append(customReport.getWarehouseIdFieldName())
                    .append(" = ").append(warehouseId);
            paramMap.put(customReport.getWarehouseIdFieldName(), warehouseId.toString());
            actualQueryString.append(" and ").append(customReport.getWarehouseIdFieldName())
                    .append(" = ").append(warehouseId);
        }


        //Map<String, String> validParameters = new HashMap<>();
        for (CustomReportParameter customReportParameter : customReport.getCustomReportParameters()) {
            if (Strings.isBlank(customReportParameter.getName())) {
                throw MissingInformationException.raiseException("report is not correctly setup, " +
                        " the parameter name is empty");
            }

            // 1. if the parameter is passed in, use the value
            // 2. if there's a default value define, user the default value
            // 3. if the parameter is not required, ignore the parameter
            // 4. raise error
            if (Strings.isNotBlank(customReportParameter.getValue())) {

                queryString.append(" and ").append(customReportParameter.getName())
                        .append(" = ")
                        .append(" :").append(customReportParameter.getName());

                paramMap.put(customReportParameter.getName(), customReportParameter.getValue());

                actualQueryString.append(" and ").append(customReportParameter.getName())
                        .append(" = '").append(customReportParameter.getValue()).append("'");
            }
            else if (Strings.isNotBlank(customReportParameter.getDefaultValue())){

                queryString.append(" and ").append(customReportParameter.getName())
                        .append(" = ")
                        .append(" :").append(customReportParameter.getName());

                paramMap.put(customReportParameter.getName(), customReportParameter.getDefaultValue());

                actualQueryString.append(" and ").append(customReportParameter.getName())
                        .append(" = '").append(customReportParameter.getDefaultValue()).append("'");

            }
            else if (Boolean.TRUE.equals(customReportParameter.getRequired())) {

                throw MissingInformationException.raiseException("parameter  " + customReportParameter.getName() +
                        " is required");
            }
        }



        if (Strings.isNotBlank(customReport.getGroupBy())) {
            queryString.append(" group by ").append(customReport.getGroupBy());
            actualQueryString.append(" group by ").append(customReport.getGroupBy());
        }
        if (Strings.isNotBlank(customReport.getSortBy())) {
            queryString.append(" order  by ").append(customReport.getSortBy());
            actualQueryString.append(" order  by ").append(customReport.getSortBy());
        }
        return Triple.of(queryString.toString(), actualQueryString.toString(), paramMap);
    }

    @Transactional
    public CustomReportExecutionHistory runCustomReport(Long id,
                                                        Long companyId,
                                                        Long warehouseId,
                                                        CustomReport customReport) {

        CustomReport existingCustomReport = findById(id);
        copyParameterValues(existingCustomReport, customReport);



        CustomReportExecutionHistory customReportExecutionHistory =
                new CustomReportExecutionHistory(existingCustomReport, companyId, warehouseId,
                        existingCustomReport.getQuery());

        customReportExecutionHistory =
                customReportExecutionHistoryService.addCustomReportExecutionHistory(customReportExecutionHistory);

        Map<String, String> paramMap;
        String queryString;
        String actualQueryString;

        try{
            Triple<String, String, Map<String, String>> query = getQuery(companyId, warehouseId, existingCustomReport);
            queryString = query.getLeft();
            actualQueryString = query.getMiddle();
            paramMap = query.getRight();

            customReportExecutionHistory.setQuery(actualQueryString);
            customReportExecutionHistoryService.save(customReportExecutionHistory);

        }
        catch (Exception ex) {

            ex.printStackTrace();

            customReportExecutionHistory.setCustomReportExecutionPercent(100);
            customReportExecutionHistory.setStatus(CustomReportExecutionStatus.FAIL);
            customReportExecutionHistory.setErrorMessage(ex.getMessage());
            customReportExecutionHistoryService.save(customReportExecutionHistory);
            return customReportExecutionHistory;
        }

        if (Strings.isBlank(queryString) || Strings.isBlank(actualQueryString) ||
            Objects.isNull(paramMap)) {
            logger.debug("fail to generate the query");

            customReportExecutionHistory.setCustomReportExecutionPercent(100);
            customReportExecutionHistory.setStatus(CustomReportExecutionStatus.FAIL);
            customReportExecutionHistory.setErrorMessage("fail to generate the query");
            customReportExecutionHistoryService.save(customReportExecutionHistory);
            return customReportExecutionHistory;
        }




        Long customReportExecutionHistoryId = customReportExecutionHistory.getId();
        inProcessCustomReport.put(customReportExecutionHistoryId, customReportExecutionHistory);

        new Thread(() ->{

            // delay to make sure the CustomReportExecutionHistory is already
            // saved to the database so that we can make sure
            // the one in the new thread is working on the same CustomReportExecutionHistory
            // as the one in the main thread
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            CustomReportExecutionHistory existingCustomReportExecutionHistory =
                    inProcessCustomReport.get(customReportExecutionHistoryId);
            try {

                existingCustomReportExecutionHistory.setStatus(CustomReportExecutionStatus.RUNNING);

                logger.debug("will save {} and change status to {}",
                        existingCustomReportExecutionHistory.getId(),
                        CustomReportExecutionStatus.RUNNING);

                existingCustomReportExecutionHistory =
                        customReportExecutionHistoryService.save(existingCustomReportExecutionHistory);

                logger.debug("execution history  {} saved",
                        existingCustomReportExecutionHistory.getId());

                //Query query = entityManager.createNativeQuery(queryString.toString(), Tuple.class);


                //for (Map.Entry<String, String> parameter : validParameters.entrySet()) {
                //    query.setParameter(parameter.getKey(), parameter.getValue());
                //}
                logger.debug("start to get result from query \n{}",
                        actualQueryString.toString());

                // List<Map<String, Object>> results = jdbcTemplate.queryForList(actualQueryString.toString(), paramMap);
                SqlRowSet sqlRowSet = jdbcTemplate.queryForRowSet(queryString.toString(), paramMap);
                if (!sqlRowSet.next()) {
                    throw new Exception("No result found");
                }
                // reset cursor back to the first row
                sqlRowSet.beforeFirst();

                int rowCount = displaySqlRowSet(sqlRowSet);

                // List<Tuple> results = query.getResultList();
                logger.debug("Get {} result from the query: \n{}",
                        rowCount, actualQueryString);



                existingCustomReportExecutionHistory.setCustomReportExecutionPercent(20);
                existingCustomReportExecutionHistory.setResultRowCount(sqlRowSet.getRow());
                existingCustomReportExecutionHistory.setStatus(CustomReportExecutionStatus.EXPORT_RESULT);
                existingCustomReportExecutionHistory =
                        customReportExecutionHistoryService.save(existingCustomReportExecutionHistory);

                // save to the file
                if (Strings.isNotBlank(customReportResultFolder)) {

                    String filePath = exportReportData(customReportResultFolder, companyId, warehouseId,
                            customReport.getName(), existingCustomReportExecutionHistory.getId(),
                            sqlRowSet);

                    existingCustomReportExecutionHistory.setCustomReportExecutionPercent(100);
                    existingCustomReportExecutionHistory.setStatus(CustomReportExecutionStatus.COMPLETE);
                    existingCustomReportExecutionHistory.setResultFile(filePath);
                    existingCustomReportExecutionHistory.setResultFileExpired(false);
                    // expired in 1 hour
                    existingCustomReportExecutionHistory.setResultFileExpiredTime(ZonedDateTime.now().plusHours(1));
                    customReportExecutionHistoryService.save(existingCustomReportExecutionHistory);
                }
                else {
                    throw new Exception("result folder is not setup");
                }

            }
            catch (Exception ex) {

                ex.printStackTrace();

                existingCustomReportExecutionHistory.setCustomReportExecutionPercent(100);
                existingCustomReportExecutionHistory.setStatus(CustomReportExecutionStatus.FAIL);
                existingCustomReportExecutionHistory.setErrorMessage(ex.getMessage());
                customReportExecutionHistoryService.save(existingCustomReportExecutionHistory);

            }

        }).start();

        return customReportExecutionHistory;

    }

    /**
     * Copy the parameters value from to
     * @param to
     * @param from
     */
    private void copyParameterValues(CustomReport to, CustomReport from) {

        // setup the custom report with parameters from the user
        Map<Long, String> parametersWithValue =new HashMap<>();
        from.getCustomReportParameters().forEach(
                parameter -> parametersWithValue.put(
                        parameter.getId(),
                        parameter.getValue()
                )
        );
        to.getCustomReportParameters().forEach(
                parameter -> {
                    if (parametersWithValue.containsKey(parameter.getId())) {
                        parameter.setValue(parametersWithValue.get(parameter.getId()));
                    }
                }
        );
    }

    private int displaySqlRowSet(SqlRowSet sqlRowSet) {

        int rowCount = 0;

        logger.debug("start to display SqlRowSet");
        logger.debug("=========    Columns   ========");
        String[] columnNames = sqlRowSet.getMetaData().getColumnNames();
        logger.debug(String.join(",", columnNames));

        while(sqlRowSet.next()) {
            List<String> cells = new ArrayList<>();
            for (String columnName : columnNames) {
                cells.add(sqlRowSet.getString(columnName));
            }
            logger.debug(String.join(",", cells));
            rowCount++;

        }

        // reset cursor to the first row
        sqlRowSet.beforeFirst();

        return rowCount;

    }

    private void displayNativeQueryResult(List<Map<String, Object>> results) {
        logger.debug("start to display the result with size {}", results.size());

        for (Map<String, Object> row : results) {

            List<String> columnNames = new ArrayList<>();
            List<String> values = new ArrayList<>();
            row.entrySet().forEach(
                    column -> {
                        columnNames.add(column.getKey());
                        values.add(column.getValue().toString());
                    }
            );
            logger.debug(String.join(",", columnNames));
            logger.debug(String.join(",", values));
        }
    }

    /**
    private void displayNativeQueryResult(List<Tuple> results) {
        for (Tuple row : results) {

            String columnNames = "";
            // Get Column Names
            List<TupleElement<?>> elements = row.getElements();
            for (TupleElement<?> element : elements ) {
                columnNames += element.getAlias() + ",";
            }
            logger.debug("=================   Column Name   =======================");
            logger.debug(columnNames);

            String cellValues = "";
            for (Object cell : row.toArray()) {
                cellValues += cell.toString() + ",";
            }
            logger.debug(cellValues);
        }
    }
     **/


    public String exportReportData(String customReportResultFolder,
                                   Long companyId,
                                   Long warehouseId,
                                   String customReportName,
                                   Long customReportExcutionHistoryId,
                                   SqlRowSet sqlRowSet) throws FileNotFoundException {

        String fileName = companyId + "_" + warehouseId + "_" + customReportName +
                "_" + customReportExcutionHistoryId + "_" + System.currentTimeMillis() + ".csv";
        Path filePath = Paths.get(customReportResultFolder, fileName);

        File resultFile = filePath.toFile();

        // remove the file if it already exists
        resultFile.deleteOnExit();
        if (!resultFile.getParentFile().exists()) {
            resultFile.getParentFile().mkdirs();
        }


        logger.debug("start to write custom report into {} !",
                filePath.toString());


        try (PrintWriter pw = new PrintWriter(resultFile)) {
            // write the header
            String[] columnNames = sqlRowSet.getMetaData().getColumnNames();
            pw.println(String.join(",", columnNames));

            // append each row
            while(sqlRowSet.next()) {
                List<String> cells = new ArrayList<>();
                for (String columnName : columnNames) {
                    cells.add(sqlRowSet.getString(columnName));
                }
                pw.println(String.join(",", cells));

            }

        }

        return filePath.toString();



    }
}
