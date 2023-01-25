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

import com.garyzhangscm.cwms.adminserver.clients.CommonServiceRestemplateClient;
import com.garyzhangscm.cwms.adminserver.exception.ResourceNotFoundException;
import com.garyzhangscm.cwms.adminserver.model.*;
import com.garyzhangscm.cwms.adminserver.repository.InvoiceRepository;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class InvoiceService {
    private static final Logger logger = LoggerFactory.getLogger(InvoiceService.class);
    @Autowired
    private InvoiceRepository invoiceRepository;

    @Autowired
    private CommonServiceRestemplateClient commonServiceRestemplateClient;

    @Autowired
    private BillingRequestService billingRequestService;
    @Autowired
    private UserService userService;
    @Autowired
    private FileService fileService;


    @Value("${admin.invoice-document.tempFolder:NOT-SET-YET}")
    private String invoiceDocumentTempFolder;
    @Value("${report.invoice-document.folder:NOT-SET-YET}")
    private String invoiceDocumentFolder;

    @Autowired
    private List<BillingService> billingServices;

    public Invoice findById(Long id) {
        return findById(id, true);
    }
    public Invoice findById(Long id, boolean includeDetails) {
        Invoice invoice = invoiceRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.raiseException("invoice not found by id: " + id));
        if (Objects.nonNull(invoice) && includeDetails) {
            loadDetails(invoice);
        }
        return invoice;
    }

    public Invoice save(Invoice invoice) {
        return save(invoice, true);
    }
    public Invoice save(Invoice invoice, boolean loadDetails) {
        Invoice newInvoice =  invoiceRepository.save(invoice);
        if (loadDetails) {

            loadDetails(newInvoice);
        }
        return newInvoice;
    }


    public Invoice saveOrUpdate(Invoice invoice) {
        return saveOrUpdate(invoice, true);
    }
    public Invoice saveOrUpdate(Invoice invoice, boolean loadDetails) {
        if (Objects.isNull(invoice.getId()) &&
                Objects.nonNull(findByNumber(invoice.getWarehouseId(), invoice.getNumber()))) {
            invoice.setId(
                    findByNumber(invoice.getWarehouseId(), invoice.getNumber()).getId()
            );
        }
        return save(invoice, loadDetails);
    }



    /**
     * return the exactly matched record if the exactmath
     * @param companyId
     * @param warehouseId
     * @param number
     * @return
     */
    public List<Invoice> findAll(Long companyId,
                                     Long warehouseId,
                                     Long clientId,
                                     String number) {
        return findAll(companyId, warehouseId, clientId, number, true);
    }


    public List<Invoice> findAll(Long companyId,
                                     Long warehouseId,
                                     Long clientId,
                                     String number,
                                     boolean includeDetails) {

        List<Invoice> invoices =  invoiceRepository.findAll(
                (Root<Invoice> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) -> {

                    List<Predicate> predicates = new ArrayList<Predicate>();

                    predicates.add(criteriaBuilder.equal(root.get("companyId"), companyId));
                    predicates.add(criteriaBuilder.equal(root.get("warehouseId"), warehouseId));
                    if (Objects.isNull(clientId)) {
                        // if the client id is not passed in, then we will only return the
                        // rate that defined for the warehouse, not for any client
                        predicates.add(criteriaBuilder.isNull(root.get("clientId")));
                    }
                    else {
                        predicates.add(criteriaBuilder.equal(root.get("clientId"), clientId));
                    }
                    if (StringUtils.isNotBlank(number)) {
                        if (number.contains("%")) {

                            predicates.add(criteriaBuilder.like(root.get("number"), number));
                        }
                        else {

                            predicates.add(criteriaBuilder.equal(root.get("number"), number));
                        }
                    }

                    Predicate[] p = new Predicate[predicates.size()];
                    return criteriaBuilder.and(predicates.toArray(p));
                }
                ,
                Sort.by(Sort.Direction.ASC, "warehouseId", "clientId")
        );

        if (!invoices.isEmpty() && includeDetails) {

            loadDetails(invoices);
        }

        return invoices;
    }


    private void loadDetails(List<Invoice> invoices) {
        invoices.forEach(
                this::loadDetails
        );
    }
    private void loadDetails(Invoice invoice) {
        if (Objects.nonNull(invoice.getClientId()) &&
                Objects.isNull(invoice.getClient())) {
            invoice.setClient(
                    commonServiceRestemplateClient.getClientById(
                            invoice.getClientId()
                    )
            );
        }
    }

    public Invoice findByNumber(Long warehouseId, String number) {
        return invoiceRepository.findByWarehouseIdAndNumber(warehouseId, number);
    }

    public Invoice generateInvoice(String number, String referenceNumber, String comment,
                                   ZonedDateTime startTime, ZonedDateTime endTime,
                                   Long companyId, Long warehouseId, Long clientId) {
        List<BillingRequest> billingRequests = billingServices.stream().map(
                billingService -> billingService.generateBillingRequest(
                        startTime, endTime, companyId, warehouseId, clientId,
                        "", true
                )
        ).collect(Collectors.toList());

        return generateInvoiceFromBillingRequest(
                number, referenceNumber, comment,
                startTime, endTime, companyId, warehouseId, clientId,
                billingRequests
        );

    }
    public String getNextNumber(Long warehouseId) {
        return commonServiceRestemplateClient.getNextNumber(warehouseId, "invoice-number");
    }

    public Invoice generateInvoiceFromBillingRequest(
            String number, String referenceNumber, String comment, ZonedDateTime startTime,
            ZonedDateTime endTime, Long companyId, Long warehouseId, Long clientId,
            List<BillingRequest> billingRequests) {

        Invoice invoice = new Invoice(
                companyId, warehouseId,
                clientId, number, referenceNumber,
                comment, startTime, endTime, 0.0
        );
        billingRequests.forEach(
                billingRequest -> {
                    // if we haven't saved the billing request yet, serialize it first
                    if (Objects.isNull(billingRequest.getId())) {
                        billingRequest = billingRequestService.addBillingRequest(billingRequest);
                    }
                    InvoiceLine invoiceLine = new InvoiceLine(invoice, billingRequest);
                    invoice.addLine(invoiceLine);
                    invoice.setTotalCharge(
                            invoice.getTotalCharge() + invoiceLine.getTotalCharge()
                    );
                }
        );
        return save(invoice);
    }

    public String uploadInvoiceDocument(Long companyId, Long warehouseId, Long invoiceId, MultipartFile file) throws IOException {
        Path filePath = getInvoiceDocumentPath(companyId, warehouseId, invoiceId);
        // let's change the file name to make sure it is unique for the user
        String newFileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();

        logger.debug("Save file to {}, file original name: {}, file new name: {}",
                filePath, file.getOriginalFilename(), newFileName);


        File savedFile =
                fileService.saveFile(
                        file, filePath, newFileName);

        logger.debug("File saved, path: {}",
                savedFile.getAbsolutePath());
        return newFileName;
    }

    /**
     * Get the path to save the invoice document. If the invoice is already created, then invoice id
     * will be passed in and the file will be saved in
     * $INVOICE_DOCUMENT_PATH/companyId/warehouseId/invoiceId/
     * If not, the file will be saved in $TEMP_INVOICE_DOCUMENT_PATH/companyId/warehouseId/username/ and
     * the file will be moved to the $INVOICE_DOCUMENT_PATH/companyId/warehouseId/invoiceId/ when
     * the invoice is created
     * @param companyId
     * @param warehouseId
     * @param invoiceId
     * @return
     */
    private Path getInvoiceDocumentPath(Long companyId, Long warehouseId, Long invoiceId) {
        if (Objects.nonNull(invoiceId)) {
            return Paths.get(invoiceDocumentFolder, companyId.toString(), warehouseId.toString(), invoiceId.toString());
        }
        // if the invoice is not exist yet, let's save it to the temporary folder

        String username = userService.getCurrentUserName();
        return Paths.get(invoiceDocumentTempFolder, companyId.toString(), warehouseId.toString(), username);
    }

    public String uploadInvoiceDocument(Long companyId, Long warehouseId, MultipartFile file) throws IOException {
        return uploadInvoiceDocument(companyId, warehouseId, null, file);
    }

    public Invoice generateVendorInvoice(String number, String referenceNumber,
                                         String comment, LocalDate invoiceDate, LocalDate dueDate,
                                         Long companyId, Long warehouseId,
                                         Long clientId, Double totalCharge, List<InvoiceDocument> invoiceDocuments) throws IOException {

        Invoice invoice = findByNumber(warehouseId, number);
        if (Objects.nonNull(invoice)) {
            return changeVendorInvoice(invoice,
                    referenceNumber, comment, invoiceDate, dueDate,
                    totalCharge, invoiceDocuments);


        }
        // invoice doesn't exists, let's create a new invoice with the document
        return createVendorInvoice(number, referenceNumber,
                comment, invoiceDate, dueDate,
                companyId, warehouseId,
                clientId, totalCharge, invoiceDocuments);
    }

    public Invoice changeVendorInvoice(Invoice invoice, String referenceNumber,
                                       String comment, LocalDate invoiceDate, LocalDate dueDate,
                                       Double totalCharge, List<InvoiceDocument> invoiceDocuments) {
        // the invoice already exists, let's change the
        // 1. refrence number
        // 2. comment
        // 3. invoice date
        // 4. due date
        // 5. total charge
        // 6. files(since the invoice already exists, we will assume the files are already uploaded
        //    to the right folder under the invoice)
        invoice.setReferenceNumber(referenceNumber);
        invoice.setComment(comment);
        invoice.setInvoiceDate(invoiceDate);
        invoice.setDueDate(dueDate);
        invoice.setTotalCharge(totalCharge);

        // key : remote file name
        // value: file name
        Map<String, String> newInvoiceDocumentMap = new HashMap<>();
        invoiceDocuments.forEach(
                invoiceDocument -> newInvoiceDocumentMap.put(
                        invoiceDocument.getRemoteFileName(),
                        invoiceDocument.getFileName()
                )
        );

        // remove the document that no long needed
        List<InvoiceDocument> existingInvoiceDocuments = invoice.getDocuments();
        existingInvoiceDocuments = existingInvoiceDocuments.stream().filter(
                invoiceDocument -> newInvoiceDocumentMap.containsKey(invoiceDocument.getRemoteFileName())
        ).collect(Collectors.toList());

        // loop through the existing documents so we know the newly added document
        existingInvoiceDocuments.forEach(
                invoiceDocument -> newInvoiceDocumentMap.remove(invoiceDocument.getRemoteFileName())
        );
        // if we still have entities in the map, then those are new invoice document, let's assume we should
        // already have files upload to the right folder(compare to the files that uploaded before the invoice is
        // created), let's just save the data
        newInvoiceDocumentMap.entrySet().forEach(
                newInvoiceDocumentEntry -> {
                    // key: remote file name
                    // value: file name(for display purpose)
                    invoice.addDocument(new InvoiceDocument(
                            newInvoiceDocumentEntry.getValue(),
                            newInvoiceDocumentEntry.getKey(),
                            invoice
                    ));
                }
        );
        return saveOrUpdate(invoice, false);
    }


    public Invoice createVendorInvoice(String number, String referenceNumber,
                                         String comment, LocalDate invoiceDate, LocalDate dueDate,
                                         Long companyId, Long warehouseId,
                                         Long clientId, Double totalCharge, List<InvoiceDocument> invoiceDocuments) throws IOException {
        // for a new vendor invoice, we will
        // 1. create a invoice without any new document
        // 2. move the document from the temporary folder to the permanent folder for this invoice
        // 3. save the document information
        Invoice invoice = new Invoice(
                companyId, warehouseId,
                clientId, number, referenceNumber,
                comment, invoiceDate, dueDate,
                totalCharge
        );
        invoice = saveOrUpdate(invoice, false);
        // let's move all the files from the temp folder to the invoice folder
        // and then remove the file

        Path tempFolderPath = getInvoiceDocumentPath(companyId, warehouseId, null);
        Path invoiceFolderPath = getInvoiceDocumentPath(companyId, warehouseId, invoice.getId());
        // if we add any document to this invoice, then we will need to
        // save the change to the database
        boolean documentAdded = false;
        for (InvoiceDocument invoiceDocument : invoiceDocuments) {

            File sourceFile = tempFolderPath.resolve(invoiceDocument.getRemoteFileName()).toFile();
            // only continue if the source file exists
            if (sourceFile.exists()) {
                logger.debug("source file {} exists for the new invoice {} / {}",
                        sourceFile.getAbsolutePath(), invoice.getId(), invoice.getNumber());

                File destinationFile = invoiceFolderPath.resolve(invoiceDocument.getRemoteFileName()).toFile();
                fileService.copyFile(sourceFile, destinationFile);
                logger.debug("invoice document is copied from {} to {}",
                        sourceFile.getAbsolutePath(), destinationFile.getAbsolutePath());

                logger.debug("source file {} is deleted? {}",
                        sourceFile.getAbsolutePath(), sourceFile.delete());

                // add the document to the invoice
                invoiceDocument.setInvoice(invoice);
                invoice.addDocument(invoiceDocument);
                documentAdded = true;
            }
            else {

                logger.debug("source file {} NOT exists for the new invoice {} / {}",
                        sourceFile.getAbsolutePath(), invoice.getId(), invoice.getNumber());
            }
        }

        // save the invoice again in case any document

        if (documentAdded) {
            invoice = saveOrUpdate(invoice);
        }
        return invoice;
    }
}
