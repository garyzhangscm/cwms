package com.garyzhangscm.cwms.integration.service;

import com.garyzhangscm.cwms.integration.clients.*;
import com.garyzhangscm.cwms.integration.exception.ResourceNotFoundException;
import com.garyzhangscm.cwms.integration.model.*;
import com.garyzhangscm.cwms.integration.repository.*;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.transaction.Transactional;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
public class DBBasedTrailerAppointmentIntegration {

    private static final Logger logger = LoggerFactory.getLogger(DBBasedTrailerAppointmentIntegration.class);

    @Autowired
    KafkaSender kafkaSender;
    @Autowired
    DBBasedTrailerAppointmentRepository dbBasedTrailerAppointmentRepository;
    @Autowired
    DBBasedStopRepository dbBasedStopRepository;
    @Autowired
    DBBasedShipmentRepository dbBasedShipmentRepository;
    @Autowired
    DBBasedShipmentLineRepository dbBasedShipmentLineRepository;
    @Autowired
    WarehouseLayoutServiceRestemplateClient warehouseLayoutServiceRestemplateClient;
    @Autowired
    InventoryServiceRestemplateClient inventoryServiceRestemplateClient;
    @Autowired
    CommonServiceRestemplateClient commonServiceRestemplateClient;
    @Autowired
    private OutbuondServiceRestemplateClient outbuondServiceRestemplateClient;

    @Value("${integration.record.process.limit:100}")
    int recordLimit;


    public List<DBBasedTrailerAppointment> findAll(String companyCode,
                                                   Long warehouseId, ZonedDateTime startTime, ZonedDateTime endTime, LocalDate date,
                                                   String statusList, Long id) {

        return dbBasedTrailerAppointmentRepository.findAll(
                (Root<DBBasedTrailerAppointment> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) -> {
                    List<Predicate> predicates = new ArrayList<Predicate>();

                    predicates.add(criteriaBuilder.equal(root.get("companyCode"), companyCode));

                    if (Objects.nonNull(warehouseId)) {
                        predicates.add(criteriaBuilder.equal(root.get("warehouseId"), warehouseId));

                    }

                    if (Objects.nonNull(startTime)) {
                        predicates.add(criteriaBuilder.greaterThanOrEqualTo(
                                root.get("createdTime"), startTime));

                    }

                    if (Objects.nonNull(endTime)) {
                        predicates.add(criteriaBuilder.lessThanOrEqualTo(
                                root.get("createdTime"), endTime));

                    }
                    logger.debug(">> Date is passed in {}", date);
                    if (Objects.nonNull(date)) {
                        LocalDateTime dateStartTime = date.atStartOfDay();
                        LocalDateTime dateEndTime = date.atStartOfDay().plusDays(1).minusSeconds(1);
                        predicates.add(criteriaBuilder.between(
                                root.get("createdTime"),
                                dateStartTime.atZone(ZoneOffset.UTC), dateEndTime.atZone(ZoneOffset.UTC)));
                    }

                    if (Strings.isNotBlank(statusList)) {
                        CriteriaBuilder.In<IntegrationStatus> inStatus = criteriaBuilder.in(root.get("status"));
                        for(String status : statusList.split(",")) {
                            inStatus.value(IntegrationStatus.valueOf(status));
                        }
                        predicates.add(criteriaBuilder.and(inStatus));
                    }

                    if (Objects.nonNull(id)) {
                        predicates.add(criteriaBuilder.equal(
                                root.get("id"), id));

                    }
                    Predicate[] p = new Predicate[predicates.size()];
                    return criteriaBuilder.and(predicates.toArray(p));
                }
        );
    }

    public DBBasedTrailerAppointment findById(Long id) {
        return dbBasedTrailerAppointmentRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.raiseException("Trailer Appointment data not found by id: " + id));
    }

    @Transactional
    public IntegrationTrailerAppointmentData addIntegrationTrailerAppointmentData(DBBasedTrailerAppointment dbBasedTrailerAppointment) {

        dbBasedTrailerAppointment.setStatus(IntegrationStatus.PENDING);
        dbBasedTrailerAppointment.getStops().forEach(
                dbBasedStop -> {
                    dbBasedStop.setTrailerAppointment(dbBasedTrailerAppointment);
                    dbBasedStop.setStatus(IntegrationStatus.ATTACHED);
                    dbBasedStop.setId(null);
                    dbBasedStop.getShipments().forEach(
                            dbBasedShipment -> {
                                dbBasedShipment.setStop(dbBasedStop);
                                dbBasedShipment.setStatus(IntegrationStatus.ATTACHED);
                                dbBasedShipment.setId(null);
                                dbBasedShipment.getShipmentLines().forEach(
                                        dbBasedShipmentLine -> {
                                            dbBasedShipmentLine.setShipment(dbBasedShipment);
                                            dbBasedShipmentLine.setStatus(IntegrationStatus.ATTACHED);
                                            dbBasedShipmentLine.setId(null);

                                        }
                                );
                            }
                    );
                }
        );
        dbBasedTrailerAppointment.setId(null);
        logger.debug("Start to save dbBasedTrailerAppointment: \n {}",
                dbBasedTrailerAppointment);
        return dbBasedTrailerAppointmentRepository.save(dbBasedTrailerAppointment);
    }

    private List<DBBasedTrailerAppointment> findPendingIntegration() {
        Pageable limit = PageRequest.of(0,recordLimit);

        Page<DBBasedTrailerAppointment> dbBasedTrailerAppointmentPage
                = dbBasedTrailerAppointmentRepository.findAll(
                (Root<DBBasedTrailerAppointment> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) -> {
                    List<Predicate> predicates = new ArrayList<Predicate>();

                    predicates.add(criteriaBuilder.equal(root.get("status"), IntegrationStatus.PENDING));

                    Predicate[] p = new Predicate[predicates.size()];
                    return criteriaBuilder.and(predicates.toArray(p));
                },
                limit
        );
        return dbBasedTrailerAppointmentPage.getContent();
    }

    private DBBasedTrailerAppointment save(DBBasedTrailerAppointment dbBasedTrailerAppointment) {
        return dbBasedTrailerAppointmentRepository.save(dbBasedTrailerAppointment);
    }

    public void listen() {
        logger.debug("Start to process trailer appointment data");
        List<DBBasedTrailerAppointment> dbBasedTrailerAppointments = findPendingIntegration();
        logger.debug(">> get {} trailer appointment data to be processed", dbBasedTrailerAppointments.size());
        dbBasedTrailerAppointments.forEach(dbBasedTrailerAppointment
                -> process(dbBasedTrailerAppointment));
    }

    private void process(DBBasedTrailerAppointment dbBasedTrailerAppointment) {

        try {

            TrailerAppointment trailerAppointment = dbBasedTrailerAppointment.convertToTrailerAppointment(
                    commonServiceRestemplateClient,
                    warehouseLayoutServiceRestemplateClient, outbuondServiceRestemplateClient);
            // setup the warehouse
            // Item item = getItemFromDatabase(dbBasedItem);
            logger.debug(">> will process trailerAppointment:\n{}", trailerAppointment);

            kafkaSender.send(IntegrationType.INTEGRATION_TRAILER_APPOINTMENT,
                    trailerAppointment.getCompanyId() + "-" +
                            (Objects.isNull(trailerAppointment.getWarehouseId()) ? "" : trailerAppointment.getWarehouseId())
                            + "-" + dbBasedTrailerAppointment.getId(), trailerAppointment);


            dbBasedTrailerAppointment.setErrorMessage("");
            dbBasedTrailerAppointment.completeIntegration(IntegrationStatus.SENT);


        }
        catch(Exception ex) {
            ex.printStackTrace();
            logger.debug("Exception : {} \n while process trailer appointment integration: \n{}",
                    ex.getMessage(), dbBasedTrailerAppointment);
            dbBasedTrailerAppointment.completeIntegration(IntegrationStatus.ERROR, ex.getMessage());


        }


        dbBasedTrailerAppointment = save(dbBasedTrailerAppointment);

        logger.debug(">> Trailer Appointment data process, {}", dbBasedTrailerAppointment);
    }


    public void saveIntegrationResult(IntegrationResult integrationResult) {
        logger.debug("will update the trailer appointment integration {}'s result to {}",
                integrationResult.getIntegrationId(),
                integrationResult.isSuccess());
        DBBasedTrailerAppointment dbBasedTrailerAppointment = findById(
                integrationResult.getIntegrationId()
        );
        IntegrationStatus integrationStatus =
                integrationResult.isSuccess() ? IntegrationStatus.COMPLETED : IntegrationStatus.ERROR;
        dbBasedTrailerAppointment.completeIntegration(integrationStatus, integrationResult.getErrorMessage());

        save(dbBasedTrailerAppointment);


    }


    public IntegrationTrailerAppointmentData resendTrailerAppointmentData(Long id) {
        DBBasedTrailerAppointment dbBasedTrailerAppointment =
                findById(id);
        dbBasedTrailerAppointment.setStatus(IntegrationStatus.PENDING);
        dbBasedTrailerAppointment.setErrorMessage("");
        return save(dbBasedTrailerAppointment);
    }


}
