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

package com.garyzhangscm.cwms.resources.repository;

import com.garyzhangscm.cwms.resources.model.Report;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;


@Repository
public interface ReportRepository
        extends JpaRepository<Report, Long>, JpaSpecificationExecutor<Report> {

    @Query("select r from Report r where r.name = :name " +
           " and r.companyId is null  " +
           " and r.warehouseId is null")
    Report findStandardReportByName(String name);

    Report findByCompanyIdAndName(Long companyId, String name);

    Report findByWarehouseIdAndName(Long warehouseId, String name);



}
