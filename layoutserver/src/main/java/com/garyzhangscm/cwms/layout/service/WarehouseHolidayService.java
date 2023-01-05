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
import com.garyzhangscm.cwms.layout.model.Company;
import com.garyzhangscm.cwms.layout.model.Warehouse;
import com.garyzhangscm.cwms.layout.model.WarehouseConfiguration;
import com.garyzhangscm.cwms.layout.model.WarehouseHoliday;
import com.garyzhangscm.cwms.layout.repository.WarehouseConfigurationRepository;
import com.garyzhangscm.cwms.layout.repository.WarehouseHolidayRepository;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.persistence.criteria.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
public class WarehouseHolidayService {

    private static final Logger logger = LoggerFactory.getLogger(WarehouseHolidayService.class);

    @Autowired
    private WarehouseHolidayRepository warehouseHolidayRepository;


    public WarehouseHoliday findById(Long id ) {
        return warehouseHolidayRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.raiseException("warehouse holiday not found by id: " + id));

    }



    public List<WarehouseHoliday> findAll(Long companyId,
                                          String companyCode,
                                          Long warehouseId,
                                          String warehouseName) {

        return warehouseHolidayRepository.findAll(
                (Root<WarehouseHoliday> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) -> {
                    List<Predicate> predicates = new ArrayList<Predicate>();

                    if (Objects.nonNull(companyId) || Objects.nonNull(warehouseId) ||
                            Strings.isNotBlank(companyCode) || Strings.isNotBlank(warehouseName)) {

                        Join<WarehouseHoliday, Warehouse> joinWarehouse = root.join("warehouse", JoinType.INNER);
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


    public WarehouseHoliday save(WarehouseHoliday warehouseHoliday) {
        return warehouseHolidayRepository.save(warehouseHoliday);
    }

    public List<WarehouseHoliday> findByWarehouse(Long warehouseId) {

        return warehouseHolidayRepository.findByWarehouse(warehouseId);
    }

    /**
     * Find all the holidays in certain year for certain warehouse. The year needs to be
     * YYYY
     * @param warehouseId
     * @param year
     * @return
     */
    public List<WarehouseHoliday> findByWarehouseAndYear(Long warehouseId, String year) {

        return warehouseHolidayRepository.findByWarehouseAndYear(warehouseId, Integer.parseInt(year));
    }


    /**
     * check if the specific date is defined as holiday for the warehouse
     * @param warehouseId
     * @param date
     * @return
     */
    public WarehouseHoliday findByWarehouseAndDate(Long warehouseId, LocalDate date) {

        // convert the date to the format of YYYY-MM-DD and compare with the
        // date saved in database

        // DateTimeFormatter formatter = DateTimeFormatter.ofPattern("YYYY-MM-dd");
        // return findByWarehouseAndDate(warehouseId, formatter.format(date));

        return warehouseHolidayRepository.findByWarehouseAndDate(warehouseId, date);
    }

    public WarehouseHoliday findByWarehouseAndDate(Long warehouseId, LocalDateTime date) {

        // convert the date to the format of YYYY-MM-DD and compare with the
        // date saved in database

        return findByWarehouseAndDate(warehouseId, date.toLocalDate());
    }


    public WarehouseHoliday saveOrUpdate(WarehouseHoliday warehouseHoliday) {
        if (warehouseHoliday.getId() == null && findByWarehouseAndDate(warehouseHoliday.getWarehouse().getId(), warehouseHoliday.getHolidayDate()) != null) {
            warehouseHoliday.setId(findByWarehouseAndDate(warehouseHoliday.getWarehouse().getId(), warehouseHoliday.getHolidayDate()).getId());
        }
        return save(warehouseHoliday);
    }

    public void delete(WarehouseHoliday warehouseHoliday) {
        warehouseHolidayRepository.delete(warehouseHoliday);
    }
    public void delete(Long id) {
        warehouseHolidayRepository.deleteById(id);
    }

    public WarehouseHoliday addWarehouseHoliday(WarehouseHoliday warehouseHoliday) {
        return saveOrUpdate(warehouseHoliday);
    }

    /**
     * Check if the date is a public holiday
     * @param warehouseId
     * @param date
     * @return
     */
    public boolean isHoliday(Long warehouseId, LocalDate date) {
        return Objects.nonNull(findByWarehouseAndDate(warehouseId, date));
    }
}
