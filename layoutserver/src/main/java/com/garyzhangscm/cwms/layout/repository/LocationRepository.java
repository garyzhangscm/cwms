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

import com.garyzhangscm.cwms.layout.model.Location;
import com.garyzhangscm.cwms.layout.model.LocationGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LocationRepository extends JpaRepository<Location, Long>, JpaSpecificationExecutor<Location> {

    @Query("select l from Location l inner join l.warehouse w where l.name = :name and w.id = :warehouseId")
    Location findByName(Long warehouseId, String name);

    @Query( "select l from Location l inner join l.locationGroup lg where lg.id in (:ids)" )
    List<Location> findByLocationGroups(@Param("ids") List<Long> locationGroupIds);

    @Modifying
    @Query( "delete from Location l where l.id in (:ids)" )
    void deleteByLocationIds(@Param("ids") List<Long> locationIds);

    @Modifying
    @Query( "delete from Location l where l.locationGroup.id = :locationGroupId" )
    void deleteByLocationGroupId(Long locationGroupId);

    List<Location> findByCountSequenceBetween(Long beginSequence, Long endSequence);

    List<Location> findByPickSequenceBetween(Long beginSequence, Long endSequence);

    List<Location> findByPutawaySequenceBetween(Long beginSequence, Long endSequence);

    @Query("select l from Location l inner join l.locationGroup.locationGroupType type where type.dock = true and l.enabled = true and l.warehouse.id = :warehouseId")
    List<Location> getDockLocations(Long warehouseId);
}
