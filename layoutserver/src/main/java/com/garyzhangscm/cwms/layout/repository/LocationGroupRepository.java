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

import com.garyzhangscm.cwms.layout.model.LocationGroup;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LocationGroupRepository extends JpaRepository<LocationGroup, Long>, JpaSpecificationExecutor<LocationGroup> {

    @Query("select lg from LocationGroup lg where lg.warehouse.name = :warehouseName")
    List<LocationGroup> findAll(String warehouseName);

    @Query("select lg from LocationGroup lg where lg.warehouse.name = :warehouseName and lg.name = :name")
    LocationGroup findByName(String warehouseName, String name);

    @Query( "select o from LocationGroup o where location_group_type_id in :ids and o.warehouse.name = :warehouseName" )
    List<LocationGroup> findByLocationGroupTypes(String warehouseName, @Param("ids") List<Long> locationGroupTypeIds);

    @Query( "select lg from LocationGroup lg inner join lg.locationGroupType type where type.trailer = true and lg.warehouse.name = :warehouseName" )
    List<LocationGroup> getDockLocationGroup(String warehouseName);

}
