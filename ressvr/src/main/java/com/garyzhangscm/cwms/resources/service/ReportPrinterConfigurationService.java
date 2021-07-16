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

import com.garyzhangscm.cwms.resources.exception.ResourceNotFoundException;
import com.garyzhangscm.cwms.resources.model.ReportHistory;
import com.garyzhangscm.cwms.resources.model.ReportPrinterConfiguration;
import com.garyzhangscm.cwms.resources.model.ReportType;
import com.garyzhangscm.cwms.resources.repository.ReportPrinterConfigurationRepository;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


@Service
public class ReportPrinterConfigurationService {
    private static final Logger logger = LoggerFactory.getLogger(ReportPrinterConfigurationService.class);
    @Autowired
    private ReportPrinterConfigurationRepository reportPrinterConfigurationRepository;

    public ReportPrinterConfiguration findById(Long id) {
        return reportPrinterConfigurationRepository.findById(id)
                .orElseThrow(
                        () -> ResourceNotFoundException.raiseException(
                                "report history not found by id: " + id));
    }

    public ReportPrinterConfiguration save(ReportPrinterConfiguration reportPrinterConfiguration) {

        return reportPrinterConfigurationRepository.save(reportPrinterConfiguration);
    }
    public void delete(ReportPrinterConfiguration reportPrinterConfiguration) {

        reportPrinterConfigurationRepository.delete(reportPrinterConfiguration);
    }
    public void delete(Long id) {

        reportPrinterConfigurationRepository.deleteById(id);
    }


    public ReportPrinterConfiguration saveOrUpdate(ReportPrinterConfiguration reportPrinterConfiguration) {

        if (Objects.isNull(reportPrinterConfiguration.getId()) &&
                Objects.nonNull(
                        reportPrinterConfigurationRepository.findByWarehouseIdAndReportTypeAndCriteriaValue(
                                reportPrinterConfiguration.getWarehouseId(),
                                reportPrinterConfiguration.getReportType(),
                                reportPrinterConfiguration.getCriteriaValue()
                        )
                )
            ) {
            reportPrinterConfiguration.setId(

                    reportPrinterConfigurationRepository.findByWarehouseIdAndReportTypeAndCriteriaValue(
                            reportPrinterConfiguration.getWarehouseId(),
                            reportPrinterConfiguration.getReportType(),
                            reportPrinterConfiguration.getCriteriaValue()
                    ).getId()
            );
        }
        return save(reportPrinterConfiguration);
    }

    public List<ReportPrinterConfiguration> findAll(
              Long warehouseId, String reportType, String criteriaValue) {

        return reportPrinterConfigurationRepository.findAll(
                (Root<ReportPrinterConfiguration> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) -> {
                    List<Predicate> predicates = new ArrayList<Predicate>();

                    if (Objects.nonNull(warehouseId)) {
                        predicates.add(
                                criteriaBuilder.equal(
                                        root.get("warehouseId"), warehouseId));
                    }

                    if (!StringUtils.isBlank(reportType)) {
                        predicates.add(criteriaBuilder.equal(root.get("reportType"),
                                ReportType.valueOf(reportType)));
                    }

                    if (!StringUtils.isBlank(criteriaValue)) {
                        predicates.add(criteriaBuilder.equal(root.get("criteriaValue"),
                                criteriaValue));
                    }



                    Predicate[] p = new Predicate[predicates.size()];
                    return criteriaBuilder.and(predicates.toArray(p));
                }
        );

    }


    public String getPrinterName(Long warehouseId, ReportType reportType, String criteriaValue) {
        // we will always print from the default printer right now
        ReportPrinterConfiguration reportPrinterConfiguration
                = reportPrinterConfigurationRepository.findByWarehouseIdAndReportTypeAndCriteriaValue(
                        warehouseId, reportType, criteriaValue
        );
        if (Objects.isNull(reportPrinterConfiguration)) {
            return "";
        }
        else {
            return reportPrinterConfiguration.getPrinterName();
        }
    }


    public ReportPrinterConfiguration addReportPrinterConfiguration(ReportPrinterConfiguration reportPrinterConfiguration) {
        return  saveOrUpdate(reportPrinterConfiguration);

    }

    public ReportPrinterConfiguration changeReportPrinterConfiguration(ReportPrinterConfiguration reportPrinterConfiguration) {
        return  saveOrUpdate(reportPrinterConfiguration);
    }

    public void deleteReportPrinterConfiguration(Long id) {
        delete(id);

    }
}
