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

package com.garyzhangscm.cwms.adminserver.service;

import com.garyzhangscm.cwms.adminserver.clients.WarehouseLayoutServiceRestemplateClient;
import com.garyzhangscm.cwms.adminserver.exception.DataTransferException;
import com.garyzhangscm.cwms.adminserver.exception.ResourceNotFoundException;
import com.garyzhangscm.cwms.adminserver.model.*;
import com.garyzhangscm.cwms.adminserver.model.wms.Company;
import com.garyzhangscm.cwms.adminserver.model.wms.UnitOfMeasure;
import com.garyzhangscm.cwms.adminserver.model.wms.Warehouse;
import com.garyzhangscm.cwms.adminserver.repository.DataTransferRequestRepository;
import com.garyzhangscm.cwms.adminserver.service.datatransfer.DataTransferExportService;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class DataTransferRequestService {
    private static final Logger logger = LoggerFactory.getLogger(DataTransferRequestService.class);
    @Autowired
    private DataTransferRequestRepository dataTransferRequestRepository;
    @Autowired
    private WarehouseLayoutServiceRestemplateClient warehouseLayoutServiceRestemplateClient;


    @Autowired()
    List<DataTransferExportService> dataTransferExportServices;

    // Map of all inprocess data initiate request
    // key: request id(database primary key)
    // value: request
    private Map<Long, DataTransferRequest> dataTransferRequestMap = new HashMap<>();


    public DataTransferRequest findById(Long id) {
        // if we have the data transfer request in process, then return from the map
        // instead of the database. The data saved in the map normally have the
        // latest data
        if (dataTransferRequestMap.containsKey(id)) {
            return dataTransferRequestMap.get(id);
        }
        DataTransferRequest dataTransferRequest =  dataTransferRequestRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.raiseException("data transfer request not found by id: " + id));
        return dataTransferRequest;
    }


    public List<DataTransferRequest> findAll(String number,
                                             Long companyId,
                                             String companyCode,
                                             String status) {

        return dataTransferRequestRepository.findAll(
                (Root<DataTransferRequest> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) -> {
                    List<Predicate> predicates = new ArrayList<Predicate>();

                    if (!StringUtils.isBlank(number)) {
                        predicates.add(criteriaBuilder.equal(root.get("number"), number));
                    }

                    if (Objects.nonNull(companyId)) {
                        predicates.add(criteriaBuilder.equal(root.get("companyId"), companyId));
                    }
                    else if (Strings.isNotBlank(companyCode)) {
                        Company company = warehouseLayoutServiceRestemplateClient.getCompanyByCode(companyCode);
                        if (Objects.nonNull(company)) {

                            predicates.add(criteriaBuilder.equal(root.get("companyId"), company.getId()));
                        }
                        else {

                            // the company code is wrong, we will return nothing
                            predicates.add(criteriaBuilder.equal(root.get("companyId"), -999));
                        }
                    }
                    if (Strings.isNotBlank(status)) {

                        predicates.add(criteriaBuilder.equal(root.get("status"), DataTransferRequestStatus.valueOf(status)));
                    }
                    Predicate[] p = new Predicate[predicates.size()];
                    return criteriaBuilder.and(predicates.toArray(p));
                }
        );



    }

    public DataTransferRequest save(DataTransferRequest dataTransferRequest) {
        return dataTransferRequestRepository.save(dataTransferRequest);
    }

    public DataTransferRequest findByNumber(String number) {
        // if we can find from the in memory map, then return data from it.
        // otherwise, get from the database
        DataTransferRequest dataTransferRequest = dataTransferRequestMap.values().stream()
                .filter(inprocessDataTransferRequest -> inprocessDataTransferRequest.getNumber().equals(number))
                .findFirst().orElse(null);
        if (Objects.nonNull(dataTransferRequest)) {
            return dataTransferRequest;
        }
        return dataTransferRequestRepository.findByNumber(number);


    }
    public DataTransferRequest saveOrUpdate(DataTransferRequest dataTransferRequest) {
        if (Objects.isNull(dataTransferRequest.getId()) &&
                !Objects.isNull(findByNumber(dataTransferRequest.getNumber()))) {
            dataTransferRequest.setId(findByNumber(dataTransferRequest.getNumber()).getId());
        }
        return save(dataTransferRequest);
    }

    public DataTransferRequest exportData(String number, Long companyId, String description) {
        DataTransferRequest dataTransferRequest = findByNumber(number);
        if (Objects.nonNull(dataTransferRequest)) {
            // the data transfer request with the specific number already exists
            return dataTransferRequest;
        }

        dataTransferRequest = createNewDataTransferRequest(number, DataTransferRequestType.EXPORT,companyId, description);

        // save the current status. We will start a new thread to process the data export
        dataTransferRequest = saveOrUpdate(dataTransferRequest);
        long dataTransferRequestId = dataTransferRequest.getId();
        dataTransferRequestMap.put(dataTransferRequestId, dataTransferRequest);
        Company company = warehouseLayoutServiceRestemplateClient.getCompanyById(companyId);

        new Thread(() ->{

            DataTransferRequest savedDataTransferRequest =
                    dataTransferRequestMap.get(dataTransferRequestId);
            logger.debug("Start to process data transfer request \n{}", savedDataTransferRequest);

            try {
                // go through all the export service and export one by one based on the sequence
                Collections.sort(dataTransferExportServices, Comparator.comparing(DataTransferExportService::getSequence));

                dataTransferExportServices.forEach(
                        dataTransferExportService -> {
                            logger.debug("start to process export service: {}", dataTransferExportService.getTablesName());
                            // set the status of the matched detail to INPROCESS
                            DataTransferRequestDetail matchedDataTransferRequestDetail =
                                    savedDataTransferRequest.getDataTransferRequestDetail(
                                            dataTransferExportService.getTablesName()
                                    );
                            if (Objects.nonNull(matchedDataTransferRequestDetail)) {
                                logger.debug("We got matched request detail");
                                matchedDataTransferRequestDetail.setStatus(
                                        DataTransferRequestStatus.INPROCESS
                                );
                                save(savedDataTransferRequest);
                            }

                            // export the data
                            boolean exportResult = true;
                            try {
                                dataTransferExportService.exportData(savedDataTransferRequest);

                            } catch (IOException e) {
                                exportResult = false;
                            }

                            // update the correspondent record to COMPLETE
                            if (Objects.nonNull(matchedDataTransferRequestDetail)) {
                                matchedDataTransferRequestDetail.setStatus(
                                        exportResult? DataTransferRequestStatus.COMPLETE : DataTransferRequestStatus.ERROR
                                );
                                save(savedDataTransferRequest);
                            }


                        }
                );

            } catch (Exception e) {
                e.printStackTrace();
                savedDataTransferRequest.setStatus(DataTransferRequestStatus.ERROR);
                saveOrUpdate(savedDataTransferRequest);
                dataTransferRequestMap.remove(savedDataTransferRequest);
            }
            // the data transfer request is done

            savedDataTransferRequest.setStatus(DataTransferRequestStatus.COMPLETE);
            saveOrUpdate(savedDataTransferRequest);
            dataTransferRequestMap.remove(savedDataTransferRequest);
        }).start();
        return dataTransferRequest;

    }

    private DataTransferRequest createNewDataTransferRequest(String number, DataTransferRequestType type, Long companyId, String description) {
        DataTransferRequest dataTransferRequest = new DataTransferRequest();
        dataTransferRequest.setCompanyId(companyId);
        dataTransferRequest.setCompany(warehouseLayoutServiceRestemplateClient.getCompanyById(companyId));

        dataTransferRequest.setType(type);
        dataTransferRequest.setNumber(number);
        dataTransferRequest.setDescription(description);
        dataTransferRequest.setStatus(DataTransferRequestStatus.PENDING);

        // for each registered service, create the request details
        if(type.equals(DataTransferRequestType.EXPORT)) {

            // go through all the export service and export one by one based on the sequence
            Collections.sort(dataTransferExportServices, Comparator.comparing(DataTransferExportService::getSequence));

            dataTransferExportServices.forEach(
                    dataTransferExportService -> {
                        DataTransferRequestDetail dataTransferRequestDetail = new DataTransferRequestDetail(
                                dataTransferExportService.getSequence(),
                                dataTransferExportService.getDescription(),
                                dataTransferExportService.getTablesName(),
                                DataTransferRequestStatus.PENDING,
                                dataTransferRequest
                        );
                        dataTransferRequest.addDataTransferRequestDetails(dataTransferRequestDetail);
                    }
            );
        }


        return dataTransferRequest;

    }

    public DataTransferRequest importData(String number, Long companyId, String description) {

        DataTransferRequest dataTransferRequest = findByNumber(number);
        if (Objects.nonNull(dataTransferRequest)) {
            // the data transfer request with the specific number already exists
            return dataTransferRequest;
        }

        return dataTransferRequest;
    }

    public DataTransferRequest addDataTransferRequest(String number, Long companyId, String description,
                                                      String type) {
        DataTransferRequestType dataTransferRequestType =
                DataTransferRequestType.valueOf(type);
        if (dataTransferRequestType.equals(DataTransferRequestType.EXPORT)) {
            return exportData(number, companyId, description
            );
        }
        else if (dataTransferRequestType.equals(DataTransferRequestType.IMPORT)) {
            return importData(number, companyId, description
            );
        }
        throw DataTransferException.raiseException("can't process data transfer request with type " + type);
    }
}
