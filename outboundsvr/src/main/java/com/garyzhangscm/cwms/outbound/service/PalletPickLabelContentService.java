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
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;

import javax.persistence.criteria.*;
import java.util.*;
import java.util.stream.Collectors;


@Service
public class PalletPickLabelContentService {
    private static final Logger logger = LoggerFactory.getLogger(PalletPickLabelContentService.class);


    @Autowired
    private PalletPickLabelContentRepository palletPickLabelContentRepository;
    @Autowired
    private PickService pickService;
    @Autowired
    private OutboundConfigurationService outboundConfigurationService;

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
        palletPickLabelContentRepository.delete(palletPickLabelContent);
    }

    public List<PalletPickLabelContent> generateAndSavePalletPickLabelEstimation(Order order) {
        // clear the existing estimation for the order
        List<PalletPickLabelContent> existingPalletPickLabelContents = findAll(
                order.getWarehouseId(), order.getId(), order.getNumber()
        );
        existingPalletPickLabelContents.forEach(
                this::delete
        );

        // add new estimation and save it
        List<PalletPickLabelContent> palletPickLabelContents =
                generatePalletPickLabelEstimation(order);

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
        // first of all, let's get all those full pallet pick and print
        // one label per pick for them
        result.addAll(
                picks.stream().filter(
                        pick -> pickService.isFullPalletPick(pick)
                ).map(
                        pick -> new PalletPickLabelContent(getNextPalletPickLabelNumber(pick.getWarehouseId()), pick)
                ).collect(Collectors.toList())
        );

        List<Pick> partialPalletPick = picks.stream()
                .filter(
                        pick -> pickService.isFullPalletPick(pick)
                ).collect(Collectors.toList());
        Pair<Double, Double> outboundPalletRestriction =
                getOutboundPalletRestriction(order);

        result.addAll(
                generatePalletPickLabelEstimation(order.getWarehouseId(), outboundPalletRestriction, partialPalletPick)
        );
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
        if (sizeRestriction == Double.MAX_VALUE && heightRestriction == Double.MAX_VALUE) {
            // if there's no restriction on the height and size, then let's group the picks into one pallet
            return List.of(
                    new PalletPickLabelContent(getNextPalletPickLabelNumber(warehouseId), picks)
            );
        }

        List<PalletPickLabelContent> results = new ArrayList<>();

        // let's sort the pick from volume biggest to smallest
        Collections.sort(picks,
                Collections.reverseOrder(Comparator.comparing(Pick::getSize)));

        // loop through biggest and until we max out the size or volume
        int biggestIndex = 0;
        int smallestIndex = picks.size() - 1;
        List<Pick> picksOnCurrentPallet = new ArrayList<>();
        double currentPalletSize = 0.0;
        double currentPalletHeight = 0.0;

        while(biggestIndex <= smallestIndex) {
            // see if we can add the biggest picks into the current pallet
            if (picksOnCurrentPallet.isEmpty()) {
                // we can always add one pick onto the pallet
                picksOnCurrentPallet.add(picks.get(biggestIndex));
                currentPalletSize += picks.get(biggestIndex).getSize();
                currentPalletHeight += picks.get(biggestIndex).getHeight();

                biggestIndex ++;

                continue;
            }
            // see if we can add the biggest pick onto this pallet
            if (currentPalletSize + picks.get(biggestIndex).getSize() < sizeRestriction &&
                currentPalletHeight + picks.get(biggestIndex).getHeight() < heightRestriction) {
                // ok we are good to add the next pick to the current pallet

                picksOnCurrentPallet.add(picks.get(biggestIndex));
                currentPalletSize += picks.get(biggestIndex).getSize();
                currentPalletHeight += picks.get(biggestIndex).getHeight();
                biggestIndex ++;
            }
            else if (currentPalletSize + picks.get(smallestIndex).getSize() < sizeRestriction &&
                    currentPalletHeight + picks.get(smallestIndex).getHeight() < heightRestriction) {
                // ok we are good to add the next pick to the current pallet

                picksOnCurrentPallet.add(picks.get(smallestIndex));
                currentPalletSize += picks.get(smallestIndex).getSize();
                currentPalletHeight += picks.get(smallestIndex).getHeight();
                smallestIndex --;
            }
            else {
                // ok there's no room for new pick in the list, let's complete the current pallet
                results.add(
                        new PalletPickLabelContent(
                                getNextPalletPickLabelNumber(warehouseId),
                                picksOnCurrentPallet
                        )
                );
                // clear the temporary value so we can start a new pallet
                picksOnCurrentPallet = new ArrayList<>();
                currentPalletSize = 0.0;
                currentPalletHeight = 0.0;
            }


        }
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
        if (Objects.isNull(order.getShipToCustomer())) {
            customerSizeRestriction = Objects.isNull(order.getShipToCustomer().getMaxPalletSize()) ?
                0.0 : order.getShipToCustomer().getMaxPalletSize();
            customerHeightRestriction = Objects.isNull(order.getShipToCustomer().getMaxPalletHeight()) ?
                0.0 : order.getShipToCustomer().getMaxPalletHeight();
        }


        OutboundConfiguration outboundConfiguration = outboundConfigurationService.findByWarehouse(
                order.getWarehouseId()
        );

        if (Objects.nonNull(outboundConfiguration)) {

            warehouseSizeRestriction = Objects.isNull(outboundConfiguration.getMaxPalletSize()) ?
                    0.0 : outboundConfiguration.getMaxPalletSize();
            warehouseHeightRestriction = Objects.isNull(outboundConfiguration.getMaxPalletHeight()) ?
                    0.0 : outboundConfiguration.getMaxPalletHeight();
        }

        double sizeRestriction = Math.max(0, Math.min(customerSizeRestriction, warehouseSizeRestriction));
        double heightRestriction =  Math.max(0, Math.min(customerHeightRestriction, warehouseHeightRestriction));
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
                                                 int totalLableCount) {
        // since we will only print at max 6 picks on the label, if
        // there're more than 6 picks on the pallet, then we will print
        // multiple labels for this pallet label, the only difference between
        // those labels are the pick information. The head and footer of those labels
        // should be the same

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
                                index, totalLableCount
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
                        warehouseId, ReportType.WALMART_SHIPPING_CARTON_LABEL, reportData, locale
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
        labelContent.put("pallet_size", palletPickLabelContent.getVolume());
        labelContent.put("pallet_height", palletPickLabelContent.getHeight());

        // get the total case quantity
        labelContent.put("case_quantity",
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
            labelContent.put("item_description" + (i+1), pick.getItem().getDescription());
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
}