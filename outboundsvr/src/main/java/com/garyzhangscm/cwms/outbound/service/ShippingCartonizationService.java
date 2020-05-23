package com.garyzhangscm.cwms.outbound.service;

import com.garyzhangscm.cwms.outbound.clients.CommonServiceRestemplateClient;
import com.garyzhangscm.cwms.outbound.clients.InventoryServiceRestemplateClient;
import com.garyzhangscm.cwms.outbound.clients.WarehouseLayoutServiceRestemplateClient;
import com.garyzhangscm.cwms.outbound.exception.PackingException;
import com.garyzhangscm.cwms.outbound.exception.ResourceNotFoundException;
import com.garyzhangscm.cwms.outbound.model.*;
import com.garyzhangscm.cwms.outbound.repository.ShippingCartonizationRepository;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


import javax.persistence.criteria.*;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ShippingCartonizationService {


    private static final Logger logger = LoggerFactory.getLogger(ShippingCartonizationService.class);

    private boolean shippingAfterPacking = true;
    @Autowired
    ShippingCartonizationRepository shippingCartonizationRepository;
    @Autowired
    ShipmentLineService shipmentLineService;

    @Autowired
    CartonService cartonService;
    @Autowired
    ShipmentService shipmentService;

    @Autowired
    private WarehouseLayoutServiceRestemplateClient warehouseLayoutServiceRestemplateClient;
    @Autowired
    private InventoryServiceRestemplateClient inventoryServiceRestemplateClient;
    @Autowired
    private CommonServiceRestemplateClient commonServiceRestemplateClient;


    public ShippingCartonization findById(Long id) {
        return shippingCartonizationRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.raiseException("shipping carton not found by id: " + id));
    }

    public ShippingCartonization findByNumber(Long warehouseId, String number) {
        return shippingCartonizationRepository.findByWarehouseIdAndNumber(warehouseId, number);
    }



    public ShippingCartonization save(ShippingCartonization shippingCartonization) {
        return shippingCartonizationRepository.save(shippingCartonization);
    }


    public ShippingCartonization saveOrUpdate(ShippingCartonization shippingCartonization) {
        if (Objects.isNull(shippingCartonization.getId()) &&
                Objects.nonNull(findByNumber(shippingCartonization.getWarehouseId(),shippingCartonization.getNumber()))) {
            shippingCartonization.setId(findByNumber(shippingCartonization.getWarehouseId(),shippingCartonization.getNumber()).getId());
        }
        return save(shippingCartonization);
    }

    public List<ShippingCartonization> findAll(Long warehouseId, String number, String status, String cartonName) {

        List<ShippingCartonization> cartonizations =  shippingCartonizationRepository.findAll(
                (Root<ShippingCartonization> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) -> {
                    List<Predicate> predicates = new ArrayList<Predicate>();

                    predicates.add(criteriaBuilder.equal(root.get("warehouseId"), warehouseId));

                    if (StringUtils.isNotBlank(number)) {
                        predicates.add(criteriaBuilder.equal(root.get("number"), number));

                    }

                    if (StringUtils.isNotBlank(cartonName)) {
                        Join<Cartonization, Carton> joinCarton = root.join("carton", JoinType.INNER);
                        predicates.add(criteriaBuilder.equal(joinCarton.get("name"), cartonName));

                    }
                    Predicate[] p = new Predicate[predicates.size()];
                    return criteriaBuilder.and(predicates.toArray(p));
                }
        );


        return cartonizations;
    }


    public ShippingCartonization pack(Long warehouseId, String inventoryId,
                                      String packingStationName,
                                      Long cartonId, String cartonName,
                                      List<PackingResult> packingResults) {
        // Let's get the location that stands for the packing station.
        // we will move the packed quantity into the packing station
        // If the packing station name is not passed in, it means the
        // warehouse doesn't want to track the actual station that the
        // user finish the packing. then we will get a default
        // station
        Location packingStation;
        if (StringUtils.isNotBlank(packingStationName)) {
            Optional<Location> location = Optional.of(warehouseLayoutServiceRestemplateClient.getLocationByName(warehouseId, packingStationName));
            packingStation = location.orElseThrow(() -> PackingException.raiseException("Can't find station by name: " + packingStationName));

        }
        else {
            Optional<Location> location = Optional.of(warehouseLayoutServiceRestemplateClient.getDefaultPackingStation(warehouseId));
            packingStation = location.orElseThrow(() -> PackingException.raiseException("Can't find default packing station"));
        }
        // Now we only support pack by LPN so we will assume
        // inventory Id is always the LPN
        // TO-DO: We plan to support pack by Oder number / shipping label / etc
        //
        // For packing, we will always move inventory from the source to a
        // new LPN standards for the shipping carton
        String shippingCartonNumber = getNextShippingCartonizationNumber();
        List<Inventory> inventories = inventoryServiceRestemplateClient.getInventoryByLpn(warehouseId, inventoryId);

        // Get all the shipment according to the inventory.
        // We need to make sure there's only one shipment.
        // We are not allow to pack inventory from different shipment into same package,
        // even they are from the same order
        List<Shipment> shipments = shipmentService.getShipmentsByPickedInventory(inventories);
        if (shipments.size() == 0) {
            throw PackingException.raiseException("The inventory is not picked inventory");
        }
        else if (shipments.size() > 1) {
            throw PackingException.raiseException("Can't pack inventory from different shipments into same box");
        }
        Shipment shipment = shipments.get(0);
        if (Objects.isNull(shipment)) {
            throw PackingException.raiseException("The inventory is not picked inventory");
        }

        // For each packed item, loop through and get the inventory to move
        List<Inventory> movedInventories = new ArrayList<>();

        packingResults.forEach(packingResult -> {
            String itemName = packingResult.getItemName();
            Long packedQuantity = packingResult.getPackedQuantity();
            List<Inventory> matchedInventories
                    = inventories.stream()
                    .filter(inventory -> inventory.getItem().getName().equals(itemName))
                    .collect(Collectors.toList());
            // Loop each inventory until we get all the packed quantities
            Iterator<Inventory> inventoryIterator = matchedInventories.iterator();
            while (packedQuantity > 0) {
                if (!inventoryIterator.hasNext()) {
                    throw PackingException.raiseException("Can't find enough quantity to be packed");
                }
                Inventory inventory = inventoryIterator.next();
                Inventory inventoryToBeMoved;
                if (inventory.getQuantity() <= packedQuantity) {
                    // OK, we can move the whole inventory into the shipping carton
                    inventoryToBeMoved = inventory;
                }
                else {
                    // OK current invenotory has more quantity than packed inventory, let's split
                    // and then move into the shipping carton
                    inventoryToBeMoved = inventoryServiceRestemplateClient.split(inventory, packedQuantity).get(1);
                }

                try {
                    inventoryServiceRestemplateClient.moveInventory(inventoryToBeMoved, packingStation, shippingCartonNumber);
                    movedInventories.add(inventoryToBeMoved);
                    packedQuantity -= inventoryToBeMoved.getQuantity();
                } catch (IOException e) {
                    throw PackingException.raiseException("Error while moving inventory into shipping carton: " + e.getMessage());
                }
            }
        });

        Carton carton = null;
        if (Objects.nonNull(cartonId) ) {
            carton = cartonService.findById(cartonId);
            logger.debug("get carton by id {}, \n {}", cartonId, carton);
        }
        else if (StringUtils.isNotBlank(cartonName)) {
            carton  = cartonService.findByName(warehouseId, cartonName);
            logger.debug("get carton by id {}, \n {}", cartonName, carton);
        }
        ShippingCartonization shippingCartonization =
             new ShippingCartonization(shippingCartonNumber, warehouseId, carton);
        logger.debug("get shippingCartonization:\n {}", shippingCartonization);

        // After we finish the packing, let's

        shippingCartonization = save(shippingCartonization);

        if (shippingAfterPacking) {
            logger.debug("After we package the carton: \n{} we will ship the inventory as well: \n{}",
                    shippingCartonization, movedInventories);
            shipping(movedInventories);
        }
        return shippingCartonization;

    }

    /**
     * Shipping the inventory after we pack the inventory
     * @param inventories
     */
    private void shipping(List<Inventory> inventories) {

        shippingPackage(inventories);

        refreshShipmentQuantity(inventories);

    }

    /**
     * Ship the inventory to outside of the current warehouse
     * @param inventories
     */
    private void shippingPackage(List<Inventory> inventories) {
        // Move the inventory to a dedicated location
        // one location per package carrier
        // Move the inprocess quantity to shipped quantity
        inventories.forEach(inventory -> {
            ShipmentLine shipmentLine = shipmentLineService.getShipmentLineByPickedInventory(inventory);
            if (Objects.nonNull(shipmentLine)) {
                Location location = warehouseLayoutServiceRestemplateClient.getShippedParcelLocation(
                        shipmentLine.getWarehouseId(),
                        shipmentLine.getOrderLine().getCarrier().getName(),
                        shipmentLine.getOrderLine().getCarrierServiceLevel().getName()
                );
                logger.debug("Will move inventory \n{} to location: {}",
                        inventory, location);

                try {
                    inventoryServiceRestemplateClient.moveInventory(inventory, location);
                } catch (IOException e) {
                    PackingException.raiseException("Can't ship the packed inventory " + inventory);
                }
            }

        });

    }

    /**
     * Update the shipment line's quantity
     * @param inventories
     */
    private void refreshShipmentQuantity(List<Inventory> inventories) {


        // Move the inprocess quantity to shipped quantity
        inventories.forEach(inventory -> {
            ShipmentLine shipmentLine = shipmentLineService.getShipmentLineByPickedInventory(inventory);
            if (Objects.nonNull(shipmentLine)) {
                shipmentLineService.shippingPackage(shipmentLine, inventory);
            }
        });
    }


    private String getNextShippingCartonizationNumber() {
        return commonServiceRestemplateClient.getNextNumber("shipping-cartonization-number");
    }


}
