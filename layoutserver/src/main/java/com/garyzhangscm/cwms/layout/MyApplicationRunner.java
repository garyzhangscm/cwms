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

package com.garyzhangscm.cwms.layout;

import com.garyzhangscm.cwms.layout.model.Company;
import com.garyzhangscm.cwms.layout.service.CompanyService;
import com.garyzhangscm.cwms.layout.service.LocationGroupTypeService;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.ResourcePatternUtils;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;


@Component
public class MyApplicationRunner implements ApplicationRunner {
    // WE will init the menu when start the application

    private static final Logger logger = LoggerFactory.getLogger(MyApplicationRunner.class);
    @Autowired
    LocationGroupTypeService locationGroupTypeService;

    @Autowired
    private CompanyService companyService;


    @Override
    public void run(ApplicationArguments args) throws Exception {

        initLocationGroupTypeData();

        initCompanyAPIKeyAndSecret();


    }

    /**
     * Init company API key and secret, only if the company
     * doesn't have the api key or secret yet
     */
    private void initCompanyAPIKeyAndSecret() {
        List<Company> companies = companyService.findAll(null, null);

        for (Company company : companies) {
            if (Strings.isBlank(company.getApiKey())) {
                company.setApiKey(
                        companyService.generateAPIKey()
                );
            }
            if (Strings.isBlank(company.getApiSecret())) {
                company.setApiSecret(
                        companyService.generateAPISecret()
                );
            }

            companyService.saveOrUpdate(company);

        }
    }


    private void initLocationGroupTypeData() throws IOException {
        System.out.println("Start to init the location group type");
        locationGroupTypeService.initTestData(null, "");




    }


}
