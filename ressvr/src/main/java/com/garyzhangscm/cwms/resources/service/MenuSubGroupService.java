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
import com.garyzhangscm.cwms.resources.exception.ResourceNotFoundException;
import com.garyzhangscm.cwms.resources.model.*;
import com.garyzhangscm.cwms.resources.repository.MenuGroupRepository;
import com.garyzhangscm.cwms.resources.repository.MenuSubGroupRepository;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

@Service
public class MenuSubGroupService implements TestDataInitiableService{
    private static final Logger logger = LoggerFactory.getLogger(MenuSubGroupService.class);

    @Autowired
    private MenuSubGroupRepository menuSubGroupRepository;
    @Autowired
    private MenuGroupService menuGroupService;

    @Autowired
    private FileService fileService;

    @Value("${fileupload.test-data.menu-sub-groups:menu-sub-groups}")
    String testDataFile;

    public MenuSubGroup findById(Long id) {
        return menuSubGroupRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.raiseException("menu sub group not found by id: " + id));
    }

    public List<MenuSubGroup> findAll() {

        return menuSubGroupRepository.findAll();
    }

    public MenuSubGroup findByName(String name) {

        return menuSubGroupRepository.findByName(name);
    }
    public MenuSubGroup save(MenuSubGroup menuSubGroup) {
        return menuSubGroupRepository.save(menuSubGroup);
    }


    public List<MenuSubGroupCSVWrapper> loadData(InputStream inputStream) throws IOException {

        CsvSchema schema = CsvSchema.builder().
                addColumn("text").
                addColumn("i18n").
                addColumn("icon").
                addColumn("shortcutRoot").
                addColumn("sequence").
                addColumn("link").
                addColumn("badge").
                addColumn("menuGroup").
                addColumn("name").
                build().withHeader();

        return fileService.loadData(inputStream, schema, MenuSubGroupCSVWrapper.class);
    }

    public void initTestData(String warehouseName) {
        try {
            String testDataFileName = StringUtils.isBlank(warehouseName) ?
                    testDataFile + ".csv" :
                    testDataFile + "-" + warehouseName + ".csv";
            InputStream inputStream = new ClassPathResource(testDataFileName).getInputStream();
            List<MenuSubGroupCSVWrapper> menuSubGroupCSVWrappers = loadData(inputStream);
            menuSubGroupCSVWrappers.stream().forEach(menuSubGroupCSVWrapper -> save(convertFromCSVWrapper(menuSubGroupCSVWrapper)));
        } catch (IOException ex) {
            logger.debug("Exception while load test data: {}", ex.getMessage());
        }
    }

    private MenuSubGroup convertFromCSVWrapper(MenuSubGroupCSVWrapper menuSubGroupCSVWrapper) {
        MenuSubGroup menuSubGroup = new MenuSubGroup();
        menuSubGroup.setText(menuSubGroupCSVWrapper.getText());
        menuSubGroup.setI18n(menuSubGroupCSVWrapper.getI18n());
        menuSubGroup.setIcon(menuSubGroupCSVWrapper.getIcon());
        menuSubGroup.setBadge(menuSubGroupCSVWrapper.getBadge());
        menuSubGroup.setLink(menuSubGroupCSVWrapper.getLink());
        menuSubGroup.setName(menuSubGroupCSVWrapper.getName());
        menuSubGroup.setSequence(menuSubGroupCSVWrapper.getSequence());
        menuSubGroup.setShortcutRoot(menuSubGroupCSVWrapper.getShortcutRoot());

        // Setup the parent menu group for this subgroup
        menuSubGroup.setMenuGroup(menuGroupService.findByName(menuSubGroupCSVWrapper.getMenuGroup()));
        return menuSubGroup;


    }
}
