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

package com.garyzhangscm.cwms.outbound.service;

import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import com.garyzhangscm.cwms.outbound.clients.CommonServiceRestemplateClient;
import com.garyzhangscm.cwms.outbound.exception.ResourceNotFoundException;
import com.garyzhangscm.cwms.outbound.model.Pick;
import com.garyzhangscm.cwms.outbound.model.hualei.HualeiProduct;
import com.garyzhangscm.cwms.outbound.repository.HualeiProductRepository;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


@Service
public class HualeiProductService  {
    private static final Logger logger = LoggerFactory.getLogger(HualeiProductService.class);

    @Autowired
    private HualeiProductRepository hualeiProductRepository;
    @Autowired
    private CommonServiceRestemplateClient commonServiceRestemplateClient;

    public HualeiProduct findById(Long id) {
        return hualeiProductRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.raiseException("Hualei product not found by id: " + id));
    }



    public List<HualeiProduct> findAll(Long warehouseId,
                                       String productId,
                                       String name,
                                       String description) {
        List<HualeiProduct> hualeiProducts = hualeiProductRepository.findAll(
                (Root<HualeiProduct> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) -> {
                    List<Predicate> predicates = new ArrayList<Predicate>();

                    predicates.add(criteriaBuilder.equal(root.get("warehouseId"), warehouseId));

                    if (StringUtils.isNotBlank(productId)) {

                        predicates.add(criteriaBuilder.equal(root.get("productId"), productId));
                    }
                    if (StringUtils.isNotBlank(name)) {
                        if (name.contains("*")) {

                            predicates.add(criteriaBuilder.like(root.get("name"), name.replaceAll("\\*", "%")));
                        }
                        else {

                            predicates.add(criteriaBuilder.equal(root.get("name"), name));
                        }

                    }
                    if (StringUtils.isNotBlank(description)) {
                        String descriptionComparator = description;
                        if (descriptionComparator.contains("*")) {
                            descriptionComparator.replaceAll("\\*", "%");
                        }
                        descriptionComparator = "%" + descriptionComparator + "%";

                        predicates.add(criteriaBuilder.like(root.get("description"), descriptionComparator));

                    }


                    Predicate[] p = new Predicate[predicates.size()];
                    return criteriaBuilder.and(predicates.toArray(p));
                }
                ,
                Sort.by(Sort.Direction.ASC, "name")
        );

        if (!hualeiProducts.isEmpty()) {
            loadAttribute(hualeiProducts);
        }
        return hualeiProducts;

    }


    public void loadAttribute(List<HualeiProduct> hualeiProducts) {
        for (HualeiProduct hualeiProduct : hualeiProducts) {
            loadAttribute(hualeiProduct);
        }
    }

    public void loadAttribute(HualeiProduct hualeiProduct) {
        if (Objects.nonNull(hualeiProduct.getCarrierId()) &&
               Objects.isNull(hualeiProduct.getCarrier()))  {
            hualeiProduct.setCarrier(
                    commonServiceRestemplateClient.getCarrierById(
                            hualeiProduct.getCarrierId()
                    )
            );
        }

        if (Objects.nonNull(hualeiProduct.getCarrierServiceLevelId()) &&
                Objects.isNull(hualeiProduct.getCarrierServiceLevel()))  {
            hualeiProduct.setCarrierServiceLevel(
                    commonServiceRestemplateClient.getCarrierServiceLevelById(
                            hualeiProduct.getCarrierServiceLevelId()
                    )
            );
        }
    }

    public HualeiProduct findByProductId(Long warehouseId, String productId) {

        HualeiProduct hualeiProduct =
                hualeiProductRepository.findByWarehouseIdAndProductId(warehouseId, productId);

        if (Objects.nonNull(hualeiProduct)) {
            loadAttribute(hualeiProduct);
        }
        return hualeiProduct;
    }

    public HualeiProduct save(HualeiProduct hualeiProduct) {
        return hualeiProductRepository.save(hualeiProduct);
    }

    public HualeiProduct saveOrUpdate(HualeiProduct hualeiProduct) {
        if (hualeiProduct.getId() == null &&
                findByProductId(hualeiProduct.getWarehouseId(), hualeiProduct.getProductId()) != null) {
            hualeiProduct.setId(
                    findByProductId(hualeiProduct.getWarehouseId(), hualeiProduct.getProductId()).getId());
        }
        return save(hualeiProduct);
    }


    public void delete(HualeiProduct hualeiProduct) {
        hualeiProductRepository.delete(hualeiProduct);
    }

    public void delete(Long id) {
        hualeiProductRepository.deleteById(id);
    }



    public HualeiProduct addHualeiProduct(HualeiProduct mould) {
        return saveOrUpdate(mould);
    }

    public HualeiProduct changeHualeiProduct(Long id, HualeiProduct mould) {
        return saveOrUpdate(mould);
    }
}
