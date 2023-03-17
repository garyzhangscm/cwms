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

import com.garyzhangscm.cwms.workorder.exception.ResourceNotFoundException;
import com.garyzhangscm.cwms.workorder.model.SiloConfiguration;
import com.garyzhangscm.cwms.workorder.repository.SiloConfigurationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;

import javax.persistence.criteria.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


@Service
public class SiloConfigurationService  {
    private static final Logger logger = LoggerFactory.getLogger(SiloConfigurationService.class);

    @Autowired
    private SiloConfigurationRepository siloConfigurationRepository;

    @Cacheable(cacheNames = "sileConfiguration", unless="#result == null")
    public SiloConfiguration findById(Long id) {
        return siloConfigurationRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.raiseException("Silo Configuration not found by id: " + id));
    }

    public List<SiloConfiguration> findAll(Long warehouseId, Boolean enabled) {


       return siloConfigurationRepository.findAll(
                (Root<SiloConfiguration> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) -> {
                    List<Predicate> predicates = new ArrayList<Predicate>();

                    if (Objects.nonNull(warehouseId)) {
                        predicates.add(criteriaBuilder.equal(root.get("warehouseId"), warehouseId));
                    }


                    if (Objects.nonNull(enabled)) {

                        predicates.add(criteriaBuilder.equal(root.get("enabled"), enabled));
                    }

                    Predicate[] p = new Predicate[predicates.size()];
                    return criteriaBuilder.and(predicates.toArray(p));
                }
        );
    }

    public List<SiloConfiguration> findSiloEnabledWarehouse() {
        return findAll(null, true);
    }


    @Caching(
            evict = {
                    @CacheEvict(cacheNames = "sileConfiguration", allEntries = true),
            }
    )
    public SiloConfiguration save(SiloConfiguration siloConfiguration) {
        return siloConfigurationRepository.save(siloConfiguration);
    }

    public SiloConfiguration saveOrUpdate(SiloConfiguration siloConfiguration) {
        if (siloConfiguration.getId() == null &&
                findByWarehouseId(siloConfiguration.getWarehouseId()) != null) {
            siloConfiguration.setId(
                    findByWarehouseId(siloConfiguration.getWarehouseId()).getId());
        }
        return save(siloConfiguration);
    }


    @Caching(
            evict = {
                    @CacheEvict(cacheNames = "sileConfiguration", allEntries = true),
            }
    )
    public void delete(SiloConfiguration siloConfiguration) {
        siloConfigurationRepository.delete(siloConfiguration);
    }

    @Caching(
            evict = {
                    @CacheEvict(cacheNames = "sileConfiguration", allEntries = true),
            }
    )
    public void delete(Long id) {
        siloConfigurationRepository.deleteById(id);
    }


    @Cacheable(cacheNames = "sileConfiguration", unless="#result == null", key = "new org.springframework.cache.interceptor.SimpleKey('warehouse_', #warehouseId.toString())")
    public SiloConfiguration findByWarehouseId(Long warehouseId) {

        return siloConfigurationRepository.findByWarehouseId(warehouseId);
    }

    public SiloConfiguration addSiloConfiguration(SiloConfiguration siloConfiguration) {
        return saveOrUpdate(siloConfiguration);
    }

    public SiloConfiguration changeSiloConfiguration(Long id, SiloConfiguration siloConfiguration) {
        return saveOrUpdate(siloConfiguration);
    }
}
