package com.garyzhangscm.cwms.workorder.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.apache.commons.lang3.tuple.Triple;
import org.apache.logging.log4j.util.Strings;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.persistence.*;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Entity
@Table(name = "work_order_line")
public class WorkOrderLine extends AuditibleEntity<String>{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "work_order_line_id")
    @JsonProperty(value="id")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "work_order_id")
    @JsonIgnore
    private WorkOrder workOrder;

    @Transient
    private String workOrderNumber;


    @OneToOne
    @JoinColumn(name = "material_work_order_id")
    private WorkOrder materialWorkOrder;



    @Column(name = "number")
    private String number;


    @Column(name = "item_id")
    private Long itemId;

    @Transient
    private Item item;

    @Column(name = "expected_quantity")
    private Long expectedQuantity;

    @Column(name = "open_quantity")
    private Long openQuantity;

    @Column(name = "inprocess_quantity")
    private Long inprocessQuantity;

    @Column(name = "delivered_quantity")
    private Long deliveredQuantity = 0L;

    @Column(name = "consumed_quantity")
    private Long consumedQuantity = 0L;


    @Column(name = "scrapped_quantity")
    private Long scrappedQuantity = 0L;


    @Column(name = "returned_quantity")
    private Long returnedQuantity = 0L;

    @Column(name = "spare_part_quantity")
    private Long sparePartQuantity = 0L;


    // Specific the inventory status that
    // user ordered. For example, when return
    // to vendor, we may return DAMAGED inventory
    @Column(name = "inventory_status_id")
    private Long inventoryStatusId;


    @Enumerated(EnumType.STRING)
    @Column(name="allocation_strategy_type")
    private AllocationStrategyType allocationStrategyType;

    @Transient
    private InventoryStatus inventoryStatus;

    @Transient
    List<Pick> picks = new ArrayList<>();

    @Transient
    List<ShortAllocation> shortAllocations = new ArrayList<>();

    @Transient
    // for display inventory in the manual pick report
    private List<String> manualPickableInventoryForDisplay = new ArrayList<>();


    @OneToMany(
            mappedBy = "workOrderLine",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    private List<WorkOrderLineSparePart> workOrderLineSpareParts = new ArrayList<>();

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WorkOrderLine that = (WorkOrderLine) o;
        if (Objects.equals(id, that.id)) {
            return true;
        }
        return Objects.equals(workOrder, that.workOrder) &&
                Objects.equals(number, that.number);


    }


    public void setupManualPickableInventoryForDisplay(List<Inventory> inventories) {

        Collections.sort(inventories, Comparator.comparing(Inventory::getInWarehouseDatetime));

        // key: location name
        // value: triple
        //        1. location name
        //        2. earliest in warehouse date in the location
        //        3. latest in warehouse date in the location
        Map<String, Triple<String, ZonedDateTime, ZonedDateTime>> locationsWithInWarehouseDateMap = new HashMap<>();

        // group inventory with locatin name
        // key: location name;
        // value: inventory in the location
        Map<String, List<Inventory>> inventoryInLocation = new HashMap<>();
        for(Inventory inventory : inventories) {
            String locationName = inventory.getLocation().getName();
            if (locationsWithInWarehouseDateMap.containsKey(locationName)) {
                Triple<String, ZonedDateTime, ZonedDateTime> locationInWarehouseDate = locationsWithInWarehouseDateMap.get(locationName);
                if (inventory.getInWarehouseDatetime().isBefore(locationInWarehouseDate.getMiddle())) {
                    locationsWithInWarehouseDateMap.put(locationName,
                            Triple.of(locationName, inventory.getInWarehouseDatetime(), locationInWarehouseDate.getRight()));
                }
                else if (inventory.getInWarehouseDatetime().isAfter(locationInWarehouseDate.getRight())) {
                    locationsWithInWarehouseDateMap.put(locationName,
                            Triple.of(locationName,  locationInWarehouseDate.getMiddle(), inventory.getInWarehouseDatetime()));

                }
            }
            else {
                locationsWithInWarehouseDateMap.put(locationName,
                        Triple.of(locationName, inventory.getInWarehouseDatetime(), inventory.getInWarehouseDatetime()));
            }
            inventoryInLocation.computeIfAbsent(locationName, key -> new ArrayList<>()).add(inventory);

        }

        // sort the location based on the earliest in warehouse date

        List<Triple<String, ZonedDateTime, ZonedDateTime>> locationsWithInWarehouseDateList
                = new ArrayList<>(locationsWithInWarehouseDateMap.values());


        Collections.sort(locationsWithInWarehouseDateList, Comparator.comparing(Triple::getMiddle));

        manualPickableInventoryForDisplay = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM/dd/yyyy");

        locationsWithInWarehouseDateList.forEach(
                locationWithInWarehouseDate -> {
                    // show the location summary first
                    manualPickableInventoryForDisplay.add(
                            locationWithInWarehouseDate.getLeft() + ": " +
                                    locationWithInWarehouseDate.getMiddle().format(formatter) + " ~ " +
                                    locationWithInWarehouseDate.getRight().format(formatter)
                    );

                    // show each inventory in the location
                    inventoryInLocation.get(locationWithInWarehouseDate.getLeft()).forEach(
                            inventory ->
                                    manualPickableInventoryForDisplay.add("    # " +
                                            inventory.getLpn() + ", " +
                                            inventory.getQuantity() + ", " +
                                            inventory.getInWarehouseDatetime().format(formatter))
                    );

                }
        );
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, workOrder, number);
    }

    public String getWorkOrderNumber() {
        return Strings.isNotBlank(workOrderNumber) ?
                workOrderNumber :
                    Objects.nonNull(workOrder) ? workOrder.getNumber() : "";
    }

    public void setWorkOrderNumber(String workOrderNumber) {
        this.workOrderNumber = workOrderNumber;
    }

    public WorkOrder getMaterialWorkOrder() {
        return materialWorkOrder;
    }

    public void setMaterialWorkOrder(WorkOrder materialWorkOrder) {
        this.materialWorkOrder = materialWorkOrder;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    public WorkOrder getWorkOrder() {
        return workOrder;
    }

    public void setWorkOrder(WorkOrder workOrder) {
        this.workOrder = workOrder;
    }


    public Long getItemId() {
        return itemId;
    }

    public void setItemId(Long itemId) {
        this.itemId = itemId;
    }

    public Item getItem() {
        return item;
    }

    public void setItem(Item item) {
        this.item = item;
    }

    public Long getExpectedQuantity() {
        return expectedQuantity;
    }

    public void setExpectedQuantity(Long expectedQuantity) {
        this.expectedQuantity = expectedQuantity;
    }

    public Long getOpenQuantity() {
        return openQuantity;
    }

    public void setOpenQuantity(Long openQuantity) {
        this.openQuantity = openQuantity;
    }

    public Long getInprocessQuantity() {
        return inprocessQuantity;
    }

    public void setInprocessQuantity(Long inprocessQuantity) {
        this.inprocessQuantity = inprocessQuantity;
    }

    public Long getConsumedQuantity() {
        return consumedQuantity;
    }

    public void setConsumedQuantity(Long consumedQuantity) {
        this.consumedQuantity = consumedQuantity;
    }

    public Long getInventoryStatusId() {
        return inventoryStatusId;
    }

    public void setInventoryStatusId(Long inventoryStatusId) {
        this.inventoryStatusId = inventoryStatusId;
    }

    public InventoryStatus getInventoryStatus() {
        return inventoryStatus;
    }

    public void setInventoryStatus(InventoryStatus inventoryStatus) {
        this.inventoryStatus = inventoryStatus;
    }

    public List<Pick> getPicks() {
        return picks;
    }

    public void setPicks(List<Pick> picks) {
        this.picks = picks;
    }

    public List<ShortAllocation> getShortAllocations() {
        return shortAllocations;
    }

    public void setShortAllocations(List<ShortAllocation> shortAllocations) {
        this.shortAllocations = shortAllocations;
    }

    public Long getDeliveredQuantity() {
        return deliveredQuantity;
    }

    public void setDeliveredQuantity(Long deliveredQuantity) {
        this.deliveredQuantity = deliveredQuantity;
    }

    public Long getScrappedQuantity() {
        return scrappedQuantity;
    }

    public void setScrappedQuantity(Long scrappedQuantity) {
        this.scrappedQuantity = scrappedQuantity;
    }

    public Long getReturnedQuantity() {
        return returnedQuantity;
    }

    public void setReturnedQuantity(Long returnedQuantity) {
        this.returnedQuantity = returnedQuantity;
    }

    public AllocationStrategyType getAllocationStrategyType() {
        return allocationStrategyType;
    }

    public void setAllocationStrategyType(AllocationStrategyType allocationStrategyType) {
        this.allocationStrategyType = allocationStrategyType;
    }

    public List<WorkOrderLineSparePart> getWorkOrderLineSpareParts() {
        return workOrderLineSpareParts;
    }

    public void setWorkOrderLineSpareParts(List<WorkOrderLineSparePart> workOrderLineSpareParts) {
        this.workOrderLineSpareParts = workOrderLineSpareParts;
    }

    public Long getSparePartQuantity() {
        return sparePartQuantity;
    }

    public void setSparePartQuantity(Long sparePartQuantity) {
        this.sparePartQuantity = sparePartQuantity;
    }

    public List<String> getManualPickableInventoryForDisplay() {
        return manualPickableInventoryForDisplay;
    }

    public void setManualPickableInventoryForDisplay(List<String> manualPickableInventoryForDisplay) {
        this.manualPickableInventoryForDisplay = manualPickableInventoryForDisplay;
    }
}
