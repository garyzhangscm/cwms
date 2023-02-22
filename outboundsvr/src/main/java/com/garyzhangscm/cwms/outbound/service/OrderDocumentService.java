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

package com.garyzhangscm.cwms.outbound.service;

import com.garyzhangscm.cwms.outbound.ResponseBodyWrapper;
import com.garyzhangscm.cwms.outbound.exception.ResourceNotFoundException;
import com.garyzhangscm.cwms.outbound.model.Order;
import com.garyzhangscm.cwms.outbound.model.*;
import com.garyzhangscm.cwms.outbound.repository.OrderDocumentRepository;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.persistence.criteria.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class OrderDocumentService {
    private static final Logger logger = LoggerFactory.getLogger(OrderDocumentService.class);

    @Autowired
    private OrderDocumentRepository orderDocumentRepository;
    @Autowired
    private OrderService orderService;
    @Autowired
    private UserService userService;

    @Autowired
    private FileService fileService;

    @Value("${outbound.order.document.folder}")
    private String orderDocumentFolder;

    public OrderDocument findById(Long id) {
        return orderDocumentRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.raiseException("order document not found by id: " + id));

    }

    public List<OrderDocument> findAll(Long warehouseId, Long orderId,
                                       String orderNumber, String fileName) {

        return orderDocumentRepository.findAll(
                (Root<OrderDocument> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) -> {
                    List<Predicate> predicates = new ArrayList<Predicate>();

                    predicates.add(criteriaBuilder.equal(root.get("warehouseId"), warehouseId));

                    if (Objects.nonNull(orderId) || Strings.isNotBlank(orderNumber)) {

                        Join<OrderDocument, Order> joinOrder = root.join("order", JoinType.INNER);
                        if (Objects.nonNull(orderId)) {
                            predicates.add(criteriaBuilder.equal(joinOrder.get("id"), orderId));

                        }
                        if (Strings.isNotBlank(orderNumber)) {
                            if (orderNumber.contains("*")) {

                                predicates.add(criteriaBuilder.like(joinOrder.get("number"), orderNumber.replaceAll("\\*", "%")));
                            }
                            else {

                                predicates.add(criteriaBuilder.equal(joinOrder.get("number"), orderNumber));

                            }

                        }


                    }
                    if (Strings.isNotBlank(fileName)) {

                        predicates.add(criteriaBuilder.equal(root.get("fileName"), fileName));
                    }
                    Predicate[] p = new Predicate[predicates.size()];
                    return criteriaBuilder.and(predicates.toArray(p));
                }
        );

    }

    public OrderDocument save(OrderDocument orderDocument) {
        return orderDocumentRepository.save(orderDocument);
    }


    public void delete(OrderDocument orderDocument) {
        orderDocumentRepository.delete(orderDocument);
    }
    public void delete(Long id) {
        orderDocumentRepository.deleteById(id);
    }

    public String uploadOrderDocument(Long warehouseId, Long orderId, MultipartFile file) throws IOException {

        String filePath = getUploadOrderDocumentFilePath(warehouseId, orderId);
        logger.debug("Save file to {}{}",
                filePath, file.getOriginalFilename());

        File savedFile =
                fileService.saveFile(
                        file, filePath, file.getOriginalFilename());

        logger.debug("File saved, path: {}",
                savedFile.getAbsolutePath());
        return file.getOriginalFilename();

    }

    // filename is the business key for the upload
    public OrderDocument findByFileName(Long warehouseId, Long orderId, String fileName) {
        List<OrderDocument> orderDocuments = findAll(warehouseId, orderId, null, fileName);
        if (orderDocuments.isEmpty()) {
            return null;
        }
        return orderDocuments.get(0);
    }

    private String getUploadOrderDocumentFilePath(Long warehouseId, Long orderId) {

        if (!orderDocumentFolder.endsWith("/")) {
            return orderDocumentFolder + "/" + warehouseId + "/" + orderId + "/";
        }
        else  {

            return orderDocumentFolder + warehouseId + "/" + orderId + "/";
        }
    }

    public List<OrderDocument> saveOrderDocument(Long warehouseId, Long orderId, List<OrderDocument> newOrderDocuments) throws IOException {
        // let's make sure the files are already in folder
        validateOrderDocumentFileExists(warehouseId, orderId, newOrderDocuments);
        Order order = orderService.findById(orderId);

        // setup the id for the new order document. We will identify a order document by file name, which means
        // the file name has to be unique in the same order
        for (OrderDocument newOrderDocument : newOrderDocuments) {
            OrderDocument existsOrderDocument = findByFileName(
                    warehouseId, orderId, newOrderDocument.getFileName()
            );
            if (Objects.nonNull(existsOrderDocument)) {
                newOrderDocument.setId(existsOrderDocument.getId());
            }
            // set up the username as well so we keep track of who upload
            // the file
            newOrderDocument.setUsername(userService.getCurrentUserName());
            newOrderDocument.setOrder(order);
        }

        List<OrderDocument> existsOrderDocuments = findAll(warehouseId, orderId, null, null);
        // see if we will need to remove any of the existing order documents
        List<OrderDocument> orderDocumentsNoLongExists =
                existsOrderDocuments.stream().filter(
                        orderDocument -> {
                            // if the existsing order document no long exists in the new order documents, then we
                            // will need to remove it
                            return newOrderDocuments.stream().noneMatch(
                                    newOrderDocument -> Objects.nonNull(newOrderDocument.getId()) &&
                                            orderDocument.getId().equals(newOrderDocument.getId())
                            );
                        }
                ).collect(Collectors.toList());
        if (!orderDocumentsNoLongExists.isEmpty()) {
            for (OrderDocument orderDocumentsNoLongExist : orderDocumentsNoLongExists) {
                delete(orderDocumentsNoLongExist.getId());
                removeFile(warehouseId, orderId, orderDocumentsNoLongExist.getFileName());
            }
        }
        List<OrderDocument> resultOrderDocuments = new ArrayList<>();
        for (OrderDocument newOrderDocument : newOrderDocuments) {
            resultOrderDocuments.add(save(newOrderDocument));
        }

        return resultOrderDocuments;



    }

    private void removeFile(Long warehouseId, Long orderId, String fileName) throws IOException {

        String filePath = getUploadOrderDocumentFilePath(warehouseId, orderId);
        logger.debug("remove file to {}{}",
                filePath, fileName);

        fileService.removeFile(filePath, fileName);
    }

    private void validateOrderDocumentFileExists(Long warehouseId, Long orderId, List<OrderDocument> orderDocuments) {

        String filePath = getUploadOrderDocumentFilePath(warehouseId, orderId);

        for (OrderDocument orderDocument : orderDocuments) {
            logger.debug("check if  file exists {}{}",
                    filePath, orderDocument.getFileName());

            if (!fileService.fileExists(filePath,orderDocument.getFileName())) {
                throw ResourceNotFoundException.raiseException("can't save the document " + orderDocument.getFileName() +
                        " as we can't find the file. Please double check if you already upload the file");
            }

        }
    }

    public File getFile(Long id) {
        OrderDocument orderDocument = findById(id);

        return getFile(
                orderDocument.getWarehouseId(),
                orderDocument.getOrder().getId(),
                orderDocument.getFileName()
        );


    }
    public File getFile(Long warehouseId, Long orderId, String fileName) {

        String filePath = getUploadOrderDocumentFilePath(warehouseId,
                orderId);

        String fileUrl = filePath + "/" + fileName;

        logger.debug("Will return {} to the client",
                fileUrl);
        return new File(fileUrl);

    }

    public String removeOrderDocument(Long id) {
        OrderDocument orderDocument = findById(id);
        try {
            removeFile(orderDocument.getWarehouseId(),
                    orderDocument.getOrder().getId(),
                    orderDocument.getFileName());
        } catch (IOException e) {
            e.printStackTrace();
            // ignore the error
            // we may have another transaction to
            // clear the aged files

        }

        delete(id);
        return orderDocument.getFileName();
    }
}
