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
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LocationGroupRepository extends JpaRepository<LocationGroup, Long>, JpaSpecificationExecutor<LocationGroup> {

    @Query("select lg from LocationGroup lg where lg.warehouse.id = :warehouseId")
    List<LocationGroup> findAll(Long warehouseId);

    @Query("select lg from LocationGroup lg inner join lg.warehouse w where w.id = :warehouseId and lg.name = :name")
    LocationGroup findByName(Long warehouseId, String name);

    @Query( "select lg from LocationGroup lg inner join lg.warehouse w inner join lg.locationGroupType lgt where lgt.id in (:ids) and w.id = :warehouseId" )
    List<LocationGroup> findByLocationGroupTypes(Long warehouseId, @Param("ids") List<Long> ids);

    @Query( "select lg from LocationGroup lg inner join lg.locationGroupType type where type.trailer = true and lg.warehouse.id = :warehouseId" )
    List<LocationGroup> getDockLocationGroup(Long warehouseId);

    @Query( "select lg from LocationGroup lg inner join lg.locationGroupType type where type.shippedParcel = true and lg.warehouse.id = :warehouseId" )
    List<LocationGroup> getShippedParcelLocationGroup(Long warehouseId);

    @Query( "select lg from LocationGroup lg inner join lg.locationGroupType type where type.shippedOrder = true and lg.warehouse.id = :warehouseId" )
    List<LocationGroup> getShippedOrderLocationGroup(Long warehouseId);

    @Query( "select lg from LocationGroup lg inner join lg.locationGroupType type where type.shippingStage = true and lg.warehouse.id = :warehouseId" )
    List<LocationGroup> getShippingStageLocationGroup(Long warehouseId);

    @Query( "select lg from LocationGroup lg inner join lg.locationGroupType type where type.storage = true and lg.warehouse.id = :warehouseId" )
    List<LocationGroup> getStorageLocationGroups(Long warehouseId);

    @Query( "select lg from LocationGroup lg inner join lg.locationGroupType type where type.qcArea = true and lg.warehouse.id = :warehouseId" )
    List<LocationGroup> getQCLocationGroups(Long warehouseId);


    @Query( "select lg from LocationGroup lg inner join lg.locationGroupType type where type.customerReturnStageLocation = true and lg.warehouse.id = :warehouseId" )
    List<LocationGroup> getCustomerReturnStageLocationGroups(Long warehouseId);



    @Modifying
    @Query( "delete from LocationGroup lg where lg.warehouse.id = :warehouseId" )
    void deleteByWarehouseId(Long warehouseId);

}
