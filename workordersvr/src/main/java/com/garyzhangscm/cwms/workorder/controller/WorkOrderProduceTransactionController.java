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

package com.garyzhangscm.cwms.workorder.controller;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.garyzhangscm.cwms.workorder.ResponseBodyWrapper;
import com.garyzhangscm.cwms.workorder.model.BillableEndpoint;
import com.garyzhangscm.cwms.workorder.model.WorkOrderProduceTransaction;
import com.garyzhangscm.cwms.workorder.service.WorkOrderProduceTransactionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

@RestController
public class WorkOrderProduceTransactionController {
    @Autowired
    WorkOrderProduceTransactionService workOrderProduceTransactionService;
    @Autowired
    private ObjectMapper objectMapper;



    @RequestMapping(value="/work-order-produce-transactions", method = RequestMethod.GET)
    public List<WorkOrderProduceTransaction> findAllWorkOrderProduceTransactions(
            @RequestParam Long warehouseId,
            @RequestParam(name="workOrderNumber", required = false, defaultValue = "") String workOrderNumber,
            @RequestParam(name="productionLineId", required = false, defaultValue = "") Long productionLineId,
            @RequestParam(name="genericMatch", required = false, defaultValue = "false") boolean genericQuery,
            @RequestParam(name = "startTime", required = false, defaultValue = "") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) ZonedDateTime startTime,
            @RequestParam(name = "endTime", required = false, defaultValue = "") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)  ZonedDateTime endTime,
            @RequestParam(name = "date", required = false, defaultValue = "") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
                                             ) {
        return workOrderProduceTransactionService.findAll(warehouseId, workOrderNumber,
                productionLineId,  genericQuery, startTime, endTime, date);
    }

    @BillableEndpoint
    @RequestMapping(value="/work-order-produce-transactions", method = RequestMethod.POST)
    public WorkOrderProduceTransaction addWorkOrderProduceTransaction(
            @RequestBody JsonNode request,
            @RequestParam(name = "rfCode", required = false, defaultValue = "") String rfCode,
            @RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestHeader(name = "username", required = false) String username,
            Authentication authentication) throws NoSuchAlgorithmException {
        try {
            WorkOrderProduceTransaction transaction = objectMapper.treeToValue(request, WorkOrderProduceTransaction.class);
            if (idempotencyKey == null) {
                return workOrderProduceTransactionService.startNewTransaction(transaction, rfCode);
            }
            String key = UUID.fromString(idempotencyKey).toString();
            if (!key.equalsIgnoreCase(idempotencyKey)) {
                throw new IllegalArgumentException("Idempotency-Key must be a UUID");
            }
            String actor = authentication != null ? authentication.getName()
                    : username == null || username.isBlank() ? "CWMS-USER" : username;
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(rfCode.getBytes(StandardCharsets.UTF_8));
            digest.update((byte) 0);
            byte[] body = objectMapper.writer().with(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS)
                    .writeValueAsBytes(objectMapper.convertValue(request, Object.class));
            return workOrderProduceTransactionService.startNewTransaction(
                    transaction, rfCode, key, actor, HexFormat.of().formatHex(digest.digest(body)));
        } catch (JsonProcessingException | IllegalArgumentException error) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid production submission", error);
        }
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ResponseBodyWrapper<Void>> productionSubmissionError(ResponseStatusException error) {
        return ResponseEntity.status(error.getStatusCode()).body(
                new ResponseBodyWrapper<>(error.getStatusCode().value(), error.getReason(), null));
    }

    @RequestMapping(value="/work-order-produce-transactions/{id}", method = RequestMethod.GET)
    public WorkOrderProduceTransaction findWorkOrderProduceTransaction(@PathVariable Long id) {
        return workOrderProduceTransactionService.findById(id);
    }





}
