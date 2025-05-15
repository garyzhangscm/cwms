package com.garyzhangscm.cwms.outbound.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;
import org.springframework.security.core.context.SecurityContextHolder;

import javax.persistence.*;
import java.io.Serializable;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Objects;

@Entity
@Table(name = "order_activity")
public class OrderActivity extends AuditibleEntity<String> implements Serializable {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_activity_id")
    @JsonProperty(value="id")
    private Long id;

    @Column(name = "warehouse_id")
    private Long warehouseId;

    @Column(name = "number")
    private String number;
    @Column(name = "transaction_group_id")
    private String transactionGroupId;

    @Column(name = "activity_datetime")
    @JsonDeserialize(using = CustomZonedDateTimeDeserializer.class)
    @JsonSerialize(using = CustomZonedDateTimeSerializer.class)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private ZonedDateTime activityDateTime;
    // @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    // @JsonSerialize(using = LocalDateTimeSerializer.class)
    // @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    // private LocalDateTime activityDateTime;

    @Column(name = "username")
    private String username;
    @Column(name = "rf_code")
    private String rfCode;

    @ManyToOne
    @JoinColumn(name="outbound_order_id")
    @NotFound(action = NotFoundAction.IGNORE)
    private Order order;

    @ManyToOne
    @JoinColumn(name="outbound_order_line_id")
    @NotFound(action = NotFoundAction.IGNORE)
    private OrderLine orderLine;

    @Column(name = "type")
    @Enumerated(EnumType.STRING)
    private OrderActivityType orderActivityType;


    @Column(name = "client_id")
    private Long clientId;

    @Transient
    private Client client;

    // transaction quantity
    @Column(name = "quantity")
    private Long quantity;

    // order related quantity
    @Column(name = "outbound_order_number")
    private String orderNumber;
    @Column(name = "outbound_order_line_number")
    private String orderLineNumber;
    @Column(name = "old_order_line_expected_quantity")
    private Long oldOrderLineExpectedQuantity;
    @Column(name = "new_order_line_expected_quantity")
    private Long newOrderLineExpectedQuantity;

    @Column(name = "old_order_line_open_quantity")
    private Long oldOrderLineOpenQuantity;
    @Column(name = "new_order_line_open_quantity")
    private Long newOrderLineOpenQuantity;

    @Column(name = "old_order_line_inprocess_quantity")
    private Long oldOrderLineInProcessQuantity;
    @Column(name = "new_order_line_inprocess_quantity")
    private Long newOrderLineInProcessQuantity;

    @Column(name = "old_order_line_shipped_quantity")
    private Long oldOrderLineShippedQuantity;
    @Column(name = "new_order_line_shipped_quantity")
    private Long newOrderLineShippedQuantity;

    // shipment line related quantity
    @Column(name = "shipment_number")
    private String shipmentNumber;
    @Column(name = "shipment_line_number")
    private String shipmentLineNumber;
    @Column(name = "old_shipment_line_quantity")
    private Long oldShipmentLineQuantity;
    @Column(name = "new_shipment_line_quantity")
    private Long newShipmentLineQuantity;

    @Column(name = "old_shipment_line_open_quantity")
    private Long oldShipmentLineOpenQuantity;
    @Column(name = "new_shipment_line_open_quantity")
    private Long newShipmentLineOpenQuantity;

    @Column(name = "old_shipment_line_inprocess_quantity")
    private Long oldShipmentLineInProcessQuantity;
    @Column(name = "new_shipment_line_inprocess_quantity")
    private Long newShipmentLineInProcessQuantity;

    @Column(name = "old_shipment_line_loaded_quantity")
    private Long oldShipmentLineLoadedQuantity;
    @Column(name = "new_shipment_line_loaded_quantity")
    private Long newShipmentLineLoadedQuantity;

    @Column(name = "old_shipment_line_shipped_quantity")
    private Long oldShipmentLineShippedQuantity;
    @Column(name = "new_shipment_line_shipped_quantity")
    private Long newShipmentLineShippedQuantity;


    // pick related quantity
    @Column(name = "pick_number")
    private String pickNumber;
    @Column(name = "old_pick_quantity")
    private Long oldPickQuantity;
    @Column(name = "new_pick_quantity")
    private Long newPickQuantity;
    @Column(name = "old_pick_picked_quantity")
    private Long oldPickPickedQuantity;
    @Column(name = "new_pick_picked_quantity")
    private Long newPickPickedQuantity;

    // bulk pick
    @Column(name = "bulk_pick_number")
    private String bulkPickNumber;

    // short allocation related quantity
    @Column(name = "old_short_allocation_quantity")
    private Long oldShortAllocationQuantity;
    @Column(name = "new_short_allocation_quantity")
    private Long newShortAllocationQuantity;

    @Column(name = "old_short_allocation_open_quantity")
    private Long oldShortAllocationOpenQuantity;
    @Column(name = "new_short_allocation_open_quantity")
    private Long newShortAllocationOpenQuantity;

    @Column(name = "old_short_allocation_inprocess_quantity")
    private Long oldShortAllocationInProcessQuantity;
    @Column(name = "new_short_allocation_inprocess_quantity")
    private Long newShortAllocationInProcessQuantity;

    @Column(name = "old_short_allocation_delivered_quantity")
    private Long oldShortAllocationDeliveredQuantity;
    @Column(name = "new_short_allocation_delivered_quantity")
    private Long newShortAllocationDeliveredQuantity;



    @ManyToOne
    @JoinColumn(name="pick_id")
    @NotFound(action = NotFoundAction.IGNORE)
    private Pick pick;

    @ManyToOne
    @JoinColumn(name="bulk_pick_id")
    @NotFound(action = NotFoundAction.IGNORE)
    private BulkPick bulkPick;

    @ManyToOne
    @JoinColumn(name="short_allocation_id")
    @NotFound(action = NotFoundAction.IGNORE)
    private ShortAllocation shortAllocation;

    @ManyToOne
    @JoinColumn(name="shipment_id")
    @NotFound(action = NotFoundAction.IGNORE)
    private Shipment shipment;

    @ManyToOne
    @JoinColumn(name="shipment_line_id")
    @NotFound(action = NotFoundAction.IGNORE)
    private ShipmentLine shipmentLine;

    public OrderActivity(){};

    public OrderActivity(Long warehouseId, String transactionGroupId, String number) {
        this.warehouseId = warehouseId;
        this.transactionGroupId = transactionGroupId;
        this.number = number;

    }
    public static OrderActivity build(Long warehouseId, String transactionGroupId, String number) {

        return new OrderActivity(warehouseId, transactionGroupId, number)
                .withUsername(SecurityContextHolder.getContext().getAuthentication().getName())
                .withActivityDateTime(ZonedDateTime.now(ZoneOffset.UTC));
    }

    public static OrderActivity build(Long warehouseId, String transactionGroupId, String number, String username) {

        return new OrderActivity(warehouseId, transactionGroupId, number)
                .withUsername(username)
                .withActivityDateTime(ZonedDateTime.now(ZoneOffset.UTC));
    }
    public OrderActivity withActivityDateTime(ZonedDateTime activityDateTime) {
        setActivityDateTime(activityDateTime);
        return this;
    }
    public OrderActivity withUsername(String username) {
        setUsername(username);
        return this;
    }
    public OrderActivity withRFCode(String rfCode) {
        setRfCode(rfCode);
        return this;
    }
    public OrderActivity withOrderActivityType(OrderActivityType orderActivityType) {
        setOrderActivityType(orderActivityType);
        return this;
    }
    public OrderActivity withOrder(Order order) {
        setOrder(order);
        setOrderNumber(order.getNumber());
        setClientId(order.getClientId());

        return this;
    }
    public OrderActivity withOrderLine(OrderLine orderLine) {
        setOrderLine(orderLine);
        setOrderLineNumber(orderLine.getNumber());
        if (Objects.nonNull(orderLine.getOrder())) {

            setClientId(orderLine.getOrder().getClientId());
        }

        setOldOrderLineExpectedQuantity(orderLine.getExpectedQuantity());
        setOldOrderLineOpenQuantity(orderLine.getOpenQuantity());
        setOldOrderLineInProcessQuantity(orderLine.getInprocessQuantity());
        setOldOrderLineShippedQuantity(orderLine.getShippedQuantity());

        setNewOrderLineExpectedQuantity(orderLine.getExpectedQuantity());
        setNewOrderLineOpenQuantity(orderLine.getOpenQuantity());
        setNewOrderLineInProcessQuantity(orderLine.getInprocessQuantity());
        setNewOrderLineShippedQuantity(orderLine.getShippedQuantity());

        return this;
    }

    public OrderActivity setQuantityByNewOrderLine(OrderLine orderLine) {

        if (Objects.nonNull(orderLine.getOrder())) {

            setClientId(orderLine.getOrder().getClientId());
        }
        setNewOrderLineExpectedQuantity(orderLine.getExpectedQuantity());
        setNewOrderLineOpenQuantity(orderLine.getOpenQuantity());
        setNewOrderLineInProcessQuantity(orderLine.getInprocessQuantity());
        setNewOrderLineShippedQuantity(orderLine.getShippedQuantity());

        return this;
    }

    public OrderActivity withShipment(Shipment shipment) {
        // setShipment(shipment);
        setClientId(shipment.getClientId());

        if (Objects.isNull(shipment)) {
            return this;
        }
        setShipmentNumber(shipment.getNumber());
        return this;
    }
    public OrderActivity withShipmentLine(ShipmentLine shipmentLine) {
        // we will not setup the shipment line here. instead we will
        // use the information from the shipment line.
        // as the shipment line contains information of picks,
        // setup the shipment line in the order activity history may
        // contains too many unnecessary information

        // setShipmentLine(shipmentLine);

        if (Objects.isNull(shipmentLine)) {
            return this;
        }

        if (Objects.nonNull(shipmentLine.getShipment())) {

            setClientId(shipmentLine.getShipment().getClientId());
        }
        setShipmentLineNumber(shipmentLine.getNumber());

        setOldShipmentLineQuantity(shipmentLine.getQuantity());
        setOldShipmentLineOpenQuantity(shipmentLine.getOpenQuantity());
        setOldShipmentLineInProcessQuantity(shipmentLine.getInprocessQuantity());
        setOldShipmentLineLoadedQuantity(shipmentLine.getLoadedQuantity());
        setOldShipmentLineShippedQuantity(shipmentLine.getShippedQuantity());

        setNewShipmentLineQuantity(shipmentLine.getQuantity());
        setNewShipmentLineOpenQuantity(shipmentLine.getOpenQuantity());
        setNewShipmentLineInProcessQuantity(shipmentLine.getInprocessQuantity());
        setNewShipmentLineLoadedQuantity(shipmentLine.getLoadedQuantity());
        setNewShipmentLineShippedQuantity(shipmentLine.getShippedQuantity());

        return this;
    }

    public OrderActivity setQuantityByNewShipmentLine(ShipmentLine shipmentLine) {

        if (Objects.nonNull(shipmentLine.getShipment())) {

            setClientId(shipmentLine.getShipment().getClientId());
        }
        setNewShipmentLineQuantity(shipmentLine.getQuantity());
        setNewShipmentLineOpenQuantity(shipmentLine.getOpenQuantity());
        setNewShipmentLineInProcessQuantity(shipmentLine.getInprocessQuantity());
        setNewShipmentLineLoadedQuantity(shipmentLine.getLoadedQuantity());
        setNewShipmentLineShippedQuantity(shipmentLine.getShippedQuantity());

        return this;
    }
    public OrderActivity withPick(Pick pick) {
        setPick(pick);

        if (Objects.isNull(pick)) {
            return this;
        }
        if (Objects.nonNull(pick.getClient())) {

        }
        setClientId(pick.getClientId());
        setPickNumber(pick.getNumber());
        setOldPickQuantity(pick.getQuantity());
        setOldPickPickedQuantity(pick.getPickedQuantity());

        setNewPickQuantity(pick.getQuantity());
        setNewPickPickedQuantity(pick.getPickedQuantity());
        return this;
    }
    public OrderActivity withBulkPick(BulkPick bulkPick) {
        setBulkPick(bulkPick);

        if (Objects.isNull(bulkPick)) {
            return this;
        }
        setBulkPickNumber(bulkPick.getNumber());
        return this;
    }

    public OrderActivity setQuantityByNewPick(Pick pick) {

        setNewPickQuantity(pick.getQuantity());
        setNewPickPickedQuantity(pick.getPickedQuantity());
        return this;
    }
    public OrderActivity withShortAllocation(ShortAllocation shortAllocation) {
        setShortAllocation(shortAllocation);

        if (Objects.isNull(shortAllocation)) {
            return this;
        }
        setClientId(shortAllocation.getClientId());
        setOldShortAllocationQuantity(shortAllocation.getQuantity());
        setOldShortAllocationOpenQuantity(shortAllocation.getOpenQuantity());
        setOldShortAllocationInProcessQuantity(shortAllocation.getInprocessQuantity());
        setOldShortAllocationDeliveredQuantity(shortAllocation.getDeliveredQuantity());

        setNewShortAllocationQuantity(shortAllocation.getQuantity());
        setNewShortAllocationOpenQuantity(shortAllocation.getOpenQuantity());
        setNewShortAllocationInProcessQuantity(shortAllocation.getInprocessQuantity());
        setNewShortAllocationDeliveredQuantity(shortAllocation.getDeliveredQuantity());

        return this;
    }
    public OrderActivity setQuantityByNewShortAllocation(ShortAllocation shortAllocation) {

        setNewShortAllocationQuantity(shortAllocation.getQuantity());
        setNewShortAllocationOpenQuantity(shortAllocation.getOpenQuantity());
        setNewShortAllocationInProcessQuantity(shortAllocation.getInprocessQuantity());
        setNewShortAllocationDeliveredQuantity(shortAllocation.getDeliveredQuantity());

        return this;
    }

    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    public ZonedDateTime getActivityDateTime() {
        return activityDateTime;
    }

    public void setActivityDateTime(ZonedDateTime activityDateTime) {
        this.activityDateTime = activityDateTime;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public Order getOrder() {
        return order;
    }

    public void setOrder(Order order) {
        this.order = order;
    }

    public OrderActivityType getOrderActivityType() {
        return orderActivityType;
    }

    public void setOrderActivityType(OrderActivityType orderActivityType) {
        this.orderActivityType = orderActivityType;
    }

    public Long getWarehouseId() {
        return warehouseId;
    }

    public void setWarehouseId(Long warehouseId) {
        this.warehouseId = warehouseId;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getQuantity() {
        return quantity;
    }

    public void setQuantity(Long quantity) {
        this.quantity = quantity;
    }

    public Pick getPick() {
        return pick;
    }

    public void setPick(Pick pick) {
        this.pick = pick;
    }

    public ShortAllocation getShortAllocation() {
        return shortAllocation;
    }

    public void setShortAllocation(ShortAllocation shortAllocation) {
        this.shortAllocation = shortAllocation;
    }

    public Shipment getShipment() {
        return shipment;
    }

    public void setShipment(Shipment shipment) {
        this.shipment = shipment;
    }

    public ShipmentLine getShipmentLine() {
        return shipmentLine;
    }

    public void setShipmentLine(ShipmentLine shipmentLine) {
        this.shipmentLine = shipmentLine;
    }

    public String getRfCode() {
        return rfCode;
    }

    public void setRfCode(String rfCode) {
        this.rfCode = rfCode;
    }

    public OrderLine getOrderLine() {
        return orderLine;
    }

    public void setOrderLine(OrderLine orderLine) {
        this.orderLine = orderLine;
    }

    public String getOrderNumber() {
        return orderNumber;
    }

    public void setOrderNumber(String orderNumber) {
        this.orderNumber = orderNumber;
    }

    public String getOrderLineNumber() {
        return orderLineNumber;
    }

    public void setOrderLineNumber(String orderLineNumber) {
        this.orderLineNumber = orderLineNumber;
    }

    public Long getOldOrderLineExpectedQuantity() {
        return oldOrderLineExpectedQuantity;
    }

    public void setOldOrderLineExpectedQuantity(Long oldOrderLineExpectedQuantity) {
        this.oldOrderLineExpectedQuantity = oldOrderLineExpectedQuantity;
    }

    public Long getNewOrderLineExpectedQuantity() {
        return newOrderLineExpectedQuantity;
    }

    public void setNewOrderLineExpectedQuantity(Long newOrderLineExpectedQuantity) {
        this.newOrderLineExpectedQuantity = newOrderLineExpectedQuantity;
    }

    public Long getOldOrderLineOpenQuantity() {
        return oldOrderLineOpenQuantity;
    }

    public void setOldOrderLineOpenQuantity(Long oldOrderLineOpenQuantity) {
        this.oldOrderLineOpenQuantity = oldOrderLineOpenQuantity;
    }

    public Long getNewOrderLineOpenQuantity() {
        return newOrderLineOpenQuantity;
    }

    public void setNewOrderLineOpenQuantity(Long newOrderLineOpenQuantity) {
        this.newOrderLineOpenQuantity = newOrderLineOpenQuantity;
    }

    public Long getOldOrderLineInProcessQuantity() {
        return oldOrderLineInProcessQuantity;
    }

    public void setOldOrderLineInProcessQuantity(Long oldOrderLineInProcessQuantity) {
        this.oldOrderLineInProcessQuantity = oldOrderLineInProcessQuantity;
    }

    public Long getNewOrderLineInProcessQuantity() {
        return newOrderLineInProcessQuantity;
    }

    public void setNewOrderLineInProcessQuantity(Long newOrderLineInProcessQuantity) {
        this.newOrderLineInProcessQuantity = newOrderLineInProcessQuantity;
    }

    public Long getOldOrderLineShippedQuantity() {
        return oldOrderLineShippedQuantity;
    }

    public void setOldOrderLineShippedQuantity(Long oldOrderLineShippedQuantity) {
        this.oldOrderLineShippedQuantity = oldOrderLineShippedQuantity;
    }

    public Long getNewOrderLineShippedQuantity() {
        return newOrderLineShippedQuantity;
    }

    public void setNewOrderLineShippedQuantity(Long newOrderLineShippedQuantity) {
        this.newOrderLineShippedQuantity = newOrderLineShippedQuantity;
    }

    public String getShipmentNumber() {
        return shipmentNumber;
    }

    public void setShipmentNumber(String shipmentNumber) {
        this.shipmentNumber = shipmentNumber;
    }

    public String getShipmentLineNumber() {
        return shipmentLineNumber;
    }

    public void setShipmentLineNumber(String shipmentLineNumber) {
        this.shipmentLineNumber = shipmentLineNumber;
    }

    public Long getOldShipmentLineQuantity() {
        return oldShipmentLineQuantity;
    }

    public void setOldShipmentLineQuantity(Long oldShipmentLineQuantity) {
        this.oldShipmentLineQuantity = oldShipmentLineQuantity;
    }

    public Long getNewShipmentLineQuantity() {
        return newShipmentLineQuantity;
    }

    public void setNewShipmentLineQuantity(Long newShipmentLineQuantity) {
        this.newShipmentLineQuantity = newShipmentLineQuantity;
    }

    public Long getOldShipmentLineOpenQuantity() {
        return oldShipmentLineOpenQuantity;
    }

    public void setOldShipmentLineOpenQuantity(Long oldShipmentLineOpenQuantity) {
        this.oldShipmentLineOpenQuantity = oldShipmentLineOpenQuantity;
    }

    public Long getNewShipmentLineOpenQuantity() {
        return newShipmentLineOpenQuantity;
    }

    public void setNewShipmentLineOpenQuantity(Long newShipmentLineOpenQuantity) {
        this.newShipmentLineOpenQuantity = newShipmentLineOpenQuantity;
    }

    public Long getOldShipmentLineInProcessQuantity() {
        return oldShipmentLineInProcessQuantity;
    }

    public void setOldShipmentLineInProcessQuantity(Long oldShipmentLineInProcessQuantity) {
        this.oldShipmentLineInProcessQuantity = oldShipmentLineInProcessQuantity;
    }

    public Long getNewShipmentLineInProcessQuantity() {
        return newShipmentLineInProcessQuantity;
    }

    public void setNewShipmentLineInProcessQuantity(Long newShipmentLineInProcessQuantity) {
        this.newShipmentLineInProcessQuantity = newShipmentLineInProcessQuantity;
    }

    public Long getOldShipmentLineLoadedQuantity() {
        return oldShipmentLineLoadedQuantity;
    }

    public void setOldShipmentLineLoadedQuantity(Long oldShipmentLineLoadedQuantity) {
        this.oldShipmentLineLoadedQuantity = oldShipmentLineLoadedQuantity;
    }

    public Long getNewShipmentLineLoadedQuantity() {
        return newShipmentLineLoadedQuantity;
    }

    public void setNewShipmentLineLoadedQuantity(Long newShipmentLineLoadedQuantity) {
        this.newShipmentLineLoadedQuantity = newShipmentLineLoadedQuantity;
    }

    public Long getOldShipmentLineShippedQuantity() {
        return oldShipmentLineShippedQuantity;
    }

    public void setOldShipmentLineShippedQuantity(Long oldShipmentLineShippedQuantity) {
        this.oldShipmentLineShippedQuantity = oldShipmentLineShippedQuantity;
    }

    public Long getNewShipmentLineShippedQuantity() {
        return newShipmentLineShippedQuantity;
    }

    public void setNewShipmentLineShippedQuantity(Long newShipmentLineShippedQuantity) {
        this.newShipmentLineShippedQuantity = newShipmentLineShippedQuantity;
    }

    public String getPickNumber() {
        return pickNumber;
    }

    public void setPickNumber(String pickNumber) {
        this.pickNumber = pickNumber;
    }

    public Long getOldPickQuantity() {
        return oldPickQuantity;
    }

    public void setOldPickQuantity(Long oldPickQuantity) {
        this.oldPickQuantity = oldPickQuantity;
    }

    public Long getNewPickQuantity() {
        return newPickQuantity;
    }

    public void setNewPickQuantity(Long newPickQuantity) {
        this.newPickQuantity = newPickQuantity;
    }

    public Long getOldPickPickedQuantity() {
        return oldPickPickedQuantity;
    }

    public void setOldPickPickedQuantity(Long oldPickPickedQuantity) {
        this.oldPickPickedQuantity = oldPickPickedQuantity;
    }

    public Long getClientId() {
        return clientId;
    }

    public void setClientId(Long clientId) {
        this.clientId = clientId;
    }

    public Client getClient() {
        return client;
    }

    public void setClient(Client client) {
        this.client = client;
    }

    public Long getNewPickPickedQuantity() {
        return newPickPickedQuantity;
    }

    public void setNewPickPickedQuantity(Long newPickPickedQuantity) {
        this.newPickPickedQuantity = newPickPickedQuantity;
    }

    public Long getOldShortAllocationQuantity() {
        return oldShortAllocationQuantity;
    }

    public void setOldShortAllocationQuantity(Long oldShortAllocationQuantity) {
        this.oldShortAllocationQuantity = oldShortAllocationQuantity;
    }

    public Long getNewShortAllocationQuantity() {
        return newShortAllocationQuantity;
    }

    public void setNewShortAllocationQuantity(Long newShortAllocationQuantity) {
        this.newShortAllocationQuantity = newShortAllocationQuantity;
    }

    public Long getOldShortAllocationOpenQuantity() {
        return oldShortAllocationOpenQuantity;
    }

    public void setOldShortAllocationOpenQuantity(Long oldShortAllocationOpenQuantity) {
        this.oldShortAllocationOpenQuantity = oldShortAllocationOpenQuantity;
    }

    public Long getNewShortAllocationOpenQuantity() {
        return newShortAllocationOpenQuantity;
    }

    public void setNewShortAllocationOpenQuantity(Long newShortAllocationOpenQuantity) {
        this.newShortAllocationOpenQuantity = newShortAllocationOpenQuantity;
    }

    public Long getOldShortAllocationInProcessQuantity() {
        return oldShortAllocationInProcessQuantity;
    }

    public void setOldShortAllocationInProcessQuantity(Long oldShortAllocationInProcessQuantity) {
        this.oldShortAllocationInProcessQuantity = oldShortAllocationInProcessQuantity;
    }

    public Long getNewShortAllocationInProcessQuantity() {
        return newShortAllocationInProcessQuantity;
    }

    public void setNewShortAllocationInProcessQuantity(Long newShortAllocationInProcessQuantity) {
        this.newShortAllocationInProcessQuantity = newShortAllocationInProcessQuantity;
    }

    public Long getOldShortAllocationDeliveredQuantity() {
        return oldShortAllocationDeliveredQuantity;
    }

    public void setOldShortAllocationDeliveredQuantity(Long oldShortAllocationDeliveredQuantity) {
        this.oldShortAllocationDeliveredQuantity = oldShortAllocationDeliveredQuantity;
    }

    public Long getNewShortAllocationDeliveredQuantity() {
        return newShortAllocationDeliveredQuantity;
    }

    public void setNewShortAllocationDeliveredQuantity(Long newShortAllocationDeliveredQuantity) {
        this.newShortAllocationDeliveredQuantity = newShortAllocationDeliveredQuantity;
    }

    public String getTransactionGroupId() {
        return transactionGroupId;
    }

    public void setTransactionGroupId(String transactionGroupId) {
        this.transactionGroupId = transactionGroupId;
    }

    public String getBulkPickNumber() {
        return bulkPickNumber;
    }

    public void setBulkPickNumber(String bulkPickNumber) {
        this.bulkPickNumber = bulkPickNumber;
    }

    public BulkPick getBulkPick() {
        return bulkPick;
    }

    public void setBulkPick(BulkPick bulkPick) {
        this.bulkPick = bulkPick;
    }
}
