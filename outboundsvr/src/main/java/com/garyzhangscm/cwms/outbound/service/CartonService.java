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
import com.garyzhangscm.cwms.outbound.clients.InventoryServiceRestemplateClient;
import com.garyzhangscm.cwms.outbound.clients.WarehouseLayoutServiceRestemplateClient;
import com.garyzhangscm.cwms.outbound.exception.OrderOperationException;
import com.garyzhangscm.cwms.outbound.exception.ResourceNotFoundException;
import com.garyzhangscm.cwms.outbound.model.*;
import com.garyzhangscm.cwms.outbound.model.Order;
import com.garyzhangscm.cwms.outbound.repository.CartonRepository;
import com.garyzhangscm.cwms.outbound.repository.OrderRepository;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import javax.persistence.criteria.*;
import javax.transaction.Transactional;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;


@Service
public class CartonService implements TestDataInitiableService {
    private static final Logger logger = LoggerFactory.getLogger(CartonService.class);

    @Autowired
    private CartonRepository cartonRepository;


    @Autowired
    private WarehouseLayoutServiceRestemplateClient warehouseLayoutServiceRestemplateClient;

    @Autowired
    private FileService fileService;

    @Value("${fileupload.test-data.cartons:cartons}")
    String testDataFile;

    public Carton findById(Long id) {
        return cartonRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.raiseException("carton not found by id: " + id));
    }

    public Carton save(Carton carton) {
        return cartonRepository.save(carton);
    }
    public Carton addCarton(Long warehouseId,
                            Carton carton) {
        if (Objects.isNull(carton.getWarehouseId())) {
            carton.setWarehouseId(warehouseId);
        }
        return saveOrUpdate(carton);
    }
    public Carton findByName(Long warehouseId, String name) {
        return cartonRepository.findByWarehouseIdAndName(warehouseId, name);
    }

    public List<Carton> findAll(Long warehouseId,
                                String name,
                                Boolean enabled,
                                Boolean pickingCartonFlag,
                                Boolean shippingCartonFlag) {

        List<Carton> cartons =  cartonRepository.findAll(
                (Root<Carton> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) -> {
                    List<Predicate> predicates = new ArrayList<Predicate>();
                    predicates.add(criteriaBuilder.equal(root.get("warehouseId"), warehouseId));

                    if (StringUtils.isNotBlank(name)) {
                        predicates.add(criteriaBuilder.equal(root.get("name"), name));

                    }
                    if (Objects.nonNull(enabled)) {
                        predicates.add(criteriaBuilder.equal(root.get("enabled"), enabled));

                    }
                    if (Objects.nonNull(pickingCartonFlag)) {
                        predicates.add(criteriaBuilder.equal(root.get("pickingCartonFlag"), pickingCartonFlag));

                    }
                    if (Objects.nonNull(shippingCartonFlag)) {
                        predicates.add(criteriaBuilder.equal(root.get("shippingCartonFlag"), shippingCartonFlag));

                    }

                    Predicate[] p = new Predicate[predicates.size()];
                    return criteriaBuilder.and(predicates.toArray(p));
                }
        );

        return cartons;
    }

    public List<Carton> findEnabledCarton(Long warehouseId) {
        return findAll(warehouseId, "", true, null, null);
    }


    public Carton saveOrUpdate(Carton carton) {
        if (Objects.isNull(carton.getId()) &&
                Objects.nonNull(findByName(carton.getWarehouseId(),carton.getName()))) {
            carton.setId(findByName(carton.getWarehouseId(),carton.getName()).getId());
        }
        return save(carton);
    }



    public List<CartonCSVWrapper> loadData(InputStream inputStream) throws IOException {

        CsvSchema schema = CsvSchema.builder().
                addColumn("warehouse").
                addColumn("name").
                addColumn("length").
                addColumn("width").
                addColumn("height").
                addColumn("fillRate").
                addColumn("enabled").
                addColumn("shippingCartonFlag").
                addColumn("pickingCartonFlag").
                build().withHeader();

        return fileService.loadData(inputStream, schema, CartonCSVWrapper.class);
    }

    public void initTestData(String warehouseName) {
        try {
            String testDataFileName = StringUtils.isBlank(warehouseName) ?
                    testDataFile + ".csv" :
                    testDataFile + "-" + warehouseName + ".csv";
            InputStream inputStream = new ClassPathResource(testDataFileName).getInputStream();
            List<CartonCSVWrapper> cartonCSVWrappers = loadData(inputStream);
            cartonCSVWrappers.stream().forEach(cartonCSVWrapper -> saveOrUpdate(convertFromWrapper(cartonCSVWrapper)));
        } catch (IOException ex) {
            logger.debug("Exception while load test data: {}", ex.getMessage());
        }
    }

    private Carton convertFromWrapper(CartonCSVWrapper cartonCSVWrapper) {

        Carton carton = new Carton();
        carton.setName(cartonCSVWrapper.getName());

        Warehouse warehouse = warehouseLayoutServiceRestemplateClient.getWarehouseByName(cartonCSVWrapper.getWarehouse());
        carton.setWarehouseId(warehouse.getId());

        carton.setHeight(cartonCSVWrapper.getHeight());
        carton.setWidth(cartonCSVWrapper.getWidth());
        carton.setLength(cartonCSVWrapper.getLength());
        carton.setFillRate(cartonCSVWrapper.getFillRate());

        carton.setEnabled(cartonCSVWrapper.getEnabled());

        carton.setPickingCartonFlag(cartonCSVWrapper.getPickingCartonFlag());
        carton.setShippingCartonFlag(cartonCSVWrapper.getShippingCartonFlag());

        return  carton;

    }

    /**
     * Get the next size carton that bigger than the current carton
     * @param carton current carton
     * @return the next size carton
     */
    public Carton getNextSizeCarton(Long warehouseId, Carton carton) {
        List<Carton> cartons = findEnabledCarton(warehouseId);
        if (cartons.size() == 0) {
            return null;
        }
        return cartons.stream().
                filter(existingCarton -> existingCarton.getTotalSpace() > carton.getTotalSpace()).
                sorted(Comparator.comparingDouble(Carton::getTotalSpace)).findFirst().orElse(null);

    }


    public Carton getBestCarton(Pick pick) {
        List<Carton> cartons = findEnabledCarton(pick.getWarehouseId());
        if (cartons.size() == 0) {
            return null;
        }
        return cartons.stream().
                filter(carton -> carton.getTotalSpace() > pick.getSize()).
                sorted(Comparator.comparingDouble(Carton::getTotalSpace)).findFirst().orElse(null);
    }

}
