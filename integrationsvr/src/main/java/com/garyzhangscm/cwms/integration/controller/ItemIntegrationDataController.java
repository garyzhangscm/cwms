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

package com.garyzhangscm.cwms.integration.controller;


import com.garyzhangscm.cwms.integration.ResponseBodyWrapper;
import com.garyzhangscm.cwms.integration.model.*;
import com.garyzhangscm.cwms.integration.service.IntegrationDataService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;


@RestController
@RequestMapping(value = "/integration-data")
public class ItemIntegrationDataController {


    private static final Logger logger = LoggerFactory.getLogger(ItemIntegrationDataController.class);

    @Autowired
    private IntegrationDataService integrationDataService;
 
    //
    // Integration - Item
    //
    @RequestMapping(value="/items", method = RequestMethod.GET)
    public List<? extends IntegrationItemData> getIntegrationItemData(
            @RequestParam String companyCode,
            @RequestParam(name = "warehouseId", required = false, defaultValue = "")  Long warehouseId,
            @RequestParam(name = "startTime", required = false, defaultValue = "") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)  ZonedDateTime startTime,
            @RequestParam(name = "endTime", required = false, defaultValue = "") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)  ZonedDateTime endTime,
            @RequestParam(name = "date", required = false, defaultValue = "") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)  LocalDate date,
            @RequestParam(name = "statusList", required = false, defaultValue = "")  String statusList,
            @RequestParam(name = "id", required = false, defaultValue = "")  Long id) {

        return integrationDataService.getItemData(
                companyCode, warehouseId, startTime, endTime, date, statusList, id);
    }

    @RequestMapping(value="/items/{id}", method = RequestMethod.GET)
    public IntegrationItemData getIntegrationItemData(@PathVariable Long id) {

        return integrationDataService.getItemData(id);
    }
    @RequestMapping(value="/items/{id}/resend", method = RequestMethod.POST)
    public IntegrationItemData resendItemData(@PathVariable Long id) {

        return integrationDataService.resendItemData(id);
    }


    @RequestMapping(value="/items", method = RequestMethod.PUT)
    public ResponseBodyWrapper addIntegrationItemData(@RequestBody Item item) {

        IntegrationItemData itemData =
                integrationDataService.addIntegrationItemData(item);
        return ResponseBodyWrapper.success(String.valueOf(item.getId()));
    }
/***
    @RequestMapping(value="/dblink/item", method = RequestMethod.PUT)
    public ResponseBodyWrapper saveIntegrationItemData(
            @RequestBody DBBasedItem dbBasedItem
    ){

        logger.debug("Start to save dbBasedItem into database \n{}",
                dbBasedItem);
        integrationDataService.addIntegrationItemData(dbBasedItem);
        return ResponseBodyWrapper.success("success");
    }
            **/

    //
    // Integration - Item Family
    //
    @RequestMapping(value="/item-families", method = RequestMethod.GET)
    public List<? extends IntegrationItemFamilyData> getIntegrationItemFamilyData(
            @RequestParam String companyCode,
            @RequestParam(name = "warehouseId", required = false, defaultValue = "")  Long warehouseId,
            @RequestParam(name = "startTime", required = false, defaultValue = "") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)  ZonedDateTime startTime,
            @RequestParam(name = "endTime", required = false, defaultValue = "") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)  ZonedDateTime endTime,
            @RequestParam(name = "date", required = false, defaultValue = "") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)  LocalDate date,
            @RequestParam(name = "statusList", required = false, defaultValue = "")  String statusList,
            @RequestParam(name = "id", required = false, defaultValue = "")  Long id) {

        return integrationDataService.getItemFamilyData(
                companyCode, warehouseId, startTime, endTime, date, statusList, id);
    }

    @RequestMapping(value="/item-families/{id}", method = RequestMethod.GET)
    public IntegrationItemFamilyData getIntegrationItemFamilyData(@PathVariable Long id) {

        return integrationDataService.getItemFamilyData(id);
    }

    @RequestMapping(value="/item-families/{id}/resend", method = RequestMethod.POST)
    public IntegrationItemFamilyData resendItemFamilyData(@PathVariable Long id) {

        return integrationDataService.resendItemFamilyData(id);
    }
    @RequestMapping(value="/item-families", method = RequestMethod.PUT)
    public IntegrationItemFamilyData addIntegrationItemFamilyData(@RequestBody ItemFamily itemFamily) {

        return integrationDataService.addIntegrationItemFamilyData(itemFamily);
    }

    //
    // Integration - Item Package Type
    //
    @RequestMapping(value="/item-package-types", method = RequestMethod.GET)
    public List<? extends IntegrationItemPackageTypeData> getIntegrationItemPackageTypeData(
            @RequestParam String companyCode,
            @RequestParam(name = "warehouseId", required = false, defaultValue = "")  Long warehouseId,
            @RequestParam(name = "startTime", required = false, defaultValue = "") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)  ZonedDateTime startTime,
            @RequestParam(name = "endTime", required = false, defaultValue = "") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)  ZonedDateTime endTime,
            @RequestParam(name = "date", required = false, defaultValue = "") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)  LocalDate date,
            @RequestParam(name = "statusList", required = false, defaultValue = "")   String statusList,
            @RequestParam(name = "id", required = false, defaultValue = "")   Long id) {

        return integrationDataService.getItemPackageTypeData(
                companyCode, warehouseId, startTime, endTime, date, statusList, id);
    }

    @RequestMapping(value="/item-package-types/{id}", method = RequestMethod.GET)
    public IntegrationItemPackageTypeData getIntegrationItemPackageTypeData(@PathVariable Long id) {

        return integrationDataService.getItemPackageTypeData(id);
    }
    @RequestMapping(value="/item-package-types/{id}/resend", method = RequestMethod.POST)
    public IntegrationItemPackageTypeData resendItemPackageTypeData(@PathVariable Long id) {

        return integrationDataService.resendItemPackageTypeData(id);
    }

    @RequestMapping(value="/item-package-types", method = RequestMethod.PUT)
    public IntegrationItemPackageTypeData addIntegrationItemPackageTypeData(@RequestBody ItemPackageType itemPackageType) {

        return integrationDataService.addIntegrationItemPackageTypeData(itemPackageType);
    }
/**
    @RequestMapping(value="/dblink/item-package-type", method = RequestMethod.PUT)
    public ResponseBodyWrapper saveIntegrationItemPackageTypeData(
            @RequestBody DBBasedItemPackageType dbBasedItemPackageType
    ){

        logger.debug("Start to save dbBasedItemPackageType into database \n{}",
                dbBasedItemPackageType);
        dbBasedItemPackageType.getItemUnitOfMeasures().forEach(
                dbBasedItemUnitOfMeasure -> dbBasedItemUnitOfMeasure.setItemPackageType(
                        dbBasedItemPackageType
                )
        );
        integrationDataService.addIntegrationItemPackageTypeData(dbBasedItemPackageType);
        return ResponseBodyWrapper.success("success");
    }
            */

    //
    // Integration - Item Unit Of Measure
    //
    @RequestMapping(value="/item-unit-of-measures", method = RequestMethod.GET)
    public List<? extends IntegrationItemUnitOfMeasureData> getIntegrationItemUnitOfMeasureData(
            @RequestParam String companyCode,
            @RequestParam(name = "warehouseId", required = false, defaultValue = "")  Long warehouseId,
            @RequestParam(name = "startTime", required = false, defaultValue = "") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)  ZonedDateTime startTime,
            @RequestParam(name = "endTime", required = false, defaultValue = "") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)  ZonedDateTime endTime,
            @RequestParam(name = "date", required = false, defaultValue = "") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)  LocalDate date,
            @RequestParam(name = "statusList", required = false, defaultValue = "")  String statusList,
            @RequestParam(name = "id", required = false, defaultValue = "")   Long id) {

        return integrationDataService.getItemUnitOfMeasureData(
                companyCode, warehouseId, startTime, endTime, date, statusList, id);
    }

    @RequestMapping(value="/item-unit-of-measures/{id}", method = RequestMethod.GET)
    public IntegrationItemUnitOfMeasureData getIntegrationItemUnitOfMeasureData(@PathVariable Long id) {

        return integrationDataService.getItemUnitOfMeasureData(id);
    }
    @RequestMapping(value="/item-unit-of-measures/{id}/resend", method = RequestMethod.POST)
    public IntegrationItemUnitOfMeasureData resendItemUnitOfMeasureData(@PathVariable Long id) {

        return integrationDataService.resendItemUnitOfMeasureData(id);
    }

    @RequestMapping(value="/item-unit-of-measures", method = RequestMethod.PUT)
    public IntegrationItemUnitOfMeasureData addIntegrationItemUnitOfMeasureData(@RequestBody ItemUnitOfMeasure itemUnitOfMeasure) {

        return integrationDataService.addIntegrationItemUnitOfMeasureData(itemUnitOfMeasure);
    }



}
