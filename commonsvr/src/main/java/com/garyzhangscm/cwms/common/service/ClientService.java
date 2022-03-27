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

package com.garyzhangscm.cwms.common.service;

import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import com.garyzhangscm.cwms.common.clients.WarehouseLayoutServiceRestemplateClient;
import com.garyzhangscm.cwms.common.exception.ResourceNotFoundException;
import com.garyzhangscm.cwms.common.model.*;
import com.garyzhangscm.cwms.common.repository.ClientRepository;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.criteria.*;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

@Service
public class ClientService implements  TestDataInitiableService{

    private static final Logger logger = LoggerFactory.getLogger(ClientService.class);

    @Autowired
    private ClientRepository clientRepository;
    @Autowired
    private FileService fileService;
    @Autowired
    private WarehouseLayoutServiceRestemplateClient warehouseLayoutServiceRestemplateClient;

    @Value("${fileupload.test-data.clients:clients}")
    String testDataFile;

    public Client findById(Long id) {
        return clientRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.raiseException("client not found by id: " + id));
    }

    public List<Client> findAll(Long companyId, Long warehouseId,
                                String name) {
        List<Client> clients = clientRepository.findAll(
                (Root<Client> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) -> {
                    List<Predicate> predicates = new ArrayList<Predicate>();

                    predicates.add(criteriaBuilder.equal(root.get("companyId"), companyId));
                    if (StringUtils.isNotBlank(name)) {
                        if (name.contains("%")) {
                            predicates.add(criteriaBuilder.like(root.get("name"), name));
                        }
                        else {
                            predicates.add(criteriaBuilder.equal(root.get("name"), name));
                        }
                    }
                    Predicate[] p = new Predicate[predicates.size()];

                    // special handling for warehouse id
                    // if warehouse id is passed in, then return both the warehouse level item
                    // and the company level item information.
                    // otherwise, return the company level item information
                    Predicate predicate = criteriaBuilder.and(predicates.toArray(p));
                    if (Objects.nonNull(warehouseId)) {
                        return criteriaBuilder.and(predicate,
                                criteriaBuilder.or(
                                        criteriaBuilder.equal(root.get("warehouseId"), warehouseId),
                                        criteriaBuilder.isNull(root.get("warehouseId"))));
                    }
                    else  {
                        return criteriaBuilder.and(predicate,criteriaBuilder.isNull(root.get("warehouseId")));
                    }
                }
                ,
                Sort.by(Sort.Direction.ASC, "warehouseId", "name")
        );

        // we may get duplicated record from the above query when we pass in the warehouse id
        // if so, we may need to remove the company level item if we have the warehouse level item
        if (Objects.nonNull(warehouseId)) {
            removeDuplicatedRecords(clients);
        }
        return clients;
    }
    /**
     * Remove teh duplicated clients record. If we have 2 record with the same clients name
     * but different warehouse, then we will remove the one without any warehouse information
     * from the result
     * @param clients
     */
    private void removeDuplicatedRecords(List<Client> clients) {
        Iterator<Client> clientIterator = clients.listIterator();
        Set<String> clientProcessed = new HashSet<>();
        while(clientIterator.hasNext()) {
            Client client = clientIterator.next();

            if (clientProcessed.contains(client.getName()) &&
                    Objects.isNull(client.getWarehouseId())) {
                // ok, we already processed the item and the current
                // record is a company level item, then we will remove
                // this record from the result
                clientIterator.remove();
            }
            clientProcessed.add(client.getName());
        }
    }

    public Client findByName(Long warehouseId, String name){
        return clientRepository.findByName(warehouseId, name);
    }

    @Transactional
    public Client save(Client client) {
        return clientRepository.save(client);
    }
    // Save when the client's name doesn't exists
    // update when the client already exists
    @Transactional
    public Client saveOrUpdate(Client client) {
        if (client.getId() == null && findByName(client.getWarehouseId(), client.getName()) != null) {
            client.setId(findByName(client.getWarehouseId(), client.getName()).getId());
        }
        return save(client);
    }

    @Transactional
    public void delete(Client client) {
        clientRepository.delete(client);
    }
    @Transactional
    public void delete(Long id) {
        clientRepository.deleteById(id);
    }

    @Transactional
    public void delete(String clientIds) {
        // remove a list of location groups based upon the id passed in
        if (!clientIds.isEmpty()) {
            long[] clientIdArray = Arrays.asList(clientIds.split(",")).stream().mapToLong(Long::parseLong).toArray();
            for(long id : clientIdArray) {
                delete(id);
            }
        }
    }

    public List<ClientCSVWrapper> loadData(String fileName) throws IOException {
        return loadData(new File(fileName));
    }

    public List<ClientCSVWrapper> loadData(File file) throws IOException {

        CsvSchema schema = CsvSchema.builder().
                addColumn("company").
                addColumn("warehouse").
                addColumn("name").
                addColumn("description").
                addColumn("contactorFirstname").
                addColumn("contactorLastname").
                addColumn("addressCountry").
                addColumn("addressState").
                addColumn("addressCounty").
                addColumn("addressCity").
                addColumn("addressDistrict").
                addColumn("addressLine1").
                addColumn("addressLine2").
                addColumn("addressPostcode").
                build().withHeader();

        return fileService.loadData(file, schema, ClientCSVWrapper.class);
    }

    public List<ClientCSVWrapper> loadData(InputStream inputStream) throws IOException {

        CsvSchema schema = CsvSchema.builder().
                addColumn("company").
                addColumn("warehouse").
                addColumn("name").
                addColumn("description").
                addColumn("contactorFirstname").
                addColumn("contactorLastname").
                addColumn("addressCountry").
                addColumn("addressState").
                addColumn("addressCounty").
                addColumn("addressCity").
                addColumn("addressDistrict").
                addColumn("addressLine1").
                addColumn("addressLine2").
                addColumn("addressPostcode").
                build().withHeader();

        return fileService.loadData(inputStream, schema, ClientCSVWrapper.class);
    }

    @Transactional
    public void initTestData(Long companyId, String warehouseName) {
        try {
            String companyCode = warehouseLayoutServiceRestemplateClient.getCompanyById(companyId).getCode();

            String testDataFileName = StringUtils.isBlank(warehouseName) ?
                    testDataFile + ".csv" :
                    testDataFile + "-" + companyCode + "-" + warehouseName + ".csv";
            InputStream inputStream = new ClassPathResource(testDataFileName).getInputStream();
            List<ClientCSVWrapper> clientCSVWrappers = loadData(inputStream);
            clientCSVWrappers.stream().forEach(clientCSVWrapper -> saveOrUpdate(convertFromWrapper(clientCSVWrapper)));
        }
        catch(IOException ex) {
            logger.debug("Exception while load test data: {}", ex.getMessage());
        }
    }

    private Client convertFromWrapper(ClientCSVWrapper clientCSVWrapper) {
        Client client = new Client();

        BeanUtils.copyProperties(clientCSVWrapper, client);

        Warehouse warehouse =warehouseLayoutServiceRestemplateClient.getWarehouseByName(
                clientCSVWrapper.getCompany(), clientCSVWrapper.getWarehouse()
        );
        client.setWarehouseId(warehouse.getId());
        return client;

    }

}
