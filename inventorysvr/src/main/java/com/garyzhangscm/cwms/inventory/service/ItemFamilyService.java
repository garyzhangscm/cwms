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

package com.garyzhangscm.cwms.inventory.service;

import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import com.garyzhangscm.cwms.inventory.model.Item;
import com.garyzhangscm.cwms.inventory.model.ItemFamily;
import com.garyzhangscm.cwms.inventory.repository.ItemFamilyRepository;
import com.garyzhangscm.cwms.inventory.repository.ItemRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

@Service
public class ItemFamilyService implements TestDataInitiableService{

    private static final Logger logger = LoggerFactory.getLogger(ItemFamilyService.class);

    @Autowired
    private ItemFamilyRepository itemFamilyRepository;
    @Autowired
    private FileService fileService;

    @Value("${fileupload.test-data.item-families:item_families.csv}")
    String testDataFile;

    public ItemFamily findById(Long id) {
        return itemFamilyRepository.findById(id).orElse(null);
    }

    public List<ItemFamily> findAll() {

        return itemFamilyRepository.findAll();
    }

    public ItemFamily findByName(String name){
        return itemFamilyRepository.findByName(name);
    }

    public ItemFamily save(ItemFamily itemFamily) {
        return itemFamilyRepository.save(itemFamily);
    }

    public ItemFamily saveOrUpdate(ItemFamily itemFamily) {
        if (findByName(itemFamily.getName()) != null) {
            itemFamily.setId(findByName(itemFamily.getName()).getId());
        }
        return save(itemFamily);
    }
    public void delete(ItemFamily itemFamily) {
        itemFamilyRepository.delete(itemFamily);
    }
    public void delete(Long id) {
        itemFamilyRepository.deleteById(id);
    }


    public List<ItemFamily> loadData(File file) throws IOException {

        CsvSchema schema = CsvSchema.builder().
                addColumn("name").
                addColumn("description").
                build().withHeader();
        return fileService.loadData(file, schema, ItemFamily.class);
    }
    public List<ItemFamily> loadData(InputStream inputStream) throws IOException {

        CsvSchema schema = CsvSchema.builder().
                addColumn("name").
                addColumn("description").
                build().withHeader();

        return fileService.loadData(inputStream, schema, ItemFamily.class);
    }

    public void initTestData() {
        try {
            InputStream inputStream = new ClassPathResource(testDataFile).getInputStream();
            List<ItemFamily> itemFamilies = loadData(inputStream);
            itemFamilies.stream().forEach(itemFamily -> saveOrUpdate(itemFamily));
        } catch (IOException ex) {
            logger.debug("Exception while load test data: {}", ex.getMessage());
        }
    }
}
