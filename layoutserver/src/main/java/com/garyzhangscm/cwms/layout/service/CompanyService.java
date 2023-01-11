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

package com.garyzhangscm.cwms.layout.service;

import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import com.garyzhangscm.cwms.layout.exception.ResourceNotFoundException;
import com.garyzhangscm.cwms.layout.model.Company;
import com.garyzhangscm.cwms.layout.repository.CompanyRepository;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.criteria.*;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
public class CompanyService implements TestDataInitiableService{

    private static final Logger logger = LoggerFactory.getLogger(CompanyService.class);

    @Autowired
    private CompanyRepository companyRepository;
    @Autowired
    private FileService fileService;

    @Value("${fileupload.test-data.companies:companies}")
    String testDataFile;

    public Company findById(Long id) {
        return companyRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.raiseException("Company not found by id: " + id));
    }

    public List<Company> findAll(String code, String name) {
        return companyRepository.findAll(
                (Root<Company> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) -> {
                    List<Predicate> predicates = new ArrayList<Predicate>();


                    if (StringUtils.isNotBlank(code)) {
                        predicates.add(criteriaBuilder.equal(root.get("code"), code));
                    }
                    if (StringUtils.isNotBlank(name)) {
                        predicates.add(criteriaBuilder.equal(root.get("name"), name));
                    }
                    Predicate[] p = new Predicate[predicates.size()];
                    return criteriaBuilder.and(predicates.toArray(p));
                }
        );
    }

    public Company findByCode(String code){
        return companyRepository.findByCode(code);
    }

    @Transactional
    public Company save(Company company) {
        return companyRepository.save(company);
    }
    // Save when the client's name doesn't exists
    // update when the client already exists
    @Transactional
    public Company saveOrUpdate(Company company) {
        if (company.getId() == null && findByCode(company.getCode()) != null) {
            company.setId(findByCode(company.getCode()).getId());
        }
        return save(company);
    }

    @Transactional
    public void delete(Company company) {
        companyRepository.delete(company);
    }
    @Transactional
    public void delete(Long id) {
        companyRepository.deleteById(id);
    }



    @Transactional
    public List<Company> loadData(String fileName) throws IOException {
        return loadData(new File(fileName));
    }

    @Transactional
    public List<Company> loadData(File file) throws IOException {

        CsvSchema schema = CsvSchema.builder().
                addColumn("code").
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

        return fileService.loadData(file, schema, Company.class);
    }

    @Transactional
    public List<Company> loadData(InputStream inputStream) throws IOException {

        CsvSchema schema = CsvSchema.builder().
                addColumn("code").
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

        return fileService.loadData(inputStream, schema, Company.class);
    }

    @Transactional
    public void initTestData(Long companyId, String warehouseName) {
        try {
            String testDataFileName = StringUtils.isBlank(warehouseName) ?
                    testDataFile + ".csv" :
                    testDataFile + "-" + warehouseName + ".csv";
            InputStream inputStream = new ClassPathResource(testDataFileName).getInputStream();
            List<Company> companies = loadData(inputStream);
            companies.stream().forEach(company -> saveOrUpdate(company));
        }
        catch(IOException ex) {
            logger.debug("Exception while load test data: {}", ex.getMessage());
        }
    }

    public Company addCompany(Company company) {
        return saveOrUpdate(company);
    }


    public String getNextCompanyCode() {
        // we will always assume the company code is in format of a number
        String maxCompanyCode = companyRepository.getMaxCompanyCode();
        if (StringUtils.isBlank(maxCompanyCode)) {
            return "10000";
        }
        return String.valueOf(Integer.parseInt(maxCompanyCode) + 1);

    }

}
