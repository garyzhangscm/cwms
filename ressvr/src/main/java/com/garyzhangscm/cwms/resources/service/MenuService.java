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
import com.garyzhangscm.cwms.resources.model.Menu;
import com.garyzhangscm.cwms.resources.model.MenuCSVWrapper;
import com.garyzhangscm.cwms.resources.model.MenuSubGroup;
import com.garyzhangscm.cwms.resources.model.MenuSubGroupCSVWrapper;
import com.garyzhangscm.cwms.resources.repository.MenuRepository;
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
import java.util.Objects;

@Service
public class MenuService  implements TestDataInitiableService{
    private static final Logger logger = LoggerFactory.getLogger(MenuService.class);
    @Autowired
    private MenuRepository menuRepository;

    @Autowired
    private MenuSubGroupService menuSubGroupService;
    @Autowired
    private LayoutServiceRestemplateClient layoutServiceRestemplateClient;

    @Autowired
    private FileService fileService;
    @Value("${fileupload.test-data.menus:menus}")
    String testDataFile;

    public Menu findById(Long id) {
        return menuRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.raiseException("menu  not found by id: " + id));
    }

    public List<Menu> findAll() {

        return menuRepository.findAll();
    }
    public Menu save(Menu menu) {
        return menuRepository.save(menu);
    }
    public Menu saveOrUpdate(Menu menu) {
        if (Objects.nonNull(findByName(menu.getName()))) {
            menu.setId(
                    findByName(menu.getName()).getId()
            );
        }
        return menuRepository.save(menu);
    }
    public Menu findByName(String name) {
        return menuRepository.findByName(name);
    }



    public List<MenuCSVWrapper> loadData(InputStream inputStream) throws IOException {

        CsvSchema schema = CsvSchema.builder().
                addColumn("text").
                addColumn("i18n").
                addColumn("link").
                addColumn("sequence").
                addColumn("menuSubGroup").
                addColumn("name").
                addColumn("icon").
                build().withHeader();

        return fileService.loadData(inputStream, schema, MenuCSVWrapper.class);
    }

    public void initTestData(Long companyId, String warehouseName) {
        try {
            String companyCode = companyId == null ?
                    "" : layoutServiceRestemplateClient.getCompanyById(companyId).getCode();

            String testDataFileName = StringUtils.isBlank(warehouseName) ?
                    testDataFile + ".csv" :
                    testDataFile + "-" + companyCode + "-" + warehouseName + ".csv";
            InputStream inputStream = new ClassPathResource(testDataFileName).getInputStream();
            List<MenuCSVWrapper> menuCSVWrappers = loadData(inputStream);
            menuCSVWrappers.stream().forEach(menuCSVWrapper -> saveOrUpdate(convertFromCSVWrapper(menuCSVWrapper)));
        } catch (IOException ex) {
            logger.debug("Exception while load test data: {}", ex.getMessage());
        }
    }

    private Menu convertFromCSVWrapper(MenuCSVWrapper menuCSVWrapper) {
        Menu menu = new Menu();
        menu.setText(menuCSVWrapper.getText());
        menu.setI18n(menuCSVWrapper.getI18n());
        menu.setLink(menuCSVWrapper.getLink());
        menu.setSequence(menuCSVWrapper.getSequence());
        menu.setName(menuCSVWrapper.getName());
        menu.setIcon(menuCSVWrapper.getIcon());

        // Setup the parent menu group for this subgroup
        menu.setMenuSubGroup(menuSubGroupService.findByName(menuCSVWrapper.getMenuSubGroup()));
        return menu;


    }

    public Menu getMenuByUrl(String url) {
        List<Menu> menus = findAll();
        return
                menus.stream()
                        .filter(menu -> menu.getLink().equals(url))
                        .findFirst()
                        .orElseThrow(() -> ResourceNotFoundException.raiseException("Menu not found for url: " + url));
    }



}
