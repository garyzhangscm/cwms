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

import com.garyzhangscm.cwms.outbound.clients.CommonServiceRestemplateClient;
import com.garyzhangscm.cwms.outbound.clients.InventoryServiceRestemplateClient;
import com.garyzhangscm.cwms.outbound.clients.ResourceServiceRestemplateClient;
import com.garyzhangscm.cwms.outbound.clients.WarehouseLayoutServiceRestemplateClient;
import com.garyzhangscm.cwms.outbound.exception.ResourceNotFoundException;
import com.garyzhangscm.cwms.outbound.model.*;
import com.garyzhangscm.cwms.outbound.model.Order;
import com.garyzhangscm.cwms.outbound.repository.PalletPickLabelContentRepository;
import com.garyzhangscm.cwms.outbound.repository.PalletPickLabelPickDetailRepository;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;

import javax.persistence.criteria.*;
import javax.transaction.Transactional;
import java.util.*;
import java.util.stream.Collectors;


@Service
public class PalletPickLabelContentService {
    private static final Logger logger = LoggerFactory.getLogger(PalletPickLabelContentService.class);


    @Autowired
    private PalletPickLabelContentRepository palletPickLabelContentRepository;
    @Autowired
    private PalletPickLabelPickDetailRepository palletPickLabelPickDetailRepository;
    @Autowired
    private PickService pickService;
    @Autowired
    private OutboundConfigurationService outboundConfigurationService;
    @Autowired
    private WalmartShippingCartonLabelService walmartShippingCartonLabelService;
    @Autowired
    private TargetShippingCartonLabelService targetShippingCartonLabelService;
    @Autowired
    private UnitService unitService;

    @Autowired
    private CommonServiceRestemplateClient commonServiceRestemplateClient;
    @Autowired
    private ResourceServiceRestemplateClient resourceServiceRestemplateClient;
    @Autowired
    private WarehouseLayoutServiceRestemplateClient warehouseLayoutServiceRestemplateClient;
    @Autowired
    private InventoryServiceRestemplateClient inventoryServiceRestemplateClient;


    public PalletPickLabelContent findById(Long id) {
        return palletPickLabelContentRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.raiseException("bulk picking not found by id: " + id));

    }

    public List<PalletPickLabelContent> findAll(Long warehouseId,
                                                Long orderId, String orderNumber) {
        return palletPickLabelContentRepository.findAll(
                (Root<PalletPickLabelContent> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) -> {
                    List<Predicate> predicates = new ArrayList<Predicate>();

                    predicates.add(criteriaBuilder.equal(root.get("warehouseId"), warehouseId));


                    if (Strings.isNotBlank(orderNumber) || Objects.nonNull(orderId)) {

                        Join<PalletPickLabelContent, Order> joinOrder = root.join("order", JoinType.INNER);
                        if (Strings.isNotBlank(orderNumber)) {
                            predicates.add(criteriaBuilder.equal(joinOrder.get("number"), orderNumber));
                        }
                        if (Objects.nonNull(orderId)) {
                            predicates.add(criteriaBuilder.equal(joinOrder.get("id"), orderId));
                        }
                    }


                    Predicate[] p = new Predicate[predicates.size()];
                    return criteriaBuilder.and(predicates.toArray(p));
                }
        );
    }

    public PalletPickLabelContent save(PalletPickLabelContent palletPickLabelContent) {
        return palletPickLabelContentRepository.save(palletPickLabelContent);
    }

    public PalletPickLabelContent saveOrUpdate(PalletPickLabelContent palletPickLabelContent) {
        if (Objects.isNull(palletPickLabelContent.getId()) &&
                Objects.nonNull(findByNumber(
                        palletPickLabelContent.getWarehouseId(), palletPickLabelContent.getNumber()
                ))) {
            palletPickLabelContent.setId(
                    findByNumber(palletPickLabelContent.getWarehouseId(), palletPickLabelContent.getNumber()).getId()
            );
        }
        return palletPickLabelContentRepository.save(palletPickLabelContent);
    }

    public PalletPickLabelContent findByNumber(Long warehouseId, String number) {
        return palletPickLabelContentRepository.findByWarehouseIdAndNumber(warehouseId, number);
    }

    public void delete(PalletPickLabelContent palletPickLabelContent) {

        // we will need to release the walmart shipping carton labels
        // that on this pallet so that the carton label can be group onto the new
        // pallet, if needed

        walmartShippingCartonLabelService.releaseShippingCartonLabel(palletPickLabelContent);
        targetShippingCartonLabelService.releaseShippingCartonLabel(palletPickLabelContent);

        palletPickLabelContentRepository.delete(palletPickLabelContent);

    }

    @Transactional
    public List<PalletPickLabelContent> generateAndSavePalletPickLabelEstimation(Order order) {
        // clear the existing estimation for the order
        List<PalletPickLabelContent> existingPalletPickLabelContents = findAll(
                order.getWarehouseId(), order.getId(), order.getNumber()
        );
        logger.debug("We have {} existing pallet pick labels information for this order {}, " +
                "let's remove them first", existingPalletPickLabelContents.size(),
                order.getNumber());
        existingPalletPickLabelContents.forEach(
                this::delete
        );

        // add new estimation and save it
        List<PalletPickLabelContent> palletPickLabelContents =
                generatePalletPickLabelEstimation(order);

        // setup the header / detail mapping so we can serialize
        // the 2 objects together
        palletPickLabelContents.forEach(
                palletPickLabelContent -> {
                    palletPickLabelContent.getPalletPickLabelPickDetails().forEach(
                            palletPickLabelPickDetail -> {
                                palletPickLabelPickDetail.setPalletPickLabelContent(palletPickLabelContent);
                            }
                    );
                }
        );

        // save the new result and return
        return
                palletPickLabelContents.stream().map(
                        palletPickLabelContent -> save(palletPickLabelContent)
                ).collect(Collectors.toList());

    }

    /**
     * Generate pallet pick labels for the order, one label per picks that can fill in
     * one pallet
     * @param order
     * @return
     */
    public List<PalletPickLabelContent> generatePalletPickLabelEstimation(Order order) {
        List<PalletPickLabelContent> result = new ArrayList<>();

        List<Pick> picks = pickService.findByOrder(order);
        logger.debug("start to estimate how many pallet will need for order {} " +
                "with {} picks",
                order.getNumber(),
                picks.size());
        // first of all, let's get all those full pallet pick and print
        // one label per pick for them
        result.addAll(
                picks.stream().filter(
                        pick -> pickService.isFullPalletPick(pick)
                ).map(
                        pick -> new PalletPickLabelContent(getNextPalletPickLabelNumber(pick.getWarehouseId()), pick, unitService)
                ).collect(Collectors.toList())
        );
        logger.debug("we added {} full pallet picks  for the order {}", result.size(),
                order.getNumber());

        result.forEach(
                palletPickLabelContent -> {
                    logger.debug(">> full pallet pick {}, item {}, quantity {}",
                            palletPickLabelContent.getPalletPickLabelPickDetails().get(0).getPick().getNumber(),
                            palletPickLabelContent.getPalletPickLabelPickDetails().get(0).getPick().getItem().getName(),
                            palletPickLabelContent.getPalletPickLabelPickDetails().get(0).getPick().getQuantity()
                            );
                }
        );

        List<Pick> partialPalletPicks = picks.stream()
                .filter(
                        pick -> !pickService.isFullPalletPick(pick)
                ).collect(Collectors.toList());
        logger.debug("we have {} partial pallet pick for the order {}",
                partialPalletPicks.size(), order.getNumber());

        partialPalletPicks.forEach(
                pick -> {
                    logger.debug(">> partial pallet pick {}, item {}, quantity {}",
                            pick.getNumber(),
                            pick.getItem().getName(),
                            pick.getQuantity()
                            );
                }
        );

        if (partialPalletPicks.isEmpty()) {
            // there's no partial pallet pick. let's end up here
            return result;

        }
        Pair<Double, Double> outboundPalletRestriction =
                getOutboundPalletRestriction(order);

        logger.debug("start to estimate based on the pallet restriction of size {} and height {}",
                outboundPalletRestriction.getFirst(), outboundPalletRestriction.getSecond());
        result.addAll(
                generatePalletPickLabelEstimation(order.getWarehouseId(), outboundPalletRestriction, partialPalletPicks)
        );
        logger.debug("we need {} pallets for the current order {} with {} picks",
                result.size(), order.getNumber(), picks.size());
        return result;
    }

    /**
     * group picks into pick based on the pallet volume restriction and height restiction
     * we will make our best estimation and try not to split pick into 2 pallet
     * @param picks
     * @return
     */
    public List<PalletPickLabelContent> generatePalletPickLabelEstimation(Long warehouseId,
                                                                          Pair<Double, Double> outboundPalletRestriction,
                                                                          List<Pick> picks) {

        double sizeRestriction = outboundPalletRestriction.getFirst() > 0 ? outboundPalletRestriction.getFirst() :
                Double.MAX_VALUE;
        double heightRestriction = outboundPalletRestriction.getSecond() > 0 ? outboundPalletRestriction.getSecond() :
                Double.MAX_VALUE;
        logger.debug("warehouse {} has pallet restriction size <= {} in3 and height <= {} in",
                warehouseId, sizeRestriction, heightRestriction);
        if (sizeRestriction == Double.MAX_VALUE && heightRestriction == Double.MAX_VALUE) {
            // if there's no restriction on the height and size, then let's group the picks into one pallet
            return List.of(
                    new PalletPickLabelContent(getNextPalletPickLabelNumber(warehouseId), picks, unitService)
            );
        }

        List<PalletPickLabelContent> results = new ArrayList<>();

        // let's sort the pick from volume biggest to smallest
        Collections.sort(picks,
                Collections.reverseOrder(Comparator.comparing(pick -> pick.getSize(unitService).getFirst())));

        // loop through biggest and until we max out the size or volume
        int biggestIndex = 0;
        int smallestIndex = picks.size() - 1;
        List<Pick> picksOnCurrentPallet = new ArrayList<>();

        double currentPalletSize = 0.0;
        double currentPalletHeight = 0.0;

        while(biggestIndex <= smallestIndex) {
            logger.debug("loop with biggestIndex = {}, smallestIndex = {}",
                    biggestIndex, smallestIndex);

            // see if we can add the biggest picks into the current pallet
            if (picksOnCurrentPallet.isEmpty()) {
                // we can always add one pick onto the pallet
                logger.debug("There's nothing on the current pallet yet, since we are reasonably assume" +
                        " that all the picks here are partial pallet pick, we will directly add the pick " +
                        " of index {} onto this empty pallet. pick number: {}, size = {} , height = {}  ",
                        biggestIndex,
                        picks.get(biggestIndex).getNumber(),
                        picks.get(biggestIndex).getSize(unitService),
                        picks.get(biggestIndex).getHeight(unitService) );
                picksOnCurrentPallet.add(picks.get(biggestIndex));
                currentPalletSize += picks.get(biggestIndex).getSize(unitService).getFirst();
                currentPalletHeight += picks.get(biggestIndex).getHeight(unitService).getFirst();

                biggestIndex ++;


                continue;
            }
            logger.debug("current pallet is not empty, let's see if we can either add the biggest pick" +
                    " or the smallest pick onto the pallet. we will start with the biggest pick first");
            // see if we can add the biggest pick onto this pallet
            logger.debug("current pallet size = {}, biggest pick size = {}, smallest pick size = {}, size restriction = {}",
                    currentPalletSize, picks.get(biggestIndex).getSize(unitService),
                    picks.get(smallestIndex).getSize(unitService), sizeRestriction);

            logger.debug("current pallet height = {}, biggest pick height = {}, smallest pick height = {}, height restriction = {}",
                    currentPalletHeight, picks.get(biggestIndex).getHeight(unitService),
                    picks.get(smallestIndex).getHeight(unitService),  heightRestriction);

            if (currentPalletSize + picks.get(biggestIndex).getSize(unitService).getFirst() < sizeRestriction &&
                currentPalletHeight + picks.get(biggestIndex).getHeight(unitService).getFirst() < heightRestriction) {
                // ok we are good to add the next pick to the current pallet

                logger.debug("We can add the biggest pick onto current pallet. " +
                                "pick number: {}, size = {}, height = {}",
                        picks.get(biggestIndex).getNumber(),
                        picks.get(biggestIndex).getSize(unitService),
                        picks.get(biggestIndex).getHeight(unitService));

                picksOnCurrentPallet.add(picks.get(biggestIndex));
                currentPalletSize += picks.get(biggestIndex).getSize(unitService).getFirst();
                currentPalletHeight += picks.get(biggestIndex).getHeight(unitService).getFirst();
                biggestIndex ++;
            }
            else if (currentPalletSize + picks.get(smallestIndex).getSize(unitService).getFirst() < sizeRestriction &&
                    currentPalletHeight + picks.get(smallestIndex).getHeight(unitService).getFirst() < heightRestriction) {
                // ok we are good to add the next pick to the current pallet

                logger.debug("We can add the smallest pick onto current pallet" +
                        "pick number: {}, size = {}, height = {}",
                        picks.get(smallestIndex).getNumber(),
                        picks.get(smallestIndex).getSize(unitService),
                        picks.get(smallestIndex).getHeight(unitService));

                picksOnCurrentPallet.add(picks.get(smallestIndex));
                currentPalletSize += picks.get(smallestIndex).getSize(unitService).getFirst();
                currentPalletHeight += picks.get(smallestIndex).getHeight(unitService).getFirst();
                smallestIndex --;
            }
            else {
                // ok there's no room for new pick in the list, let's complete the current pallet

                // add the pallet to the final list once we have something on it
                // we may add more onto the pallet during the iteration
                results.add(
                        new PalletPickLabelContent(
                                getNextPalletPickLabelNumber(warehouseId),
                                picksOnCurrentPallet, unitService
                        )
                );
                logger.debug("No more room on current pallet, let's start with a new empty pallet");
                // clear the temporary value so we can start a new pallet
                picksOnCurrentPallet = new ArrayList<>();
                currentPalletSize = 0.0;
                currentPalletHeight = 0.0;
            }


        }

        // handle the last pallet
        if (!picksOnCurrentPallet.isEmpty()) {

            // add the pallet to the final list once we have something on it
            // we may add more onto the pallet during the iteration
            results.add(
                    new PalletPickLabelContent(
                            getNextPalletPickLabelNumber(warehouseId),
                            picksOnCurrentPallet, unitService
                    )
            );
        }
        logger.debug("we got {} pallets for the picks", results.size());
        results.forEach(
                palletPickLabelContent -> {
                    logger.debug("====   Picks on Pallet # {} ==========",
                            palletPickLabelContent.getNumber());
                    palletPickLabelContent.getPalletPickLabelPickDetails().forEach(
                            palletPickLabelPickDetail -> {
                                logger.debug("# pick: {}, item: {}, quantity {}",
                                        palletPickLabelPickDetail.getPick().getNumber(),
                                        Objects.isNull(palletPickLabelPickDetail.getPick().getItem()) ? "N/A" :
                                            palletPickLabelPickDetail.getPick().getItem().getName(),
                                        palletPickLabelPickDetail.getPick().getQuantity());
                            }
                    );
                }
        );
        return results;

    }


    /**
     * Get pallet outbound restriction on volume and height. We will use this restriction
     * to estimate how many pallets the order may needs to ship
     * it will read restriction from customer and warheouse and get the smallest number
     * if the restirction is 0 or null, then it means there's no restriction
     * @param order
     * @return
     */
    private Pair<Double, Double> getOutboundPalletRestriction(Order order) {
        double customerSizeRestriction = 0.0;
        double customerHeightRestriction =  0.0;

        double warehouseSizeRestriction = 0.0;
        double warehouseHeightRestriction = 0.0;

        if (Objects.isNull(order.getShipToCustomer()) && Objects.nonNull(order.getShipToCustomerId())) {
            order.setShipToCustomer(
                    commonServiceRestemplateClient.getCustomerById(
                            order.getShipToCustomerId()
                    )
            );
        }
        if (Objects.nonNull(order.getShipToCustomer())) {
            customerSizeRestriction = Objects.isNull(order.getShipToCustomer().getMaxPalletSize()) || order.getShipToCustomer().getMaxPalletSize() <= 0?
                Double.MAX_VALUE : order.getShipToCustomer().getMaxPalletSize();
            customerHeightRestriction = Objects.isNull(order.getShipToCustomer().getMaxPalletHeight()) || order.getShipToCustomer().getMaxPalletHeight() <= 0?
                    Double.MAX_VALUE : order.getShipToCustomer().getMaxPalletHeight();
            logger.debug("order {}'s customer {} has a size restriction {} and height restriction {} on the pallet",
                    order.getNumber(),
                    order.getShipToCustomer().getName(),
                    customerSizeRestriction,
                    customerHeightRestriction);
        }


        OutboundConfiguration outboundConfiguration = outboundConfigurationService.findByWarehouse(
                order.getWarehouseId()
        );

        if (Objects.nonNull(outboundConfiguration)) {

            warehouseSizeRestriction = Objects.isNull(outboundConfiguration.getMaxPalletSize()) || outboundConfiguration.getMaxPalletSize() <= 0?
                    Double.MAX_VALUE : outboundConfiguration.getMaxPalletSize();
            warehouseHeightRestriction = Objects.isNull(outboundConfiguration.getMaxPalletHeight()) || outboundConfiguration.getMaxPalletHeight() <= 0?
                    Double.MAX_VALUE : outboundConfiguration.getMaxPalletHeight();

            logger.debug("outbound configuration has a size restriction {} and height restriction {} on the pallet",
                    warehouseSizeRestriction,
                    warehouseHeightRestriction);
        }

        double sizeRestriction = Math.min(customerSizeRestriction, warehouseSizeRestriction);
        double heightRestriction =  Math.min(customerHeightRestriction, warehouseHeightRestriction);

        logger.debug("Final result: size restriction {} and height restriction {} on the pallet",
                sizeRestriction,
                heightRestriction);

        return Pair.of(sizeRestriction, heightRestriction);
    }


    public List<ReportHistory> generatePalletPickLabel(Long warehouseId,
                                                 int copies, String locale,
                                                 PalletPickLabelContent palletPickLabelContent)   {
        return generatePalletPickLabel(warehouseId,
                copies, locale, palletPickLabelContent, 1, 1);
    }

    public List<ReportHistory> generatePalletPickLabel(Long warehouseId,
                                                 int copies, String locale,
                                                 PalletPickLabelContent palletPickLabelContent,
                                                 int index,
                                                 int totalLabelCount) {
        // since we will only print at max 6 picks on the label, if
        // there're more than 6 picks on the pallet, then we will print
        // multiple labels for this pallet label, the only difference between
        // those labels are the pick information. The head and footer of those labels
        // should be the same

        logger.debug("start to print pallet pick labels, which is {} of a total serious of {} pallet picks" +
                ", label number: {}, reference number: {}",
                index, totalLabelCount,
                palletPickLabelContent.getNumber(),
                palletPickLabelContent.getReferenceNumber());

        int palletLabelCount = ((palletPickLabelContent.getPalletPickLabelPickDetails().size() - 1) / 6) + 1;
        List<List<PalletPickLabelPickDetail>> palletPickLabelPickDetailsList = new ArrayList<>();
        for (int i = 0; i < palletLabelCount; i++) {
            int startIndex = i * 6;
            int endIndex = Math.min((i + 1) * 6, palletPickLabelContent.getPalletPickLabelPickDetails().size());

            palletPickLabelPickDetailsList.add(
                    palletPickLabelContent.getPalletPickLabelPickDetails().subList(
                            startIndex, endIndex
                    )
            );
        }

        return palletPickLabelPickDetailsList.stream().map(
                palletPickLabelPickDetails ->
                        generatePalletPickLabel(
                                warehouseId, copies, locale,
                                palletPickLabelContent,
                                palletPickLabelPickDetails,
                                index, totalLabelCount
                        )
        ).collect(Collectors.toList());
    }
    public ReportHistory generatePalletPickLabel(Long warehouseId,
                                                 int copies, String locale,
                                                 PalletPickLabelContent palletPickLabelContent,
                                                 List<PalletPickLabelPickDetail> palletPickLabelPickDetails,
                                                 int index, int totalLableCount) {
        Report reportData = new Report();


        setupPalletPickLabelData(
                reportData, copies, palletPickLabelContent,
                palletPickLabelPickDetails,
                index, totalLableCount );


        logger.debug("will call resource service to print the report with locale: {}",
                locale);
        logger.debug("Will print {} labels", reportData.getData().size());
        logger.debug("####   Report   Data  ######");
        logger.debug(reportData.toString());
        ReportHistory reportHistory =
                resourceServiceRestemplateClient.generateReport(
                        warehouseId, ReportType.PALLET_PICK_LABEL, reportData, locale
                );


        logger.debug("####   Report   printed: {}", reportHistory.getFileName());
        return reportHistory;
    }

    /**
     * Setup the label content for the pallet pick label
     * @param reportData
     * @param copies
     * @param palletPickLabelContent
     * @param index
     * @param totalLableCount
     */
    private void setupPalletPickLabelData(Report reportData, int copies,
                                          PalletPickLabelContent palletPickLabelContent,
                                          List<PalletPickLabelPickDetail> palletPickLabelPickDetails,
                                          int index,
                                          int totalLableCount) {
        Map<String, Object> labelContent = new HashMap<>();

        labelContent.put("number", palletPickLabelContent.getNumber());
        labelContent.put("reference_number", palletPickLabelContent.getReferenceNumber());
        labelContent.put("pallet_size", palletPickLabelContent.getVolume() + " " + palletPickLabelContent.getVolumeUnit());
        labelContent.put("pallet_height", palletPickLabelContent.getHeight() + " " + palletPickLabelContent.getHeightUnit());

        // get the total case quantity
        labelContent.put("total_cases",
                palletPickLabelContent.getPalletPickLabelPickDetails().stream().map(
                        PalletPickLabelPickDetail::getCaseQuantity
                ).mapToLong(Long::longValue).sum());

        labelContent.put("index", index);
        labelContent.put("total_label_count", totalLableCount);

        for(int i = 0; i < 6 && i < palletPickLabelPickDetails.size(); i++) {

            Pick pick = palletPickLabelPickDetails.get(i).getPick();
            if (Objects.isNull(pick.getSourceLocation())) {
                pick.setSourceLocation(
                        warehouseLayoutServiceRestemplateClient.getLocationById(
                                pick.getSourceLocationId()
                        )
                );
            }
            if (Objects.isNull(pick.getItem())) {
                pick.setItem(
                        inventoryServiceRestemplateClient.getItemById(
                                pick.getItemId()
                        )
                );
            }
            labelContent.put("source_location_" + (i+1), pick.getSourceLocation().getName());
            labelContent.put("item_" + (i+1), pick.getItem().getName());
            labelContent.put("item_description_" + (i+1), pick.getItem().getDescription());
            labelContent.put("quantity_" + (i+1),
                    palletPickLabelPickDetails.get(i).getPickQuantity() +
                            "(" + palletPickLabelPickDetails.get(i).getCaseQuantity() + " " + palletPickLabelPickDetails.get(i).getCaseUnitOfMeasureName() + ")" );

        }


        List<Map<String, Object>> labelContents = new ArrayList<>();

        for (int i = 0; i < copies; i++) {
            labelContents.add(labelContent);
        }


        reportData.setData(labelContents);
    }

    public String getNextPalletPickLabelNumber(Long warehouseId) {
        return commonServiceRestemplateClient.getNextNumber(warehouseId, "pallet-pick-label-number");
    }

    private PalletPickLabelPickDetail findPalletPickLabelPickDetailByPick(Pick pick) {
        return palletPickLabelPickDetailRepository.findByPickId(pick.getId());
    }

    /**
     * When a pick is removed, let's remove the correspondent pallet pick
     * @param pick
     */
    public void onPickRemove(Pick pick) {
        PalletPickLabelPickDetail palletPickLabelPickDetail =
                findPalletPickLabelPickDetailByPick(pick);
        if (Objects.nonNull(palletPickLabelPickDetail)) {

            // this is the only pick in the pallet, let's remove the whole pallet
            if (palletPickLabelPickDetail.getPalletPickLabelContent().getPalletPickLabelPickDetails().size() <= 1){
                delete(palletPickLabelPickDetail.getPalletPickLabelContent());
            }
            else {
                // there's more in the pallet, let's just remove the pick
                palletPickLabelPickDetailRepository.delete(palletPickLabelPickDetail);
            }
        }

    }
}