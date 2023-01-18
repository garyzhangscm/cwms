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

package com.garyzhangscm.cwms.layout.service;

import com.garyzhangscm.cwms.layout.exception.ResourceNotFoundException;
import com.garyzhangscm.cwms.layout.model.*;
import com.garyzhangscm.cwms.layout.repository.WarehouseConfigurationRepository;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.persistence.criteria.*;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
public class WarehouseConfigurationService   {

    private static final Logger logger = LoggerFactory.getLogger(WarehouseConfigurationService.class);

    @Autowired
    private WarehouseConfigurationRepository warehouseConfigurationRepository;
    @Autowired
    private WarehouseHolidayService warehouseHolidayService;


    public WarehouseConfiguration findById(Long id ) {
        return warehouseConfigurationRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.raiseException("warehouse configuration not found by id: " + id));

    }



    public List<WarehouseConfiguration> findAll(Long companyId,
                                               String companyCode,
                                               Long warehouseId,
                                               String warehouseName) {

        return warehouseConfigurationRepository.findAll(
                (Root<WarehouseConfiguration> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) -> {
                    List<Predicate> predicates = new ArrayList<Predicate>();

                    if (Objects.nonNull(companyId) || Objects.nonNull(warehouseId) ||
                            Strings.isNotBlank(companyCode) || Strings.isNotBlank(warehouseName)) {

                        Join<WarehouseConfiguration, Warehouse> joinWarehouse = root.join("warehouse", JoinType.INNER);
                        if (Objects.nonNull(warehouseId)) {
                            predicates.add(criteriaBuilder.equal(joinWarehouse.get("id"), warehouseId));

                        }
                        else if (Strings.isNotBlank(warehouseName)) {
                            predicates.add(criteriaBuilder.equal(joinWarehouse.get("name"), warehouseName));

                        }
                        if (Objects.nonNull(companyId) ||Strings.isNotBlank(companyCode)) {

                            Join<Warehouse, Company> joinCompany = joinWarehouse.join("company", JoinType.INNER);

                            if (Objects.nonNull(companyId)) {
                                predicates.add(criteriaBuilder.equal(joinCompany.get("id"), companyId));

                            }
                            else if (Strings.isNotBlank(companyCode)) {
                                predicates.add(criteriaBuilder.equal(joinCompany.get("code"), companyCode));

                            }
                        }

                    }
                    Predicate[] p = new Predicate[predicates.size()];
                    return criteriaBuilder.and(predicates.toArray(p));
                }
        );
    }


    public WarehouseConfiguration save(WarehouseConfiguration warehouseConfiguration) {
        return warehouseConfigurationRepository.save(warehouseConfiguration);
    }

    public WarehouseConfiguration findByWarehouse(Long warehouseId) {

        return warehouseConfigurationRepository.findByWarehouse(warehouseId);
    }

    public WarehouseConfiguration saveOrUpdate(WarehouseConfiguration warehouseConfiguration) {
        if (warehouseConfiguration.getId() == null && findByWarehouse(warehouseConfiguration.getWarehouse().getId()) != null) {
            warehouseConfiguration.setId(findByWarehouse(warehouseConfiguration.getWarehouse().getId()).getId());
        }
        return save(warehouseConfiguration);
    }

    public void delete(WarehouseConfiguration warehouseConfiguration) {
        warehouseConfigurationRepository.delete(warehouseConfiguration);
    }
    public void delete(Long id) {
        warehouseConfigurationRepository.deleteById(id);
    }


    public WarehouseConfiguration changeWarehouseConfiguration(Long companyId, WarehouseConfiguration warehouseConfiguration) {
        return saveOrUpdate(warehouseConfiguration);
    }

    public void removeWarehouseConfiguration(Warehouse warehouse) {
        WarehouseConfiguration warehouseConfiguration = findByWarehouse(warehouse.getId());

        if (Objects.nonNull(warehouseConfiguration)) {
            delete(warehouseConfiguration.getId());
        }
    }

    /**
     * Check if the specific date is a working day for the warehouse
     * if the date is not passed in, then check if today is a working day
     * @param warehouseId
     * @param date
     * @return
     */
    public Boolean isWorkingDay(Long warehouseId, LocalDate date) {
        if (Objects.isNull(date)) {
            date = LocalDate.now();
        }
        // check if the warehouse is working on the date based on
        // the weekday calendar
        if (!isWorkingWeekDay(warehouseId, date)) {
            // according to the weekday calenda, the warehouse is
            // not working on that day. For example, some warehouse
            // may not working on Saturday and Sunday
            logger.debug("{} is not a working weekday", date);
            return false;
        }
        if (isHoliday(warehouseId, date)) {
            // the warehouse may schedule to work on that weekday
            // but it is a public holiday so that the warehouse
            // will be closed
            return false;
        }
        return true;
    }

    /**
     * Check if a specific date is a holiday and the warehouse is close
     * @param warehouseId
     * @param date
     * @return
     */
    private boolean isHoliday(Long warehouseId, LocalDate date) {
        return warehouseHolidayService.isHoliday(warehouseId, date);
    }

    /**
     * Check if the warehouse is working on that weekday
     * @param warehouseId
     * @param date
     * @return
     */
    private boolean isWorkingWeekDay(Long warehouseId, LocalDate date) {
        // get the weekday of the date
        DayOfWeek weekDay = date.getDayOfWeek();
        WarehouseConfiguration warehouseConfiguration = findByWarehouse(warehouseId);
        logger.debug("date: {}, days of week {}",
                date, weekDay);
        boolean isWorkingWeekDay = false;
        switch (weekDay) {
            case SUNDAY:
                isWorkingWeekDay = Boolean.TRUE.equals(warehouseConfiguration.getWorkingOnSundayFlag());
                break;
            case MONDAY:
                isWorkingWeekDay = Boolean.TRUE.equals(warehouseConfiguration.getWorkingOnMondayFlag());
                break;
            case TUESDAY:
                isWorkingWeekDay = Boolean.TRUE.equals(warehouseConfiguration.getWorkingOnTuesdayFlag());
                break;
            case WEDNESDAY:
                isWorkingWeekDay = Boolean.TRUE.equals(warehouseConfiguration.getWorkingOnWednesdayFlag());
                break;
            case THURSDAY:
                isWorkingWeekDay = Boolean.TRUE.equals(warehouseConfiguration.getWorkingOnThursdayFlag());
                break;
            case FRIDAY:
                isWorkingWeekDay = Boolean.TRUE.equals(warehouseConfiguration.getWorkingOnFridayFlag());
                break;
            case SATURDAY:
                isWorkingWeekDay = Boolean.TRUE.equals(warehouseConfiguration.getWorkingOnSaturdayFlag());
                break;
        }
        return isWorkingWeekDay;
    }

    /**
     * Get the next working day for the warehouse
     *
     * @param warehouseId
     * @param includingToday
     * @return
     */
    public LocalDate getNextWorkingDay(Long warehouseId, Boolean includingToday) {
        LocalDate today = LocalDate.now();
        if (Boolean.TRUE.equals(includingToday) && isWorkingDay(warehouseId, today)) {
            // let's check if today is a working day, if so, let's return the today
            return today;
        }
        // let's get the next working date
        // we will just look forward 999 days to find the next working day
        // just in case any configuration goes wrong
        for(int i = 0; i < 999; i++) {
            if (isWorkingDay(warehouseId, today.plusDays(i))) {
                logger.debug("find the next working day, which is {}",
                        today.plusDays(i));
                return today.plusDays(i);
            }
        }
        logger.debug("can't find next working day, start from today and looking forward for 999 days");
        throw ResourceNotFoundException.raiseException("can't find next working day, start from today and looking forward for 999 days");
    }
}
