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

package com.garyzhangscm.cwms.inventory.controller;

import com.garyzhangscm.cwms.inventory.ResponseBodyWrapper;
import com.garyzhangscm.cwms.inventory.clients.WarehouseLayoutServiceRestemplateClient;
import com.garyzhangscm.cwms.inventory.exception.MissingInformationException;
import com.garyzhangscm.cwms.inventory.model.*;
import com.garyzhangscm.cwms.inventory.service.FileService;
import com.garyzhangscm.cwms.inventory.service.ItemService;
import com.garyzhangscm.cwms.inventory.service.UploadFileService;
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
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.Objects;

@RestController
public class ItemController {
    private static final Logger logger = LoggerFactory.getLogger(ItemController.class);
    @Autowired
    ItemService itemService;

    @Autowired
    FileService fileService;
    @Autowired
    private UploadFileService uploadFileService;

    @Autowired
    WarehouseLayoutServiceRestemplateClient warehouseLayoutServiceRestemplateClient;

    @ClientValidationEndpoint
    @RequestMapping(value="/items", method = RequestMethod.GET)
    public List<Item> findAllItems(@RequestParam(name="companyId", required = false, defaultValue = "")  Long companyId,
                                   @RequestParam(name="warehouseId", required = false, defaultValue = "")  Long warehouseId,
                                   @RequestParam(name="name", required = false, defaultValue = "") String name,
                                   @RequestParam(name="description", required = false, defaultValue = "") String description,
                                   @RequestParam(name="quickbookListId", required = false, defaultValue = "") String quickbookListId,
                                   @RequestParam(name="clientIds", required = false, defaultValue = "") String clientIds,
                                   @RequestParam(name="itemFamilyIds", required = false, defaultValue = "") String itemFamilyIds,
                                   @RequestParam(name="itemIdList", required = false, defaultValue = "") String itemIdList,
                                   @RequestParam(name="companyItem", required = false, defaultValue = "") Boolean companyItem,
                                   @RequestParam(name="warehouseSpecificItem", required = false, defaultValue = "") Boolean warehouseSpecificItem,
                                   @RequestParam(name="loadDetails", required = false, defaultValue = "true") Boolean loadDetails,
                                   ClientRestriction clientRestriction) {

        // company ID or warehouse id is required
        if (Objects.isNull(companyId) && Objects.isNull(warehouseId)) {

            throw MissingInformationException.raiseException("company information or warehouse id is required for item integration");
        }
        else if (Objects.isNull(companyId)) {
            // if company Id is empty, but we have company code,
            // then get the company id from the code
            companyId =
                    warehouseLayoutServiceRestemplateClient
                            .getWarehouseById(warehouseId).getCompanyId();
        }

        return itemService.findAll(companyId, warehouseId, name, quickbookListId, clientIds, itemFamilyIds, itemIdList, companyItem,
                warehouseSpecificItem, description,  loadDetails, clientRestriction);
    }


    @ClientValidationEndpoint
    @RequestMapping(value="/items-query/by-keyword", method = RequestMethod.GET)
    public List<Item> findByKeyword(@RequestParam Long companyId,
                                    @RequestParam Long warehouseId,
                                    @RequestParam String keyword,
                                    @RequestParam(name="loadDetails", required = false, defaultValue = "true") Boolean loadDetails,
                                    ClientRestriction clientRestriction) {

        return itemService.findByKeyword(companyId, warehouseId, keyword, loadDetails, clientRestriction);

    }
    @RequestMapping(value="/items/{id}", method = RequestMethod.GET)
    public Item findItem(@PathVariable Long id) {
        return itemService.findById(id);
    }

    @BillableEndpoint
    @RequestMapping(value="/items/{id}", method = RequestMethod.DELETE)
    @Caching(
            evict = {
                    @CacheEvict(cacheNames = "AdminService_Item", allEntries = true),
                    @CacheEvict(cacheNames = "InboundService_Item", allEntries = true),
                    @CacheEvict(cacheNames = "IntegrationService_Item", allEntries = true),
                    @CacheEvict(cacheNames = "LayoutService_Item", allEntries = true),
                    @CacheEvict(cacheNames = "OutboundService_Item", allEntries = true),
                    @CacheEvict(cacheNames = "WorkOrderService_Item", allEntries = true),
                    @CacheEvict(cacheNames = "IntegrationService_ItemPackageType", allEntries = true),
            }
    )
    public Item deleteItem(@PathVariable Long id) {
        return itemService.deleteItem(id);
    }

    @BillableEndpoint
    @RequestMapping(method=RequestMethod.POST, value="/items/{id}/images/upload")
    @Caching(
            evict = {
                    @CacheEvict(cacheNames = "AdminService_Item", allEntries = true),
                    @CacheEvict(cacheNames = "InboundService_Item", allEntries = true),
                    @CacheEvict(cacheNames = "IntegrationService_Item", allEntries = true),
                    @CacheEvict(cacheNames = "LayoutService_Item", allEntries = true),
                    @CacheEvict(cacheNames = "OutboundService_Item", allEntries = true),
                    @CacheEvict(cacheNames = "WorkOrderService_Item", allEntries = true),
                    @CacheEvict(cacheNames = "IntegrationService_ItemPackageType", allEntries = true),
            }
    )
    public Item uploadItemImages(@PathVariable Long id,
                                           @RequestParam("file") MultipartFile file) throws IOException {


        return  itemService.uploadItemImages(id, file);
    }

    @BillableEndpoint
    @RequestMapping(method=RequestMethod.POST, value="/items/{id}/work-order-sop/upload")
    @Caching(
            evict = {
                    @CacheEvict(cacheNames = "AdminService_Item", allEntries = true),
                    @CacheEvict(cacheNames = "InboundService_Item", allEntries = true),
                    @CacheEvict(cacheNames = "IntegrationService_Item", allEntries = true),
                    @CacheEvict(cacheNames = "LayoutService_Item", allEntries = true),
                    @CacheEvict(cacheNames = "OutboundService_Item", allEntries = true),
                    @CacheEvict(cacheNames = "WorkOrderService_Item", allEntries = true),
                    @CacheEvict(cacheNames = "IntegrationService_ItemPackageType", allEntries = true),
            }
    )
    public Item uploadItemWorkOrderSOP(@PathVariable Long id,
                                 @RequestParam("file") MultipartFile file) throws IOException {


        return  itemService.uploadItemWorkOrderSOP(id, file);
    }

    @BillableEndpoint
    @RequestMapping(method=RequestMethod.DELETE, value="/items")
    @Caching(
            evict = {
                    @CacheEvict(cacheNames = "AdminService_Item", allEntries = true),
                    @CacheEvict(cacheNames = "InboundService_Item", allEntries = true),
                    @CacheEvict(cacheNames = "IntegrationService_Item", allEntries = true),
                    @CacheEvict(cacheNames = "LayoutService_Item", allEntries = true),
                    @CacheEvict(cacheNames = "OutboundService_Item", allEntries = true),
                    @CacheEvict(cacheNames = "WorkOrderService_Item", allEntries = true),
                    @CacheEvict(cacheNames = "IntegrationService_ItemPackageType", allEntries = true),
            }
    )
    public void removeItems(@RequestParam(name = "item_ids", required = false, defaultValue = "") String itemIds) {
        itemService.delete(itemIds);
    }

    @BillableEndpoint
    @RequestMapping(method=RequestMethod.POST, value="/items")
    public Item addItem(@RequestBody Item item) {
        return itemService.addItem(item);
    }

    @BillableEndpoint
    @RequestMapping(value="/items/{id}", method = RequestMethod.PUT)
    @Caching(
            evict = {
                    @CacheEvict(cacheNames = "AdminService_Item", allEntries = true),
                    @CacheEvict(cacheNames = "InboundService_Item", allEntries = true),
                    @CacheEvict(cacheNames = "IntegrationService_Item", allEntries = true),
                    @CacheEvict(cacheNames = "LayoutService_Item", allEntries = true),
                    @CacheEvict(cacheNames = "OutboundService_Item", allEntries = true),
                    @CacheEvict(cacheNames = "WorkOrderService_Item", allEntries = true),
                    @CacheEvict(cacheNames = "IntegrationService_ItemPackageType", allEntries = true),
            }
    )
    public Item changeItem(@PathVariable Long id, @RequestBody Item item) {
        return itemService.changeItem(id, item);
    }

    @RequestMapping(method=RequestMethod.GET, value="/items/{id}/thumbnail")
    public ResponseEntity<Resource> getThumbnail(@RequestParam Long warehouseId,
                                                 @PathVariable Long id) throws FileNotFoundException {



        File thumbnailFile = itemService.getThumbnail(id);

        InputStreamResource resource
                = new InputStreamResource(new FileInputStream(thumbnailFile));
        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment;fileName=" + thumbnailFile.getName())
                .contentLength(thumbnailFile.length())
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
    }

    @RequestMapping(method=RequestMethod.GET, value="/items/{id}/image")
    public ResponseEntity<Resource> getImage(@RequestParam Long warehouseId,
                                                 @PathVariable Long id,
                                             @RequestParam(required = false,name = "download", defaultValue = "true") Boolean download) throws FileNotFoundException {

        logger.debug("start get image for item id {}, download? {}",
                id, download);
        File imageFile = itemService.getImage(id);

        InputStreamResource resource
                = new InputStreamResource(new FileInputStream(imageFile));

        return ResponseEntity.ok()
                .header("Content-Disposition",

                        (Boolean.FALSE.equals(download) ? "inline" : "attachment") +
                                ";fileName=" + imageFile.getName())
                .contentLength(imageFile.length())
                .contentType(Boolean.FALSE.equals(download) ? MediaType.IMAGE_PNG : MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
    }
    @RequestMapping(method=RequestMethod.GET, value="/items/{id}/work-order-sop")
    public ResponseEntity<Resource> getWorkOrderSOP(@RequestParam Long warehouseId,
                                             @PathVariable Long id,
                                                    @RequestParam(required = false,name = "download", defaultValue = "true") Boolean download) throws FileNotFoundException {


        logger.debug("start get work order sop for item id {}, download? {}",
                id, download);

        File workOrderSOPFile = itemService.getWorkOrderSOP(id);

        InputStreamResource resource
                = new InputStreamResource(new FileInputStream(workOrderSOPFile));
        return ResponseEntity.ok()
                .header("Content-Disposition",
                        (Boolean.FALSE.equals(download) ? "inline" : "attachment") +
                                ";fileName=" + workOrderSOPFile.getName())
                .contentLength(workOrderSOPFile.length())
                .contentType(Boolean.FALSE.equals(download) ? MediaType.APPLICATION_PDF : MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
    }

    @RequestMapping(method=RequestMethod.POST, value="/items/validate-new-item-name")
    public ResponseBodyWrapper<String> validateNewItemName(@RequestParam Long warehouseId,
                                                           @RequestParam String itemName,
                                                           @RequestParam(name="clientId", required = false, defaultValue = "") Long clientId)  {

        return ResponseBodyWrapper.success(itemService.validateNewItemName(warehouseId,clientId,  itemName));
    }


    @BillableEndpoint
    @RequestMapping(method=RequestMethod.POST, value="/items/upload")
    @Caching(
            evict = {
                    @CacheEvict(cacheNames = "AdminService_Item", allEntries = true),
                    @CacheEvict(cacheNames = "InboundService_Item", allEntries = true),
                    @CacheEvict(cacheNames = "IntegrationService_Item", allEntries = true),
                    @CacheEvict(cacheNames = "LayoutService_Item", allEntries = true),
                    @CacheEvict(cacheNames = "OutboundService_Item", allEntries = true),
                    @CacheEvict(cacheNames = "WorkOrderService_Item", allEntries = true),
                    @CacheEvict(cacheNames = "IntegrationService_ItemPackageType", allEntries = true),
            }
    )
    public ResponseBodyWrapper uploadItems(Long companyId, Long warehouseId,
                                           @RequestParam(name = "ignoreUnknownFields", defaultValue = "false", required = false) Boolean ignoreUnknownFields,
                                           @RequestParam("file") MultipartFile file) throws IOException {


        try {

            File localFile = uploadFileService.convertToCSVFile(
                    companyId, warehouseId, "items", fileService.saveFile(file), ignoreUnknownFields);

            String fileUploadProgressKey = itemService.uploadItemData(warehouseId, localFile);
            return  ResponseBodyWrapper.success(fileUploadProgressKey);
        }
        catch (Exception ex) {
            return new ResponseBodyWrapper(-1, ex.getMessage(), "");
        }

    }

    @RequestMapping(method=RequestMethod.GET, value="/items/upload/progress")
    public ResponseBodyWrapper getFileUploadProgress(Long warehouseId,
                                                     String key) throws IOException {



        return  ResponseBodyWrapper.success(
                String.format("%.2f",itemService.getFileUploadProgress(key)));
    }
    @RequestMapping(method=RequestMethod.GET, value="/items/upload/result")
    public List<FileUploadResult> getFileUploadResult(Long warehouseId,
                                                      String key) throws IOException {


        return itemService.getFileUploadResult(warehouseId, key);
    }

    /**
     * Manually process item override. We may need to update the item id in the receipt
     * work order / orders / etc.
     * @return
     */
    @BillableEndpoint
    @RequestMapping(method=RequestMethod.POST, value="/items/process-item-override")
    @Caching(
            evict = {
                    @CacheEvict(cacheNames = "AdminService_Item", allEntries = true),
                    @CacheEvict(cacheNames = "InboundService_Item", allEntries = true),
                    @CacheEvict(cacheNames = "IntegrationService_Item", allEntries = true),
                    @CacheEvict(cacheNames = "LayoutService_Item", allEntries = true),
                    @CacheEvict(cacheNames = "OutboundService_Item", allEntries = true),
                    @CacheEvict(cacheNames = "WorkOrderService_Item", allEntries = true),
                    @CacheEvict(cacheNames = "IntegrationService_ItemPackageType", allEntries = true),
            }
    )
    public ResponseBodyWrapper processItemOverride(@RequestParam Long warehouseId,
                                                   @RequestParam(name = "itemId", defaultValue = "", required = false) Long itemId) {

        new Thread(() -> itemService.processItemOverride(warehouseId, itemId)).start();
        return  ResponseBodyWrapper.success("Success");
    }

    @RequestMapping(method=RequestMethod.GET, value="/items/last-item-from-silo-location")
    public Item getLastItemFromSiloLocation(
            Long warehouseId, String locationName,
            @RequestParam(name="loadDetails", required = false, defaultValue = "true") Boolean loadDetails) throws IOException {


        return itemService.getLastItemFromSiloLocation(warehouseId, locationName, loadDetails);
    }
}
