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

package com.garyzhangscm.cwms.layout.repository;

import com.garyzhangscm.cwms.layout.model.WarehouseConfiguration;
import com.garyzhangscm.cwms.layout.model.WarehouseHoliday;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface WarehouseHolidayRepository extends JpaRepository<WarehouseHoliday, Long>, JpaSpecificationExecutor<WarehouseHoliday> {

    @Query("select wh from WarehouseHoliday wh where wh.warehouse.id = :warehouseId")
    public List<WarehouseHoliday> findByWarehouse(Long warehouseId);

    /**
     * Find all the holidays in certain year for certain warehouse. The year needs to be
     * YYYY
     * @param warehouseId
     * @param year
     * @return
     */
    @Query("select wh from WarehouseHoliday wh where wh.warehouse.id = :warehouseId and YEAR(holidayDate) = :year")
    public List<WarehouseHoliday> findByWarehouseAndYear(Long warehouseId, int year);

    /**
     * check if the specific date is defined as holiday for the warehouse
     * date needs to be in the format of YYYY-MM-DD
     * @param warehouseId
     * @param date
     * @return
     */
    @Query(value = "select * from warehouse_holiday where warehouse_id = :warehouseId and DATE(holiday_date)  = :date",
            nativeQuery = true)
    public WarehouseHoliday findByWarehouseAndDate(Long warehouseId, LocalDate date);
}
