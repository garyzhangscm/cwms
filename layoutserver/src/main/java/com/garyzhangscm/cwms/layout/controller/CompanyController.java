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

package com.garyzhangscm.cwms.layout.controller;

import com.garyzhangscm.cwms.layout.ResponseBodyWrapper;
import com.garyzhangscm.cwms.layout.clients.CommonServiceRestemplateClient;
import com.garyzhangscm.cwms.layout.exception.MissingInformationException;
import com.garyzhangscm.cwms.layout.exception.RequestValidationFailException;
import com.garyzhangscm.cwms.layout.model.BillableEndpoint;
import com.garyzhangscm.cwms.layout.model.Client;
import com.garyzhangscm.cwms.layout.model.Company;
import com.garyzhangscm.cwms.layout.model.Warehouse;
import com.garyzhangscm.cwms.layout.service.CompanyService;
import com.garyzhangscm.cwms.layout.service.WarehouseService;
import com.garyzhangscm.cwms.layout.usercontext.UserContextHolder;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.swing.event.CaretListener;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@RestController
public class CompanyController {

    private static final Logger logger = LoggerFactory.getLogger(CompanyController.class);
    @Autowired
    CompanyService companyService;

    @Autowired
    HttpServletRequest request;

    @Autowired
    private CommonServiceRestemplateClient commonServiceRestemplateClient;


    @RequestMapping(value="/companies", method=RequestMethod.GET)
    public List<Company> listCompanies(
            @RequestParam(name = "code", required = false, defaultValue = "") String code,
            @RequestParam(name = "name", required = false, defaultValue = "") String name) {
/**
        if (Strings.isBlank(code) && Strings.isBlank(name)) {
            throw MissingInformationException.raiseException("code or name is required to get the company information");
        }
 **/
        return companyService.findAll(code, name);
    }


    @RequestMapping(value="/companies/{id}", method = RequestMethod.GET)
    public Company findCompanyByID(@PathVariable long id) {
        return companyService.findById(id);
    }


    @RequestMapping(value="/companies/validate", method = RequestMethod.GET)
    public Long validateCompanyCode(@RequestParam String code) {

        return companyService.findByCode(code) == null ? null : companyService.findByCode(code).getId();
    }



    @BillableEndpoint
    @RequestMapping(value="/companies", method = RequestMethod.POST)
    public Company addCompany(@RequestBody Company company) {

        return companyService.addCompany(company);
    }

    @RequestMapping(value="/companies/code/next", method = RequestMethod.GET)
    public ResponseBodyWrapper<String> getNextCompanyCode() {

        String nextCompanyCode =  companyService.getNextCompanyCode();
        logger.debug("next compay Code is {}", nextCompanyCode);
        return ResponseBodyWrapper.success(nextCompanyCode);
    }

    @RequestMapping(value="/companies/{id}/enable", method = RequestMethod.POST)
    @Caching(
            evict = {
                    @CacheEvict(cacheNames = "AdminService_Company", allEntries = true),
                    @CacheEvict(cacheNames = "CommonService_Company", allEntries = true),
                    @CacheEvict(cacheNames = "InboundService_Company", allEntries = true),
                    @CacheEvict(cacheNames = "IntegrationService_Company", allEntries = true),
                    @CacheEvict(cacheNames = "InventoryService_Company", allEntries = true),
                    @CacheEvict(cacheNames = "OutboundService_Company", allEntries = true),
                    @CacheEvict(cacheNames = "ResourceService_Company", allEntries = true),
                    @CacheEvict(cacheNames = "WorkOrderService_Company", allEntries = true),
                    @CacheEvict(cacheNames = "ZuulService_CompanyEnabled", allEntries = true),
                    @CacheEvict(cacheNames = "ZuulService_CompanyByWarehouseId", allEntries = true),
                    @CacheEvict(cacheNames = "ZuulService_Company", allEntries = true),
            }
    )
    public Company enableCompany(@PathVariable long id) {
        return companyService.enableCompany(id, true);
    }

    @RequestMapping(value="/companies/{id}/disable", method = RequestMethod.POST)
    @Caching(
            evict = {
                    @CacheEvict(cacheNames = "AdminService_Company", allEntries = true),
                    @CacheEvict(cacheNames = "CommonService_Company", allEntries = true),
                    @CacheEvict(cacheNames = "InboundService_Company", allEntries = true),
                    @CacheEvict(cacheNames = "IntegrationService_Company", allEntries = true),
                    @CacheEvict(cacheNames = "InventoryService_Company", allEntries = true),
                    @CacheEvict(cacheNames = "OutboundService_Company", allEntries = true),
                    @CacheEvict(cacheNames = "ResourceService_Company", allEntries = true),
                    @CacheEvict(cacheNames = "WorkOrderService_Company", allEntries = true),
                    @CacheEvict(cacheNames = "ZuulService_CompanyEnabled", allEntries = true),
                    @CacheEvict(cacheNames = "ZuulService_CompanyByWarehouseId", allEntries = true),
                    @CacheEvict(cacheNames = "ZuulService_Company", allEntries = true),
            }
    )
    public Company disableCompany(@PathVariable long id) {
        return companyService.enableCompany(id, false);
    }

    @RequestMapping(value="/companies/{id}/is-enabled", method = RequestMethod.GET)
    public Boolean isCompanyEnabled(@PathVariable long id) {
        return companyService.isCompanyEnabled(id);
    }

}
