/**
 * Copyright 2019
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

package com.garyzhangscm.cwms.resources.service;

import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import com.garyzhangscm.cwms.resources.model.Role;
import com.garyzhangscm.cwms.resources.repository.RoleRepository;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

@Service
public class RoleUserService implements TestDataInitiableService{
    private static final Logger logger = LoggerFactory.getLogger(RoleUserService.class);
    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private FileService fileService;

    @Value("${fileupload.test-data.roles:roles}")
    String testDataFile;

    public Role findById(Long id) {
        return roleRepository.findById(id).orElse(null);
    }

    public List<Role> findAll() {

        return roleRepository.findAll();

    }





    public Role save(Role role) {
        return roleRepository.save(role);
    }


    public List<Role> loadData(InputStream inputStream) throws IOException {

        CsvSchema schema = CsvSchema.builder().
                addColumn("name").
                addColumn("description").
                addColumn("enabled").
                build().withHeader();

        return fileService.loadData(inputStream, schema, Role.class);
    }

    public void initTestData(String warehouseName) {
        try {
            String testDataFileName = StringUtils.isBlank(warehouseName) ?
                    testDataFile + ".csv" :
                    testDataFile + "-" + warehouseName + ".csv";
            InputStream inputStream = new ClassPathResource(testDataFileName).getInputStream();
            List<Role> roles = loadData(inputStream);
            roles.stream().forEach(role -> save(role));
        } catch (IOException ex) {
            logger.debug("Exception while load test data: {}", ex.getMessage());
        }
    }

}
