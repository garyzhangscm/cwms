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
import com.garyzhangscm.cwms.common.controller.TestDataInitController;
import com.garyzhangscm.cwms.common.model.Client;
import com.garyzhangscm.cwms.common.model.UnitOfMeasure;
import com.garyzhangscm.cwms.common.repository.ClientRepository;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

@Service
public class ClientService implements  TestDataInitiableService{

    private static final Logger logger = LoggerFactory.getLogger(ClientService.class);

    @Autowired
    private ClientRepository clientRepository;
    @Autowired
    private FileService fileService;

    @Value("${fileupload.test-data.clients:clients}")
    String testDataFile;

    public Client findById(Long id) {
        return clientRepository.findById(id).orElse(null);
    }

    public List<Client> findAll(String name) {

        if (StringUtils.isBlank(name)) {
            return clientRepository.findAll();
        }
        else {
            return Arrays.asList(new Client[]{findByName(name)});
        }
    }

    public Client findByName(String name){
        return clientRepository.findByName(name);
    }

    public Client save(Client client) {
        logger.debug("Start to save client: {}", client.getName());

        return clientRepository.save(client);
    }
    // Save when the client's name doesn't exists
    // update when the client already exists
    public Client saveOrUpdate(Client client) {
        if (client.getId() == null && findByName(client.getName()) != null) {
            client.setId(findByName(client.getName()).getId());
        }
        return save(client);
    }

    public void delete(Client client) {
        clientRepository.delete(client);
    }
    public void delete(Long id) {
        clientRepository.deleteById(id);
    }

    public void delete(String clientIds) {
        // remove a list of location groups based upon the id passed in
        if (!clientIds.isEmpty()) {
            long[] clientIdArray = Arrays.asList(clientIds.split(",")).stream().mapToLong(Long::parseLong).toArray();
            for(long id : clientIdArray) {
                delete(id);
            }
        }
    }

    public List<Client> loadData(String fileName) throws IOException {
        return loadData(new File(fileName));
    }

    public List<Client> loadData(File file) throws IOException {

        CsvSchema schema = CsvSchema.builder().
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

        return fileService.loadData(file, schema, Client.class);
    }

    public List<Client> loadData(InputStream inputStream) throws IOException {

        CsvSchema schema = CsvSchema.builder().
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

        return fileService.loadData(inputStream, schema, Client.class);
    }

    public void initTestData(String warehouseName) {
        try {
            String testDataFileName = StringUtils.isBlank(warehouseName) ?
                    testDataFile + ".csv" :
                    testDataFile + "-" + warehouseName + ".csv";
            InputStream inputStream = new ClassPathResource(testDataFileName).getInputStream();
            List<Client> clients = loadData(inputStream);
            clients.stream().forEach(client -> saveOrUpdate(client));
        }
        catch(IOException ex) {
            logger.debug("Exception while load test data: {}", ex.getMessage());
        }
    }
}
