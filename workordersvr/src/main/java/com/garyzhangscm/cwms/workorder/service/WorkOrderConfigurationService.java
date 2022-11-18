/**
 * Copyright 2018
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

package com.garyzhangscm.cwms.workorder.service;

import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import com.garyzhangscm.cwms.workorder.clients.WarehouseLayoutServiceRestemplateClient;
import com.garyzhangscm.cwms.workorder.exception.ResourceNotFoundException;
import com.garyzhangscm.cwms.workorder.exception.WorkOrderException;
import com.garyzhangscm.cwms.workorder.model.*;
import com.garyzhangscm.cwms.workorder.repository.ProductionShiftScheduleRepository;
import com.garyzhangscm.cwms.workorder.repository.WorkOrderConfigurationRepository;
import org.apache.commons.lang.StringUtils;
import org.hibernate.jdbc.Work;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

@Service
public class WorkOrderConfigurationService implements TestDataInitiableService{
    private static final Logger logger = LoggerFactory.getLogger(WorkOrderConfigurationService.class);
    @Autowired
    private WorkOrderConfigurationRepository workOrderConfigurationRepository;

    @Autowired
    private ProductionShiftScheduleRepository productionShiftScheduleRepository;


    @Autowired
    private WarehouseLayoutServiceRestemplateClient layoutServiceRestemplateClient;
    @Autowired
    private FileService fileService;
    @Value("${fileupload.test-data.work-order-configuration:work-order-configuration}")
    String testDataFile;


    public WorkOrderConfiguration findById(Long id) {
        WorkOrderConfiguration workOrderConfiguration = workOrderConfigurationRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.raiseException("web client tab display configuration not found by id: " + id));

        loadProductionShiftSchedule(workOrderConfiguration);
        return  workOrderConfiguration;
    }

    public List<WorkOrderConfiguration> findAll(Long companyId,
                                                Long warehouseId) {

        List<WorkOrderConfiguration> workOrderConfigurations =
                workOrderConfigurationRepository.findAll(
                (Root<WorkOrderConfiguration> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) -> {
                    List<Predicate> predicates = new ArrayList<Predicate>();

                    if (Objects.nonNull(companyId)) {
                        predicates.add(criteriaBuilder.equal(root.get("companyId"), companyId));

                    }
                    if (Objects.nonNull(warehouseId)) {
                        predicates.add(criteriaBuilder.equal(root.get("warehouseId"), warehouseId));

                    }
                    Predicate[] p = new Predicate[predicates.size()];
                    return criteriaBuilder.and(predicates.toArray(p));
                }
        );
        loadProductionShiftSchedule(workOrderConfigurations);
        return workOrderConfigurations;
    }
    public WorkOrderConfiguration save(WorkOrderConfiguration workOrderConfiguration) {
        WorkOrderConfiguration newWorkOrderConfiguration =
                workOrderConfigurationRepository.save(workOrderConfiguration);
        loadProductionShiftSchedule(newWorkOrderConfiguration);
        return newWorkOrderConfiguration;
    }



    public List<WorkOrderConfigurationCSVWrapper> loadData(InputStream inputStream) throws IOException {

        CsvSchema schema = CsvSchema.builder().
                addColumn("company").
                addColumn("warehouse").
                addColumn("materialConsumeTiming").
                addColumn("overConsumeIsAllowed").
                addColumn("overProduceIsAllowed").
                build().withHeader();

        return fileService.loadData(inputStream, schema, WorkOrderConfigurationCSVWrapper.class);
    }

    public void initTestData(Long companyId, String warehouseName) {
        try {
            String companyCode = companyId == null ?
                    "" : layoutServiceRestemplateClient.getCompanyById(companyId).getCode();

            String testDataFileName = StringUtils.isBlank(warehouseName) ?
                    testDataFile + ".csv" :
                    testDataFile + "-" + companyCode + "-" + warehouseName + ".csv";

            logger.debug("Start to init web client tab display configuration from {}",
                    testDataFileName);
            InputStream inputStream = new ClassPathResource(testDataFileName).getInputStream();
            List<WorkOrderConfigurationCSVWrapper> workOrderConfigurationCSVWrappers = loadData(inputStream);

            workOrderConfigurationCSVWrappers.stream().forEach(
                    workOrderConfigurationCSVWrapper ->
                            save(convertFromCSVWrapper(workOrderConfigurationCSVWrapper)));
        } catch (IOException ex) {
            logger.debug("Exception while load test data: {}", ex.getMessage());
        }
    }

    private WorkOrderConfiguration convertFromCSVWrapper(
            WorkOrderConfigurationCSVWrapper workOrderConfigurationCSVWrapper) {
        WorkOrderConfiguration workOrderConfiguration = new WorkOrderConfiguration();
        workOrderConfiguration.setMaterialConsumeTiming(
                WorkOrderMaterialConsumeTiming.valueOf(
                        workOrderConfigurationCSVWrapper.getMaterialConsumeTiming()
                ));
        workOrderConfiguration.setOverConsumeIsAllowed(
                workOrderConfigurationCSVWrapper.isOverConsumeIsAllowed());
        workOrderConfiguration.setOverProduceIsAllowed(
                workOrderConfigurationCSVWrapper.isOverProduceIsAllowed());

        if (StringUtils.isNotBlank(workOrderConfigurationCSVWrapper.getCompany())) {
            Company company = layoutServiceRestemplateClient.getCompanyByCode(
                    workOrderConfigurationCSVWrapper.getCompany()
            );
            if (Objects.nonNull(company)) {
                workOrderConfiguration.setCompanyId(company.getId());
                // only set the warehouse if the warehouse name and company name are
                // all present
                if (StringUtils.isNotBlank(workOrderConfigurationCSVWrapper.getWarehouse())) {
                    Warehouse warehouse = layoutServiceRestemplateClient.getWarehouseByName(
                            company.getCode(), workOrderConfigurationCSVWrapper.getWarehouse()
                    );
                    if (Objects.nonNull(warehouse)) {
                        workOrderConfiguration.setWarehouseId(warehouse.getId());
                    }
                }
            }
        }

        return workOrderConfiguration;


    }

    public boolean isOverProduceAllowed(WorkOrder workOrder) {

        WorkOrderConfiguration workOrderConfiguration = getWorkOrderConfiguration(
                workOrder.getWarehouse().getCompanyId(),
                workOrder.getWarehouseId()
        );

        return Objects.nonNull(workOrderConfiguration) &&
                Boolean.TRUE.equals(workOrderConfiguration.getOverProduceIsAllowed());
    }
    public boolean isOverConsumeAllowed(WorkOrder workOrder) {

        WorkOrderConfiguration workOrderConfiguration = getWorkOrderConfiguration(
                workOrder.getWarehouse().getCompanyId(),
                workOrder.getWarehouseId()
        );

        return Objects.nonNull(workOrderConfiguration) &&
                Boolean.TRUE.equals(workOrderConfiguration.getOverConsumeIsAllowed());
    }
    public WorkOrderMaterialConsumeTiming getWorkOrderMaterialConsumeTiming(WorkOrder workOrder) {

        // if we already have the configuration setup in the work order, then
        // use the one from work order. otherwise, use the one from the configuration
        logger.debug("Get work order material consume timing");
        logger.debug("work order's material consume timing is setup to {}",
                workOrder.getMaterialConsumeTiming());
        if (Objects.nonNull(workOrder.getMaterialConsumeTiming())) {
            return workOrder.getMaterialConsumeTiming();
        }
        return getWorkOrderConfiguration(
                workOrder.getWarehouse().getCompanyId(),
                workOrder.getWarehouseId()
        ).getMaterialConsumeTiming();
    }


    public WorkOrderConfiguration getWorkOrderConfiguration(Long companyId, Long warehouseId) {
        // we will start with most specific configuration until we get the most generic config
        // most specific -> most generic
        // 1. company + warehouse
        // 2. company
        // 3. default

        WorkOrderConfiguration bestMatchWorkOrderConfiguration = null;
        List<WorkOrderConfiguration> workOrderConfigurations = new ArrayList<>();
        // 1. company + warehouse
        workOrderConfigurations = findAll(companyId, warehouseId);
        if (workOrderConfigurations.size() >= 1) {
            bestMatchWorkOrderConfiguration = workOrderConfigurations.get(0);
        }
        // 2. company
        workOrderConfigurations = findAll(companyId, null);
        if (workOrderConfigurations.size() >= 1) {
            bestMatchWorkOrderConfiguration = workOrderConfigurations.get(0);
        }
        // 3. default
        workOrderConfigurations = findAll(null, null);
        if (workOrderConfigurations.size() >= 1) {
            bestMatchWorkOrderConfiguration = workOrderConfigurations.get(0);
        }
        if (Objects.isNull(bestMatchWorkOrderConfiguration)) {
            return null;
        }
        loadProductionShiftSchedule(bestMatchWorkOrderConfiguration);

        return bestMatchWorkOrderConfiguration;
    }


    public WorkOrderConfiguration saveOrUpdate(WorkOrderConfiguration workOrderConfiguration) {

        WorkOrderConfiguration existingWorkOrderConfiguration =
                getWorkOrderConfiguration(workOrderConfiguration.getCompanyId(), workOrderConfiguration.getWarehouseId());
        if (Objects.nonNull(existingWorkOrderConfiguration) &&
                Objects.equals(existingWorkOrderConfiguration.getCompanyId(), workOrderConfiguration.getCompanyId()) &&
                Objects.equals(existingWorkOrderConfiguration.getWarehouseId(), workOrderConfiguration.getWarehouseId())) {
            workOrderConfiguration.setId(existingWorkOrderConfiguration.getId());
        }
        // let's refresh the production line shift schedule
        saveProductionLineSchedule(workOrderConfiguration);
        return save(workOrderConfiguration);

    }

    @Transactional
    private void saveProductionLineSchedule(WorkOrderConfiguration workOrderConfiguration) {
        logger.debug("start to save the shift schedule \n{}", workOrderConfiguration.getProductionShiftSchedules());
        validateShiftSchedule(workOrderConfiguration);

        logger.debug("the shift schedule passed validation");
        logger.debug("will remove the shift schedule for warehouse {} first",
                workOrderConfiguration.getWarehouseId());
        // remove the existing shift schedule and then save the new one
        productionShiftScheduleRepository.deleteByWarehouseId(workOrderConfiguration.getWarehouseId());


        for (ProductionShiftSchedule productionShiftSchedule : workOrderConfiguration.getProductionShiftSchedules()) {
            logger.debug("will save schedule {}", productionShiftSchedule);
            productionShiftScheduleRepository.save(productionShiftSchedule);
        }
    }

    private void validateShiftSchedule(WorkOrderConfiguration workOrderConfiguration) {
        // make sure the schedule is not overlapped

        if (workOrderConfiguration.getProductionShiftSchedules().isEmpty()) {
            // production shift schedule is not setup, do nothing
            return;
        }

        // load all the schedule
        int shiftNumbers = workOrderConfiguration.getProductionShiftSchedules().size();
        LocalDateTime[] startTimes = new LocalDateTime[shiftNumbers];
        LocalDateTime[] endTimes = new LocalDateTime[shiftNumbers];

        int index = 0;
        // the start time and end time will be added to today and then we will do the compare
        for (ProductionShiftSchedule productionShiftSchedule : workOrderConfiguration.getProductionShiftSchedules()) {
            startTimes[index] = LocalDateTime.now().withHour(productionShiftSchedule.getShiftStartTime().getHour())
                    .withMinute(productionShiftSchedule.getShiftStartTime().getMinute())
                    .withSecond(productionShiftSchedule.getShiftStartTime().getSecond());
            if (Boolean.TRUE.equals(productionShiftSchedule.getShiftEndNextDay())) {
                endTimes[index] = LocalDateTime.now().plusDays(1)
                        .withHour(productionShiftSchedule.getShiftEndTime().getHour())
                        .withMinute(productionShiftSchedule.getShiftEndTime().getMinute())
                        .withSecond(productionShiftSchedule.getShiftEndTime().getSecond());
            }
            else {
                endTimes[index] = LocalDateTime.now()
                        .withHour(productionShiftSchedule.getShiftEndTime().getHour())
                        .withMinute(productionShiftSchedule.getShiftEndTime().getMinute())
                        .withSecond(productionShiftSchedule.getShiftEndTime().getSecond());
            }
            index++;

        }

        for(int i = 0; i < shiftNumbers - 1; i++) {
            for (int j = i + 1; j < shiftNumbers; j ++) {
                if (isTimeRangeOverLapping(startTimes[i], endTimes[i], startTimes[j], endTimes[j])) {
                    throw WorkOrderException.raiseException("Can't setup the shift schedule as the following time overlapping: " +
                            "[" + startTimes[i] + "," + endTimes[i] + "] and " +
                            "[" + startTimes[j] + "," +  endTimes[j] + "]");
                }

            }
        }

    }

    /**
     * Check if the 2 time range has over lapping.
     * 1. time range 1: start time1 ~ end time 1
     * 2. time range 2: start time2 ~ end time 2
     * @param startTime1
     * @param endTime1
     * @param startTime2
     * @param endTime2
     * @return
     */
    private boolean isTimeRangeOverLapping(LocalDateTime startTime1, LocalDateTime endTime1,
                                           LocalDateTime startTime2, LocalDateTime endTime2) {
        // not over lapping if one of the following condition meet
        // 1. end time 1 < start time 2
        // 2. start time 1 > end time 2
        if (endTime1.isBefore(startTime2)) {
            return false;
        }
        else if (startTime1.isAfter(endTime2)) {
            return false;
        }
        return false;
    }

    private void loadProductionShiftSchedule(List<WorkOrderConfiguration> workOrderConfigurations) {

        workOrderConfigurations.forEach(
                workOrderConfiguration -> loadProductionShiftSchedule(workOrderConfiguration)
        );
    }
    private void loadProductionShiftSchedule(WorkOrderConfiguration workOrderConfiguration) {

        workOrderConfiguration.setProductionShiftSchedules(
                productionShiftScheduleRepository.findByWarehouseId(
                        workOrderConfiguration.getWarehouseId()
                )
        );
    }


    public WorkOrderConfiguration changeWorkOrderConfiguration(WorkOrderConfiguration workOrderConfiguration) {
        return saveOrUpdate(workOrderConfiguration);
    }
}
