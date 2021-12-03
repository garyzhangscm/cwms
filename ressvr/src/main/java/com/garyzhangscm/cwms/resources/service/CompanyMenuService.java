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

package com.garyzhangscm.cwms.resources.service;

import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import com.garyzhangscm.cwms.resources.clients.LayoutServiceRestemplateClient;
import com.garyzhangscm.cwms.resources.exception.ResourceNotFoundException;
import com.garyzhangscm.cwms.resources.model.CompanyMenu;
import com.garyzhangscm.cwms.resources.model.Menu;
import com.garyzhangscm.cwms.resources.model.MenuCSVWrapper;
import com.garyzhangscm.cwms.resources.model.Role;
import com.garyzhangscm.cwms.resources.repository.CompanyMenuRepository;
import com.garyzhangscm.cwms.resources.repository.MenuRepository;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

@Service
public class CompanyMenuService  {
    private static final Logger logger = LoggerFactory.getLogger(CompanyMenuService.class);
    @Autowired
    private CompanyMenuRepository companyMenuRepository;


    public CompanyMenu findById(Long id) {
        return companyMenuRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.raiseException("company menu  not found by id: " + id));
    }

    public List<CompanyMenu> findAll(Long companyId) {

        return companyMenuRepository.findByCompanyId(companyId);
    }


    public CompanyMenu findByCompanyIdAndMenu(Long companyId, Long menuId) {

        return companyMenuRepository.findByCompanyIdAndMenu(companyId, menuId);
    }
    public CompanyMenu save(CompanyMenu companyMenu) {
        return companyMenuRepository.save(companyMenu);
    }
    public CompanyMenu saveOrUpdate(CompanyMenu companyMenu) {
        if (Objects.isNull(companyMenu.getId()) &&
                Objects.nonNull(findByCompanyIdAndMenu(companyMenu.getCompanyId(), companyMenu.getMenu().getId()))) {
            companyMenu.setId(findByCompanyIdAndMenu(companyMenu.getCompanyId(), companyMenu.getMenu().getId()).getId()
            );
        }
        return save(companyMenu);
    }

    @Transactional
    public void processCompanyMenus(Long companyId, String assignedMenuIds, String deassignedMenuIds) {

        // get all menus that assigned to this company
        List<CompanyMenu> companyMenus = findAll(companyId);
        Map<Long, Long> assignedMenuMap = new HashMap<>();
        companyMenus.stream().forEach(
                companyMenu -> assignedMenuMap.put(companyMenu.getMenu().getId(), companyMenu.getMenu().getId())
        );


        if (!StringUtils.isBlank(assignedMenuIds)) {
            Arrays.stream(assignedMenuIds.split(","))
                    .mapToLong(Long::parseLong)
                    // for each menu that is not assigned yet
                    .filter(menuId -> !assignedMenuMap.containsKey(menuId))
                    .forEach(menuId -> {
                        companyMenuRepository.assignMenu(companyId, menuId);
                    });
        }
        if (!StringUtils.isBlank(deassignedMenuIds)) {
            Arrays.stream(deassignedMenuIds.split(","))
                    .mapToLong(Long::parseLong)
                    // for each menu that is already assigned yet
                    .filter(menuId -> assignedMenuMap.containsKey(menuId))
                    .forEach(menuId -> {
                        companyMenuRepository.deassignMenu(companyId, menuId);
                    });
        }

    }



}
