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
import com.garyzhangscm.cwms.outbound.model.*;
import com.garyzhangscm.cwms.outbound.repository.OrderLineRepository;
import com.garyzhangscm.cwms.outbound.repository.OrderRepository;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

@Service
public class OrderLineService implements TestDataInitiableService{
    private static final Logger logger = LoggerFactory.getLogger(OrderLineService.class);

    @Autowired
    private OrderLineRepository orderLineRepository;

    @Autowired
    private OrderService orderService;

    @Autowired
    private CommonServiceRestemplateClient commonServiceRestemplateClient;
    @Autowired
    private InventoryServiceRestemplateClient inventoryServiceRestemplateClient;
    @Autowired
    private WarehouseLayoutServiceRestemplateClient warehouseLayoutServiceRestemplateClient;
    @Autowired
    private FileService fileService;

    @Value("${fileupload.test-data.order_lines:order_lines.csv}")
    String testDataFile;

    public OrderLine findById(Long id) {
        return findById(id, true);
    }

    public OrderLine findById(Long id, boolean includeDetails) {
        OrderLine orderLine = orderLineRepository.findById(id).orElse(null);
        if (orderLine != null && includeDetails) {
            loadOrderLineAttribute(orderLine);
        }
        return orderLine;
    }
    public List<OrderLine> findAll() {
        return findAll(true);
    }
    public List<OrderLine> findAll(boolean includeDetails) {


        List<OrderLine> orderLines = orderLineRepository.findAll();
        if (orderLines.size() > 0 && includeDetails) {
            loadOrderLineAttribute(orderLines);
        }
        return orderLines;
    }

    public OrderLine findByNaturalKey(Long orderId, String number) {
        return orderLineRepository.findByNaturalKey(orderId, number);

    }


    public void loadOrderLineAttribute(List<OrderLine> orderLines) {
        for(OrderLine orderLine : orderLines) {
            loadOrderLineAttribute(orderLine);
        }
    }

    public void loadOrderLineAttribute(OrderLine orderLine) {

        // Load Item information
        if (orderLine.getItemId() != null && orderLine.getItem() == null) {
            orderLine.setItem(inventoryServiceRestemplateClient.getItemById(orderLine.getItemId()));

        }

    }

    public OrderLine save(OrderLine orderLine) {
        return orderLineRepository.save(orderLine);
    }

    public OrderLine saveOrUpdate(OrderLine orderLine) {

        if (orderLine.getId() == null && findByNaturalKey(orderLine.getOrder().getId(), orderLine.getNumber()) != null) {
            orderLine.setId(findByNaturalKey(orderLine.getOrder().getId(), orderLine.getNumber()).getId());
        }
        return save(orderLine);
    }

    public void delete(OrderLine orderLine) {
        orderLineRepository.delete(orderLine);
    }
    public void delete(Long id) {
        orderLineRepository.deleteById(id);
    }
    public void delete(String orderLineIds) {
        if (!orderLineIds.isEmpty()) {
            long[] orderLineIdArray = Arrays.asList(orderLineIds.split(",")).stream().mapToLong(Long::parseLong).toArray();
            for(long id : orderLineIdArray) {
                delete(id);
            }
        }
    }

    public List<OrderLineCSVWrapper> loadData(InputStream inputStream) throws IOException {

        CsvSchema schema = CsvSchema.builder().
                addColumn("order").
                addColumn("number").
                addColumn("item").
                addColumn("expectedQuantity").
                addColumn("inventoryStatus").
                build().withHeader();

        return fileService.loadData(inputStream, schema, OrderLineCSVWrapper.class);
    }

    public void initTestData() {
        try {
            InputStream inputStream = new ClassPathResource(testDataFile).getInputStream();
            List<OrderLineCSVWrapper> orderLineCSVWrappers = loadData(inputStream);
            orderLineCSVWrappers.stream().forEach(orderLineCSVWrapper -> saveOrUpdate(convertFromWrapper(orderLineCSVWrapper)));
        } catch (IOException ex) {
            logger.debug("Exception while load test data: {}", ex.getMessage());
        }
    }

    private OrderLine convertFromWrapper(OrderLineCSVWrapper orderLineCSVWrapper) {

        OrderLine orderLine = new OrderLine();
        orderLine.setNumber(orderLineCSVWrapper.getNumber());
        orderLine.setExpectedQuantity(orderLineCSVWrapper.getExpectedQuantity());
        orderLine.setShippedQuantity(0L);

        if (!StringUtils.isBlank(orderLineCSVWrapper.getOrder())) {
            Order order = orderService.findByNumber(orderLineCSVWrapper.getOrder());
            orderLine.setOrder(order);
        }
        if (!StringUtils.isBlank(orderLineCSVWrapper.getItem())) {
            Item item = inventoryServiceRestemplateClient.getItemByName(orderLineCSVWrapper.getItem());
            orderLine.setItemId(item.getId());
        }
        if (!StringUtils.isBlank(orderLineCSVWrapper.getInventoryStatus())) {
            InventoryStatus inventoryStatus = inventoryServiceRestemplateClient.getInventoryStatusByName(orderLineCSVWrapper.getInventoryStatus());
            orderLine.setInventoryStatusId(inventoryStatus.getId());
        }
        return orderLine;
    }
}
