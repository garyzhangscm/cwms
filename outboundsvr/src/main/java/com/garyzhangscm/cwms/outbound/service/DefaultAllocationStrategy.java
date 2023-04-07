package com.garyzhangscm.cwms.outbound.service;

import com.garyzhangscm.cwms.outbound.clients.InventoryServiceRestemplateClient;
import com.garyzhangscm.cwms.outbound.exception.AllocationException;
import com.garyzhangscm.cwms.outbound.exception.GenericException;
import com.garyzhangscm.cwms.outbound.exception.ShortAllocationException;
import com.garyzhangscm.cwms.outbound.model.*;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;

import javax.transaction.Transactional;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class DefaultAllocationStrategy implements AllocationStrategy {

    private static final Logger logger = LoggerFactory.getLogger(DefaultAllocationStrategy.class);

    @Autowired
    protected PickService pickService;

    @Autowired
    private AllocationTransactionHistoryService allocationTransactionHistoryService;

    @Autowired
    protected InventorySummaryService inventorySummaryService;

    @Autowired
    protected ShortAllocationService shortAllocationService;

    @Autowired
    protected InventoryServiceRestemplateClient inventoryServiceRestemplateClient;


    @Override
    public AllocationStrategyType getType() {
        return AllocationStrategyType.FIRST_IN_FIRST_OUT;
    }

    @Override
    @Transactional
    public AllocationResult allocate(AllocationRequest allocationRequest) {
        return allocate(allocationRequest, null);
    }

    /**
     * Allocate from certain location. Allocate from any location(s) when sourceLocation is null
     * @param allocationRequest
     * @param sourceLocation
     * @return
     */
    @Override
    @Transactional
    public AllocationResult allocate(AllocationRequest allocationRequest, Location sourceLocation) {
        Item item = allocationRequest.getItem();
        Long openQuantity = allocationRequest.getQuantity();
        InventoryStatus inventoryStatus = allocationRequest.getInventoryStatus();

        logger.debug("Start to allocate request with FIFO. \n item: {} / {} \n quantity: {} \n inventory status: {}, from location {}",
                item.getId(),
                allocationRequest.getItem().getName(),
                allocationRequest.getQuantity(),
                inventoryStatus.getName(),
                Objects.isNull(sourceLocation) ? "N/A" : sourceLocation.getName());


        List<Pick> existingPicks =
                        pickService.getOpenPicksByItemIdAndSourceLocation(item.getId(), sourceLocation);

        // Let's get all the pickable inventory and existing picks to the trace file
        logger.debug("We have {} existing picks against this item, let's future filter out by inventory attribute", existingPicks.size());


        existingPicks.stream().forEach(pick -> {
            logger.debug("pick # {}, source location: {}, destination location: {}, quantity: {}, picked quantity: {}",
                    pick.getNumber(), pick.getSourceLocation().getName(),
                    Objects.isNull(pick.getDestinationLocation()) ?
                    "N/A" : pick.getDestinationLocation().getName(),
                    pick.getQuantity(), pick.getPickedQuantity());
        });

        List<Inventory> pickableInventory
                = inventoryServiceRestemplateClient.getPickableInventory(
                item.getId(), inventoryStatus.getId(),
                Objects.isNull(sourceLocation) ?  null : sourceLocation.getId(),
                allocationRequest.getColor(),
                allocationRequest.getProductSize(),
                allocationRequest.getStyle());

        // for manual pick, we will filter out the inventory to specific LPN
        if (Boolean.TRUE.equals(allocationRequest.isManualAllocation()) &&
                Strings.isNotBlank(allocationRequest.getLpn())) {
            pickableInventory = pickableInventory.stream().filter(
                    inventory -> allocationRequest.getLpn().equals(inventory.getLpn())
            ).collect(Collectors.toList());
        }

        // Let's get all the pickable inventory and existing picks to the trace file
        logger.debug("We have {} pickable inventory of this item, location specified? {}",
                pickableInventory.size(),
                Objects.isNull(sourceLocation) ? "N/A" : sourceLocation.getName());

        List<InventorySummary> inventorySummaries = sort(inventorySummaryService.getInventorySummaryForAllocation(pickableInventory));



        logger.debug("We have inventory snapshot for allocation:");
        inventorySummaries.stream().forEach(inventorySummary -> {
            logger.debug("Inventory location: {},  quantity: {}",
                    inventorySummary.getLocation().getName(),
                    inventorySummary.getQuantity());
        });

        return allocate(allocationRequest, item, openQuantity,
                inventorySummaries, existingPicks);
    }

    @Override
    public boolean isDefault() {
        return false;
    }

    /**
     * Allocate inventory. We will try those four possible ways to allocate
     * 1. Allocate by LPN(only if allowed)
     * 2. Allocate by UOM
     * 3. Allocate by LPN and round up(only if both LPN and Round up are allowed)
     * 4. Allocate by LPN and round up(only if both LPN and Round up are allowed)
     * We will provide a default implementation of each allocation method here but
     * recommend a more specific allocation strategy to override
     * @param allocationRequest  Allocation request
     * @param item Item to be allocated
     * @param totalQuantity Total quantity to be allocated
     * @param inventorySummaries Inventory candidate for allocation
     * @param existingPicks open picks from those inventory candidate
     * @return
     */
    @Transactional
    protected AllocationResult allocate(AllocationRequest allocationRequest,
                                        Item item, Long totalQuantity,
                                         List<InventorySummary> inventorySummaries,
                                         List<Pick> existingPicks) {

        displayInventorySummaryInformation(inventorySummaries);
        // All picks generated by this allocation will be saved in the list
        AllocationResult allocationResult = new AllocationResult();

        // if we are processing manually allocation, then we won't allow allocation by LPN or round up
        boolean allocationByLPN = allocationRequest.isManualAllocation() ? false : isAllocateByLPNAllowed(item);

        AllocationRoundUpStrategy allocationRoundUpStrategy =
                allocationRequest.isManualAllocation() ? AllocationRoundUpStrategy.none() : getAllocationRoundUpStrategy(item);

        // openQuantity is the quantity that still left to be allocated
        long openQuantity = totalQuantity;

        if (allocationByLPN) {
            // try allocating by LPN, without round up first
            allocationResult.addPicks(tryAllocateByLPN(allocationRequest, openQuantity, inventorySummaries, existingPicks,
                    AllocationRoundUpStrategy.none()));
        }
        else {
            allocationTransactionHistoryService.createAndSendEmptyAllocationTransactionHistory(
                    allocationRequest, null, openQuantity, true, false,
                    "allocation by lpn is not allowed for the item"
            );
        }
        // Let's see how many we have allocated
        openQuantity = totalQuantity - getTotalPickQuantity(allocationResult);
        logger.debug("After allocate by LPN without round up, we still need {} of item {}",
                openQuantity, item.getName());
        if(openQuantity <= 0) {
            // OK, we allocated enough quantity, let's return
            return allocationResult;
        }

        // If we are here, we know we don't have enough allocated quantity, we will try
        // allocate by UOM, without round up

        allocationResult.addPicks(tryAllocateByUnitOfMeasure(allocationRequest, openQuantity, inventorySummaries, existingPicks,
                AllocationRoundUpStrategy.none()));


        // Let's see how many we have allocated
        openQuantity = totalQuantity - getTotalPickQuantity(allocationResult);
        logger.debug("After allocate by UOM without round up, we still need {} of item {}",
                openQuantity, item.getName());
        if(openQuantity <= 0) {
            // OK, we allocated enough quantity, let's return
            return allocationResult;
        }


        //
        // If we are still here, we know we are still short of some quantity
        // We will try round up the quantity to either full LPN quantity
        // or whole UOM quantity (only if allowed)
        //
        if (allocationRoundUpStrategy.isRoundUpAllowed()) {
            logger.debug("Round up is allowed for the item {}, we will try allocating by rounding up",
                    item.getName());
            if (allocationByLPN) {
                // try allocating by LPN, without round up first
                allocationResult.addPicks(tryAllocateByLPN(allocationRequest, openQuantity, inventorySummaries, existingPicks,
                        allocationRoundUpStrategy));
            }
            else {
                allocationTransactionHistoryService.createAndSendEmptyAllocationTransactionHistory(
                        allocationRequest, null, openQuantity, true, true,
                        "allocation by lpn is not allowed for the item"
                );
            }
            // Let's see how many we have allocated
            openQuantity = totalQuantity - getTotalPickQuantity(allocationResult);

            logger.debug("After allocate by lpn with round up, we still need {} of item {}",
                    openQuantity, item.getName());
            if(openQuantity <= 0) {
                // OK, we allocated enough quantity, let's return
                return allocationResult;
            }
            // If we are here, we know we don't have enough allocated quantity, we will try
            // allocate by UOM, without round up

            allocationResult.addPicks(tryAllocateByUnitOfMeasure(allocationRequest, openQuantity, inventorySummaries, existingPicks,
                    allocationRoundUpStrategy));


            // Let's see how many we have allocated
            openQuantity = totalQuantity - getTotalPickQuantity(allocationResult);
            logger.debug("After allocate by UOM with round up, we still need {} of item {}",
                    openQuantity, item.getName());
            if(openQuantity <= 0) {
                // OK, we allocated enough quantity, let's return
                return allocationResult;
            }
        }

        // If we are still here, we are short of some quantity
        Long shortQuantity = totalQuantity - getTotalPickQuantity(allocationResult);
        if (shortQuantity > 0) {

            logger.debug("We are still short {} of item {}",
                    shortQuantity, item.getName());
            allocationResult.addShortAllocation(generateShortAllocation(allocationRequest, item, shortQuantity));
        }
        return allocationResult;



    }

    private long getTotalPickQuantity(AllocationResult allocationResult) {
        return allocationResult.getPicks().stream().map(Pick::getQuantity).mapToLong(Long::longValue).sum();
    }

    @Transactional(dontRollbackOn = GenericException.class)
    private List<Pick> tryAllocateByUnitOfMeasure(AllocationRequest allocationRequest, long openQuantity,
                                                  List<InventorySummary> inventorySummaries,
                                                  List<Pick> existingPicks,
                                                  AllocationRoundUpStrategy allocationRoundUpStrategy){

        List<Pick> picks = new ArrayList<>();
        long totalQuantityToBeAllocated = openQuantity;
        // Let's try to allocate by LPN
        for(InventorySummary inventorySummary : inventorySummaries) {

            if (totalQuantityToBeAllocated <=0) {
                // we got enough quantity
                // let's return;
                break;
            }

            ItemUnitOfMeasure smallestPickableUnitOfMeasure =
                    getSmallestPickableUnitOfMeasure(inventorySummary);


            if (Objects.isNull(smallestPickableUnitOfMeasure)) {
                logger.debug("No pickable unit of measure defined for the inventory summary, {} / {}",
                        inventorySummary.getItem().getName(), inventorySummary.getItemPackageType().getName());
                allocationTransactionHistoryService.createAndSendEmptyAllocationTransactionHistory(
                        allocationRequest,
                        inventorySummary.getLocation(),
                        totalQuantityToBeAllocated,
                        false,
                        allocationRoundUpStrategy.isRoundUpAllowed() ? true: false,
                        "No pickable unit of measure defined for the inventory summary," +
                         inventorySummary.getItem().getName() + " / " + inventorySummary.getItemPackageType().getName()
                );
                continue;
            }


            logger.debug("Will try to allocate with smallestPickableUnitOfMeasureQuantity: {}",
                    smallestPickableUnitOfMeasure.getQuantity());

            // Get the quantity that already allocated to certain pick
            // we will need to make sure even after we allocate from certain LPN,
            // we will still have enough quantity for the existing picks
            // to verify the quantity, we will skip the quantity that 'allocated by LPN' type
            // of picks
            List<Pick> existingPicksByInventorySummary = getExistingPicksByInventorySummary(inventorySummary, existingPicks);

            // pickByQuantityPicksTotalOpenQuantity will return all open quantity that allocated
            // by NON LPN picks
            // if we are working on a manual picking process, then we will ignore the existing picks
            // as the manual picking will always be the highest priority
            // getAvailableInventoryQuantity will return all available quantity that excludes
            // the LPN that has been allocated by LPN picks.
            // the balance of those 2 number is the available quantity that can be allocated at this moment
            long pickByQuantityPicksTotalOpenQuantity =
                    Boolean.TRUE.equals(allocationRequest.isManualAllocation()) ?
                            0 : pickByQuantityPicksTotalOpenQuantity(existingPicksByInventorySummary);
            long totalInventoryQuantity =
                    inventorySummary.getInventories().values().stream().flatMap(List::stream).mapToLong(Inventory::getQuantity).sum();
            long availableInventoryQuantity = getAvailableInventoryQuantity(inventorySummary);

            long allocatibleQuantity = getAllocatiableQuantityByUnitOfMeasure(
                    (availableInventoryQuantity - pickByQuantityPicksTotalOpenQuantity),
                    smallestPickableUnitOfMeasure.getQuantity(),
                    totalQuantityToBeAllocated,
                    allocationRoundUpStrategy
                    );

            logger.debug("We can allocate {} from location {}, item {}", allocatibleQuantity,
                    inventorySummary.getLocation().getName(), inventorySummary.getItem().getName());

            if (allocatibleQuantity <= 0) {
                // we can't allocate anything from this location
                allocationTransactionHistoryService.createAndSendEmptyAllocationTransactionHistory(
                        allocationRequest,
                        inventorySummary.getLocation(),
                        totalQuantityToBeAllocated,
                        false,
                        allocationRoundUpStrategy.isRoundUpAllowed() ? true: false,
                        "No allocatable quantity in the location"
                );
                continue;
            }

            // total allocated quantity
            long totalAllocatedQuantity = 0;
            try {

                // we will try to make sure each pick's quantity won't exceed the LPN's quantity
                // in some case, it make no sense to pick more than one LPN each travel
                // key: LPN
                // value: LPN's total quantity
                Map<String, Long> lpnQuantites = new HashMap<>();
                inventorySummary.getInventories().entrySet().forEach(
                        entrySet -> {
                            String lpn = entrySet.getKey();
                            List<Inventory> inventories = entrySet.getValue();
                            Long quantityOnLPN = inventories.stream().mapToLong(Inventory::getQuantity).sum();
                            lpnQuantites.put(lpn, quantityOnLPN);
                        }
                );
                // we will sort the map by LPN's total quantities
                List<Map.Entry<String, Long>> sortedLPNQuantitesList = new ArrayList<>(lpnQuantites.entrySet());
                sortedLPNQuantitesList.sort(Map.Entry.comparingByValue());

                Map<String, Long> sortedLPNQuantites = new LinkedHashMap<>();
                for (Map.Entry<String, Long> entry : sortedLPNQuantitesList) {
                    sortedLPNQuantites.put(entry.getKey(), entry.getValue());
                }

                Iterator<Map.Entry<String, Long>> sortedLPNQuantityIterator = sortedLPNQuantites.entrySet().iterator();


                while (allocatibleQuantity > 0 && totalQuantityToBeAllocated > 0
                        && sortedLPNQuantityIterator.hasNext())
                {
                    // we will always generate one pick work for each LPN. in some scenario
                    // we think LPN stands for pallet and there's no way to pick more than one
                    // pallet in one transaction. So it is better to split the pick work based
                    // on the LPN quantity. If the user is able to pick 2 pallet(or 2 LPN) in one
                    // travel, then we can either use pick list to group the picks, or just
                    // confirm 2 or more picks before deposit
                    Map.Entry<String, Long> quantityOnLpn = sortedLPNQuantityIterator.next();
                    logger.debug("start to allocate from lpn {}, quantity {}",
                            quantityOnLpn.getKey(), quantityOnLpn.getValue());
                    Long allocatedQuantity = Math.min(quantityOnLpn.getValue(), allocatibleQuantity);
                    String allocatedLpn = quantityOnLpn.getKey();

                    logger.debug("will allocate from lpn {}, original quantity {}, allocated quantity {}",
                            quantityOnLpn.getKey(), quantityOnLpn.getValue(),
                            allocatedQuantity);

                    Pick pick = tryCreatePickForUOMAllocation(allocationRequest, inventorySummary, allocatedQuantity, smallestPickableUnitOfMeasure);
                    picks.add(pick);

                    // SAVE the allocation transaction
                    allocationTransactionHistoryService.createAndSendAllocationTransactionHistory(
                            allocationRequest,
                            inventorySummary.getLocation(),
                            totalQuantityToBeAllocated,
                            totalInventoryQuantity,
                            availableInventoryQuantity,
                            allocatedQuantity,
                            pickByQuantityPicksTotalOpenQuantity,
                            false,
                            false,
                            allocationRoundUpStrategy.isRoundUpAllowed() ? true: false,
                            ""
                    );

                    // update all the quantities
                    allocatibleQuantity -= allocatedQuantity;
                    totalQuantityToBeAllocated -= allocatedQuantity;
                    totalAllocatedQuantity += allocatedQuantity;

                    logger.debug("We are able to allocate {} from LPN {}, after this LPN, we still need to allocate {}, " +
                            " there's still quantity {} left in this inventory summary",
                            allocatedQuantity, allocatedLpn, totalQuantityToBeAllocated,
                            allocatibleQuantity, inventorySummary.getLocation().getName());
                    sortedLPNQuantityIterator.remove();
                }



            }
            catch (GenericException ex) {
                // in case we can't generate the pick from this location, let's
                // continue and try next location
                logger.debug("Get error {} during allocation, we will ignore", ex.getMessage());
                ex.printStackTrace();
                continue;
            }

            if (totalAllocatedQuantity == 0) {

                // we can't allocate anything from this location
                allocationTransactionHistoryService.createAndSendEmptyAllocationTransactionHistory(
                        allocationRequest,
                        inventorySummary.getLocation(),
                        totalQuantityToBeAllocated,
                        false,
                        allocationRoundUpStrategy.isRoundUpAllowed() ? true: false,
                        "We are not able to allocate anything from this location"
                );
            }






        }
        return picks;
    }

    /**
     * Create picks based on UOM, note we will ignore the exception here and won't rollback
     * any DB transaction. The exception will be handled in the up stream and we will either
     * try the next location, or generate a short allocation, in both case we will update the
     * changes to the order and shipment
     * @param allocationRequest allocation request
     * @param inventorySummary inventory summary
     * @param allocatibleQuantity allocatible quantity
     * @param smallestPickableUnitOfMeasure smallest pickable unit of measure
     * @return
     */
    @Transactional(dontRollbackOn = GenericException.class)
    private Pick tryCreatePickForUOMAllocation(AllocationRequest allocationRequest, InventorySummary inventorySummary,
                                               long allocatibleQuantity, ItemUnitOfMeasure smallestPickableUnitOfMeasure) {
        if (allocationRequest.getShipmentLines().size() > 0) {
            return pickService.generatePick(inventorySummary, allocationRequest.getShipmentLines().get(0),
                    allocatibleQuantity, smallestPickableUnitOfMeasure);
        }
        else if(allocationRequest.getWorkOrder() != null &&
                allocationRequest.getWorkOrderLines().size() > 0) {

            return pickService.generatePick(allocationRequest.getWorkOrder() ,
                    inventorySummary, allocationRequest.getWorkOrderLines().get(0),
                    allocatibleQuantity, smallestPickableUnitOfMeasure,
                    allocationRequest.getDestinationLocationId());
        }
        else {
            throw AllocationException.raiseException("now we only support new allocation for single shipment line or single work order line");
        }

    }

    /**
     * Check how many quantity we can allocate from the location
     * @param avaiableQuantity total available quantity in the location
     * @param pickableUnitOfMeasureQuantity pickable UOM quantity. We can only pick by multiple of this quantity
     * @param quantityToBeAllocated total quantity we need
     * @param allocationRoundUpStrategy round up strategy
     * @return quantity can be allocated from this location
     */
    private long getAllocatiableQuantityByUnitOfMeasure(long avaiableQuantity,
                                                        long pickableUnitOfMeasureQuantity,
                                                        long quantityToBeAllocated,
                                                        AllocationRoundUpStrategy allocationRoundUpStrategy) {

        logger.debug("getAllocatiableQuantityByUnitOfMeasure: \n " +
                "avaiableQuantity: {}\n" +
                "pickableUnitOfMeasureQuantity: {}\n" +
                "quantityToBeAllocated: {}\n" +
                "allocationRoundUpStrategy: {}",
                avaiableQuantity,
                pickableUnitOfMeasureQuantity,
                quantityToBeAllocated,
                allocationRoundUpStrategy);
        if (pickableUnitOfMeasureQuantity > avaiableQuantity) {
            // we can't even pick a uint of measure from this location,

            logger.debug("we don't have enough quantity in the location, even for one unit of measure " +
                    "pickableUnitOfMeasureQuantity: {}, avaiableQuantity: {}",
                    pickableUnitOfMeasureQuantity, avaiableQuantity);
            return 0;
        }

        // we are only allowed to pick by unit of measure
        long avaiableQuantityInUnitOfMeasure = avaiableQuantity / pickableUnitOfMeasureQuantity;

        logger.debug("avaiableQuantityInUnitOfMeasure: {}",
                avaiableQuantityInUnitOfMeasure);

        // if we have quantity less than required, then allocate them all
        if (avaiableQuantityInUnitOfMeasure * pickableUnitOfMeasureQuantity <= quantityToBeAllocated) {

            logger.debug("we don't have enough quantity in the location. return what ever we have: " +
                            "avaiableQuantityInUnitOfMeasure * pickableUnitOfMeasureQuantity: {}",
                    avaiableQuantityInUnitOfMeasure * pickableUnitOfMeasureQuantity);

            return avaiableQuantityInUnitOfMeasure * pickableUnitOfMeasureQuantity;
        }
        else {

            logger.debug("We have enough quantity in the location, let's try some more strategy");
            // we have more than required, we will still need to allocate by unit of measure
            // 1. if quantityToBeAllocated can be divided evenly by UOM quantity, then we allocate
            //    just the required quantity
            // 2. if quantityToBeAllocated can not be divided evenly by UOM quantity, and round up is not
            //    allowed, then we will allocate as mush as possible but not exceed the requirement amount
            // 3. if quantityToBeAllocated  can not be divided evenly by UOM quantity, and round up is  allowed,
            //    then we may allocate more than needed, but as less over allocation as possible
            long unitOfMeasureQuantityToBeAllocated = quantityToBeAllocated / pickableUnitOfMeasureQuantity;

            logger.debug("Let's see if we can allocate {} unit of measure from this location: " ,
                    unitOfMeasureQuantityToBeAllocated);

            if (quantityToBeAllocated % pickableUnitOfMeasureQuantity == 0) {

                logger.debug("we can allocate {} unit of measure from this location, as it can be divided by the UOM quantity: {}" ,
                        unitOfMeasureQuantityToBeAllocated, pickableUnitOfMeasureQuantity);
                return quantityToBeAllocated;
            }
            else if (!allocationRoundUpStrategy.isRoundUpAllowed()){
                // round up is not allowed and required quantity can't be divided evenly
                // by uom quantity, then allocate by unit of measure quantity, which may be
                // a bit less than needed
                logger.debug("quantityToBeAllocated % pickableUnitOfMeasureQuantity is not 0, and round up is not allowed, will return {}" ,
                        unitOfMeasureQuantityToBeAllocated * pickableUnitOfMeasureQuantity);
                return unitOfMeasureQuantityToBeAllocated * pickableUnitOfMeasureQuantity;
            }
            else{
                long maxQuantityToBeAllocated = 0;
                switch (allocationRoundUpStrategy.getType()) {
                    case BY_QUANTITY:
                        maxQuantityToBeAllocated  = quantityToBeAllocated + (long)allocationRoundUpStrategy.getValue().doubleValue();
                        break;
                    case BY_PERCENTAGE:
                        maxQuantityToBeAllocated  = (long)(quantityToBeAllocated * (100 + allocationRoundUpStrategy.getValue()) / 100);
                        break;
                    case NO_LIMIT:
                        maxQuantityToBeAllocated = Long.MAX_VALUE;
                        break;
                    default:
                        maxQuantityToBeAllocated = quantityToBeAllocated;
                        break;
                }
                // convert the max quantity to max unit of measure quantity
                long maxUnitOfMeasureQuantityToBeAllocated = maxQuantityToBeAllocated / pickableUnitOfMeasureQuantity;
                logger.debug("quantityToBeAllocated % pickableUnitOfMeasureQuantity is not 0, and round up is allowed, \n" +
                        "max quantity allowed is {}, max quantity of UOM is {}, uom quantity required is {}" ,
                        maxQuantityToBeAllocated, maxUnitOfMeasureQuantityToBeAllocated, unitOfMeasureQuantityToBeAllocated);
                // if the max unit of measure quantity is bigger than the required quantity, then round up
                if (maxUnitOfMeasureQuantityToBeAllocated > unitOfMeasureQuantityToBeAllocated) {
                    logger.debug("Will round up by one UOM, final quantity is {}",
                            (unitOfMeasureQuantityToBeAllocated + 1) * pickableUnitOfMeasureQuantity);
                    return (unitOfMeasureQuantityToBeAllocated + 1) * pickableUnitOfMeasureQuantity;
                }
                else {
                    // OK, even we allow round up, the result is still the same as no round up, due to the
                    // restriction of the unit of measure quantity
                    logger.debug("Will NOT round up by one UOM, final quantity is {}",
                            unitOfMeasureQuantityToBeAllocated * pickableUnitOfMeasureQuantity);
                    return unitOfMeasureQuantityToBeAllocated * pickableUnitOfMeasureQuantity;
                }

            }
        }
    }

    private List<Pick> tryAllocateByLPN(AllocationRequest allocationRequest, long openQuantity,
                                  List<InventorySummary> inventorySummaries,
                                  List<Pick> existingPicks,
                                  AllocationRoundUpStrategy allocationRoundUpStrategy) {
        logger.debug("Start to allocate item: {}, by LPN, total quantity needed: {}, round up strategy: {} / {}",
                allocationRequest.getItem().getName(), openQuantity,
                allocationRoundUpStrategy.getType(),
                allocationRoundUpStrategy.getValue());
        List<Pick> picks = new ArrayList<>();
        long totalQuantityToBeAllocated = openQuantity;
        // Let's try to allocate by unit of measure

        for(InventorySummary inventorySummary : inventorySummaries) {
            if (totalQuantityToBeAllocated <=0) {
                // we got enough quantity
                // let's return;
                break;
            }
            // skip the inventory if it is already 'allocated by LPN'
            // inventory map in the inventory summary:
            // key: lpn
            // value: list of the inventory on the lpn
            Map<String, List<Inventory>> lpnInventories = inventorySummary.getInventories();
            // Get the quantity that already allocated to certain pick
            // we will need to make sure even after we allocate from certain LPN,
            // we will still have enough quantity for the existing picks
            // to verify the quantity, we will skip the quantity that 'allocated by LPN' type
            // of picks
            List<Pick> existingPicksByInventorySummary = getExistingPicksByInventorySummary(inventorySummary, existingPicks);


            // setup the LPN and quantity that can be allocated;
            // AN lpn either can be allocated as full, or
            // can't be allocated at all
            // we will sort the inventory based on the following rules
            // 1. 1st: if the LPN quantity matches with the required quantity
            // 2. 2nd: the LPN quantity is greater than the required quantity
            // 2.1 sort by quantity asc
            // 3. 3rd: The lpn quantity is less than the required quantity
            // 2.1 sort by quantity desc
            Map<String, Long> lpnQuantityToBeAllocatedMap = new HashMap<>();

            // Go through each LPN to see if we can allocate from the whole LPN
            Iterator<Map.Entry<String, List<Inventory>>> lpnInventoryIterator = lpnInventories.entrySet().iterator();
            long alreadyAllocatedQuantity = 0;


            while(lpnInventoryIterator.hasNext()) {
                Map.Entry<String, List<Inventory>> lpnInventoryMapEntry =
                        lpnInventoryIterator.next();
                // The LPN is allocatable when there's none in the
                // inventory with this LPN is allocated by certain pick
                // and we don't break the existing picks which is picked
                // by SUOM.
                logger.debug("Check if lpn {} is allocatable", lpnInventoryMapEntry.getKey());

                boolean lpnAlreadyAllocated =
                        lpnInventoryMapEntry.getValue().stream()
                                .anyMatch(inventory -> Objects.nonNull(inventory.getAllocatedByPickId()));
                long totalLPNquantity =
                        lpnInventoryMapEntry.getValue().stream()
                                .mapToLong(Inventory::getQuantity).sum();

                if (lpnAlreadyAllocated) {
                    alreadyAllocatedQuantity  += totalLPNquantity;

                }


                if (!lpnAlreadyAllocated &&
                        validateLPNAllocatable(totalQuantityToBeAllocated,
                            lpnInventoryMapEntry.getValue(),
                            inventorySummary, existingPicksByInventorySummary, allocationRoundUpStrategy)) {


                    logger.debug("lpn {} is allocatable, quantity: {}",
                            lpnInventoryMapEntry.getKey(),
                            totalLPNquantity);
                    lpnQuantityToBeAllocatedMap.put(
                            lpnInventoryMapEntry.getKey(),
                            totalLPNquantity
                    );
                }
                else {

                    logger.debug("lpn {}  is NOT allocatable",
                            lpnInventoryMapEntry.getKey());
                }

            }


            lpnQuantityToBeAllocatedMap =
                    sortLPNByQuantityWithRequiredQuantity(lpnQuantityToBeAllocatedMap, totalQuantityToBeAllocated);

            logger.debug("LPNs we can allocate from location {}:\n {}, \n, required quantity: {}",
                    inventorySummary.getLocation().getName(),
                    lpnQuantityToBeAllocatedMap,
                    totalQuantityToBeAllocated);


            Iterator<Map.Entry<String, Long>> lpnToBeAllocatedMapIterator = lpnQuantityToBeAllocatedMap.entrySet().iterator();
            while(lpnToBeAllocatedMapIterator.hasNext() && totalQuantityToBeAllocated > 0) {
                // re-evaluate whether the LPN is still valid for this LPN pick
                // as the total quantity to be allocated is changed for each round
                Map.Entry<String, Long> lpnToBeAllocated = lpnToBeAllocatedMapIterator.next();

                if (!validateLPNAllocatable(totalQuantityToBeAllocated,
                                            lpnToBeAllocated.getValue(),
                                            inventorySummary, existingPicksByInventorySummary,
                                            allocationRoundUpStrategy)) {

                    logger.debug("lpn {}  is NOT allocatable in the re-evaluation " +
                                    " after the totalQuantityToBeAllocated is changed to {} ",
                            lpnToBeAllocated.getKey(), totalQuantityToBeAllocated);
                    // Current LPN is a valid LPN at the first place
                    // but it is not valid any more after we allocated several
                    // LPNs
                    allocationTransactionHistoryService.createAndSendEmptyAllocationTransactionHistory(
                            allocationRequest,
                            inventorySummary.getLocation(),
                            totalQuantityToBeAllocated,
                            true,
                            allocationRoundUpStrategy.isRoundUpAllowed() ? true: false,
                            "lpn " + lpnToBeAllocated.getKey() + " is NOT allocatable in the re-evaluation " +
                                    " after the totalQuantityToBeAllocated is changed to " + totalQuantityToBeAllocated
                    );

                    continue;

                }
                logger.debug("we can allocate from LPN: {}, quantity: {}",
                        lpnToBeAllocated.getKey(), lpnToBeAllocated.getValue());
                String lpn = lpnToBeAllocated.getKey();
                Long lpnQuantityToBeAllocated = lpnToBeAllocated.getValue();
                // create the pick
                Pick pick = tryCreatePickForLPNAllocation(allocationRequest,
                        lpn, lpnQuantityToBeAllocated,
                        inventorySummary);
                picks.add(pick);
                logger.debug("pick by LPN: pick {}, LPN {}",
                        pick.getNumber(), lpn);
                // setup the pick on the inventory of this LPN
                // and refresh the invenotry summary so it can be used later
                markLPNAllocated(inventorySummary, lpn, pick);
                allocationTransactionHistoryService.createAndSendAllocationTransactionHistory(
                        allocationRequest,
                        inventorySummary.getLocation(),
                        totalQuantityToBeAllocated,  // required quantity at this allocation transaction
                        lpnQuantityToBeAllocated,  // total required quantity
                        lpnQuantityToBeAllocatedMap.values().stream().mapToLong(Long::longValue).sum(),  // all LPN quantity left in this location
                        lpnQuantityToBeAllocated,  // allocated quantity by this round
                        alreadyAllocatedQuantity,
                        false,
                        true,
                        allocationRoundUpStrategy.isRoundUpAllowed() ? true: false,
                        ""
                );
                // allocate quantity from this LPN
                totalQuantityToBeAllocated = totalQuantityToBeAllocated - lpnQuantityToBeAllocated;

                logger.debug("After allocate from LPN: {}, we still have quantity: {}",
                        lpnToBeAllocated.getKey(), totalQuantityToBeAllocated);




            }
        }
        return picks;
    }


    private Map<String, Long> sortLPNByQuantityWithRequiredQuantity(Map<String, Long> lpnQuantityToBeAllocatedMap, long requiredQuantity) {
        logger.debug("start to sort the map: {}, required quantity: {}",
                lpnQuantityToBeAllocatedMap,
                requiredQuantity);
        List<Map.Entry<String, Long>> list = new ArrayList<>(lpnQuantityToBeAllocatedMap.entrySet());
        list.sort((entry1, entry2) -> {
            long diff1 = entry1.getValue() - requiredQuantity;
            long diff2 = entry2.getValue() - requiredQuantity;
            // if one of the quantity matches with the required quantity
            // return it first
            if (diff1 == 0) {
                return -1;
            }
            else if (diff2 == 0) {
                return 1;
            }
            else if(diff1 * diff2 > 0){
                // both quantities are in the side of the required quantity
                // we will return the one that is close to the required quantity
                return Long.compare(Math.abs(diff1), Math.abs(diff2));
            }
            else {
                // one quantity is bigger than the required quantity and the other
                // is smaller than the reuqired quantity, let's return the bigger one
                return Long.compare(entry2.getValue(), entry1.getValue());
            }

        });
        Map<String, Long> result = new LinkedHashMap<>();
        for (Map.Entry<String, Long> entry : list) {
            result.put(entry.getKey(), entry.getValue());
        }
        logger.debug("after sorted,  the map: {}, required quantity: {}",
                result,
                requiredQuantity);
        return result;
    }

    private void markLPNAllocated(InventorySummary inventorySummary, String lpn, Pick pick) {

        inventorySummary.markLPNAsAllocated(lpn, pick.getId());

        logger.debug("Will mark inventory with lpn: {} as picked by {}",
                lpn, pick.getNumber());
        inventoryServiceRestemplateClient.markLPNAllocated(
                pick.getWarehouseId(),
                lpn, pick.getId()
        );
    }

    private void displayInventorySummaryInformation(List<InventorySummary> inventorySummaries) {
        StringBuilder stringBuilder = new StringBuilder();
        inventorySummaries.forEach(inventorySummary -> {

            stringBuilder.append("location group:").append(inventorySummary.getLocation().getLocationGroup().getName()).append("\n")
                    .append("Location: ").append(inventorySummary.getLocation().getName()).append("\n")
                    .append("Item: ").append(inventorySummary.getItem().getName())
                    .append("Color: ").append(inventorySummary.getColor())
                    .append("Product Size: ").append(inventorySummary.getProductSize())
                    .append("Style: ").append(inventorySummary.getStyle())
                    .append(", Total Quantity: ").append(inventorySummary.getQuantity())
                    .append(", inventory: \n");
            Iterator<Map.Entry<String, List<Inventory>>> lpnIterator = inventorySummary.getInventories().entrySet().iterator();
            int index = 1;
            while(lpnIterator.hasNext()) {
                Map.Entry<String, List<Inventory>> lpnInventory = lpnIterator.next();
                stringBuilder.append(">> LPN: ").append(lpnInventory.getKey()).append("\n");
                lpnInventory.getValue().forEach(inventory -> {
                    stringBuilder.append(">>>> ").append(index).append(". id:").append(inventory.getId())
                            .append(", ").append("qty:").append(inventory.getQuantity()).append("\n");
                });
            }
        });
        logger.debug("==============  Inventory Summaries  ============");
        logger.debug(stringBuilder.toString());
    }

    private Pick tryCreatePickForLPNAllocation(AllocationRequest allocationRequest,
                                               String lpn, Long lpnQuantityToBeAllocated,
                                               InventorySummary inventorySummary) {

        if (allocationRequest.getShipmentLines().size() > 0) {
            logger.debug("Start to create picks for {} shipment ",
                    allocationRequest.getShipmentLines().size());
            return pickService.generatePick(inventorySummary, allocationRequest.getShipmentLines().get(0),
                    lpnQuantityToBeAllocated, lpn);
        }
        else if(allocationRequest.getWorkOrder() != null &&
                allocationRequest.getWorkOrderLines().size() > 0) {

            return pickService.generatePick(allocationRequest.getWorkOrder() ,
                    inventorySummary, allocationRequest.getWorkOrderLines().get(0),
                    lpnQuantityToBeAllocated, lpn,
                    allocationRequest.getDestinationLocationId());
        }
        else {
            throw AllocationException.raiseException("now we only support new allocation for single shipment line");
        }
    }

    /**
     * Check if we can allocate from this LPN, by 'allocate by LPN'
     * @param lpnInventories inventories on this LPN
     * @param inventorySummary inventories summary in the location
     * @param existingPicksByInventorySummary existing picks in the location
     * @return
     */
    private boolean validateLPNAllocatable(long totalQuantityToBeAllocated,
                                           List<Inventory> lpnInventories,
                                           InventorySummary inventorySummary,
                                           List<Pick> existingPicksByInventorySummary,
                                           AllocationRoundUpStrategy allocationRoundUpStrategy) {

        boolean lpnAlreadyAllocated =
                lpnInventories.stream()
                        .anyMatch(inventory -> Objects.nonNull(inventory.getAllocatedByPickId()));
        // if the LPN is allocated by certain pick, then it is not valid for new pick
        if (lpnAlreadyAllocated) {
            logger.debug("This inventory is already allocated!");
            return false;
        }
        Long quantityByLPN = lpnInventories.stream().mapToLong(Inventory::getQuantity).sum();

        return validateLPNAllocatable(totalQuantityToBeAllocated,
                quantityByLPN, inventorySummary, existingPicksByInventorySummary, allocationRoundUpStrategy);
    }

    private boolean validateLPNAllocatable(long totalQuantityToBeAllocated,
                                           long quantityByLPN,
                                           InventorySummary inventorySummary,
                                           List<Pick> existingPicksByInventorySummary,
                                           AllocationRoundUpStrategy allocationRoundUpStrategy) {
        long maxQuantityToBeAllocated = totalQuantityToBeAllocated;
        // make sure
        // 1. if round up is not allowed, then the LPN's quantity doesn't exceed the quantity needed
        // 2. if round up is allowed, then the LPN's quantity doesn't exceed the max round up quantity
        switch (allocationRoundUpStrategy.getType()) {
            case BY_PERCENTAGE:
                maxQuantityToBeAllocated = (long)(totalQuantityToBeAllocated * (100 + allocationRoundUpStrategy.getValue()) / 100);
                break;
            case NO_LIMIT:
                // when there's no limit, we can allocate the whole LPN quantity
                maxQuantityToBeAllocated = quantityByLPN;
                break;
            case BY_QUANTITY:
                maxQuantityToBeAllocated = totalQuantityToBeAllocated + (long)allocationRoundUpStrategy.getValue().doubleValue();
                break;
            default:
                // by default, we won't allow over allocation
                maxQuantityToBeAllocated = totalQuantityToBeAllocated;
                break;
        }
        if (maxQuantityToBeAllocated < quantityByLPN) {
            // the max quantity we are allowed to allocate is less than the whole LPN's quantity,
            // which means we can't allocate from this LPN
            logger.debug("can't allocate from this LPN due to maxQuantityToBeAllocated: {}, quantityByLPN: {}",
                    maxQuantityToBeAllocated, quantityByLPN);
            return false;
        }

        return validateQuantityAllocatable(quantityByLPN, inventorySummary, existingPicksByInventorySummary);
    }

    /**
     * check if we can allocate the quantity from the inventory summary.
     *
     * we will ignore that LPN that is already allocated to certain picks and those
     * picks as well. Then we only have those picks that is allocated by quantity
     * and the inventory that is not allocated by LPN yet., both of which are quantity
     * based.
     * @param quantityToBeAllocated
     * @param inventorySummary
     * @param existingPicksByInventorySummary
     * @return
     */
    private boolean validateQuantityAllocatable(Long quantityToBeAllocated,
                                                InventorySummary inventorySummary,
                                                List<Pick> existingPicksByInventorySummary) {

        // the LPN is not allocated by certain pick, let's see if we can allocate the quantity
        // from this LPN, while not breaking other open picks
        // we will only validate against the picks that allocate by quantity
        Long pickByQuantityPicksTotalOpenQuantity
                = pickByQuantityPicksTotalOpenQuantity(existingPicksByInventorySummary);
        Long availableInventoryQuantity
                = getAvailableInventoryQuantity(inventorySummary);

        logger.debug("quantityToBeAllocated: {}, pickByQuantityPicksTotalOpenQuantity: {}, availableInventoryQuantity: {}",
                quantityToBeAllocated,
                pickByQuantityPicksTotalOpenQuantity,
                availableInventoryQuantity);
        if (quantityToBeAllocated + pickByQuantityPicksTotalOpenQuantity <= availableInventoryQuantity) {
            return true;
        }
        else {
            return false;
        }
    }

    /**
     * Get the total open quantity of picks from a inventory, excluding those picked by LPN
     * @param existingPicks
     * @return
     */
    private long pickByQuantityPicksTotalOpenQuantity(List<Pick> existingPicks) {
        return existingPicks.stream()
                .filter(pick -> Objects.isNull(pick.getLpn()))
                .map(pick -> pick.getQuantity()-pick.getPickedQuantity())
                .mapToLong(Long::longValue).sum();
    }

    /**
     * get available inventory quantity, exclued those lPN that already allocated.
     * Note: the quantity will include the quantity that already allocated by picks
     * that is not Allocate By LPN
     * @param inventorySummary
     * @return
     */
    private long getAvailableInventoryQuantity(InventorySummary inventorySummary) {
        return inventorySummary.getInventories().entrySet().stream()
                .filter(entry -> {
                    List<Inventory> inventories = entry.getValue();
                    if(inventories.stream().anyMatch(inventory -> Objects.nonNull(inventory.getAllocatedByPickId()))) {
                        // the LPN is allocated, let's ignore this as it is not valid
                        return false;
                    }
                    return true;
                }).mapToLong(entry -> {
                    // return the total quantity of this LPN
                    List<Inventory> inventories = entry.getValue();
                    return inventories.stream().map(Inventory::getQuantity).mapToLong(Long::longValue).sum();
                }).sum();
    }

    /**
     * Get the picks from a specific location out of all existing picks against the item
     * @param inventorySummary inventory summary of certain item in certain location
     * @param existingPicks existing picks against certain item
     * @return
     */
    private List<Pick> getExistingPicksByInventorySummary (InventorySummary inventorySummary, List<Pick> existingPicks) {

        return existingPicks.stream()
                .filter(pick -> inventorySummaryPickableByPickWork(inventorySummary, pick))
                .collect(Collectors.toList());
    }

    private boolean inventorySummaryPickableByPickWork(InventorySummary inventorySummary, Pick pick) {
        if (!pick.getSourceLocationId().equals(inventorySummary.getLocationId())) {
            return false;
        }
        // if pick is not for a specific attribute, then the pick is able to pick inventory with
        // any attribute value
        if (Strings.isNotBlank(pick.getColor()) && !pick.getColor().equalsIgnoreCase(inventorySummary.getColor())) {
            return false;
        }
        if (Strings.isNotBlank(pick.getProductSize()) && !pick.getProductSize().equalsIgnoreCase(inventorySummary.getProductSize())) {
            return false;
        }
        if (Strings.isNotBlank(pick.getStyle()) && !pick.getStyle().equalsIgnoreCase(inventorySummary.getStyle())) {
            return false;
        }
        return true;
    }
    /**
     * Return the smallest unit of measure of the inventory
     * By default we assume all the unit of measure is pickable in the location and will return
     * the one with smallest quantity. This is supposed to be override by child implementation
     * @param inventorySummary
     * @return
     */
    protected ItemUnitOfMeasure getSmallestPickableUnitOfMeasure(InventorySummary inventorySummary) {

        // If item is marked as pick by LPN, then we don't allow pick by UOM
        if(inventorySummary.getItem().getAllowAllocationByLPN()) {
            logger.debug("item {} is picked by LPN, not allowed to be pick by UOM",
                    inventorySummary.getItem().getName());
            return null;
        }
        // By default, we return the smallest unit of measure of the inventory in the summary
        return inventorySummary.getInventories().entrySet().stream().map(entry -> entry.getValue())
                .flatMap(List::stream).map(inventory -> inventory.getItemPackageType().getItemUnitOfMeasures())
                .flatMap(List::stream)
                .min((unitOfMeasure1, unitOfMeasure2) -> (int) (unitOfMeasure1.getQuantity() - unitOfMeasure2.getQuantity()))
                .orElse(null);
    }


    protected AllocationRoundUpStrategy getAllocationRoundUpStrategy(Item item) {
        logger.debug("Item {}'s round up strategy: strategy: {}, value: {}",
                item.getName(), item.getAllocationRoundUpStrategyType(),
                item.getAllocationRoundUpStrategyValue());
        return new AllocationRoundUpStrategy(item.getAllocationRoundUpStrategyType(),
                item.getAllocationRoundUpStrategyValue());
    }
    protected boolean isAllocateByLPNAllowed(Item item) {

        logger.debug("allocate by LPN is allowed? {}, for item {}",
                item.getAllowAllocationByLPN(), item.getName());
        return Objects.isNull(item.getAllowAllocationByLPN()) ? false : item.getAllowAllocationByLPN();
    }
    /**
     * Sort the inventory summaries based upon certain rules. The child class with
     * different stategy is supposed to override this method so it can implement
     * different rules to sort the inventory summary and get a different allocation result
     * @param inventorySummaries inventory summary list to be sorted
     * @return inventory summary list after being sorted
     */
    protected List<InventorySummary> sort(List<InventorySummary> inventorySummaries) {

        // by default, we will return the inventory with biggest quantity first
        Collections.sort(inventorySummaries, (o1, o2) -> o2.getQuantity().compareTo(o1.getQuantity()));
        return inventorySummaries;

    }

    private ShortAllocation generateShortAllocation(Item item,
                                                    ShipmentLine shipmentLine,
                                                    AllocationRequest allocationRequest,
                                                    Long quantity) {

        return shortAllocationService.generateShortAllocation(item, shipmentLine, allocationRequest, quantity);

    }

    private ShortAllocation generateShortAllocation(WorkOrder workOrder, Item item,
                                                    WorkOrderLine workOrderLine,
                                                    AllocationRequest allocationRequest,
                                                    Long quantity) {

        return shortAllocationService.generateShortAllocation(workOrder, item, workOrderLine,
                allocationRequest, quantity);

    }
    /**
     * @param allocationRequest
     * @param item
     * @param quantity
     * @return
     */
    private ShortAllocation generateShortAllocation(AllocationRequest allocationRequest, Item item, Long quantity) {

        logger.debug("Start to generate short allocation for item {}, quantity {}",
                item.getName(), quantity);
        // For now we will only support allocate one line by one line
        // either shipment line or work order line
        if (allocationRequest.getShipmentLines().size() > 0) {
            return generateShortAllocation(item, allocationRequest.getShipmentLines().get(0), allocationRequest, quantity);
        }

        else if (Objects.nonNull(allocationRequest.getWorkOrder()) &&
                allocationRequest.getWorkOrderLines().size() > 0) {
            return generateShortAllocation(allocationRequest.getWorkOrder(),
                    item, allocationRequest.getWorkOrderLines().get(0),
                    allocationRequest, quantity);

        }
        else {
            throw ShortAllocationException.raiseException("Can't generate short allocation for allocation request: ");
        }

    }


}
