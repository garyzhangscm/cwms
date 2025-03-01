package com.garyzhangscm.cwms.outbound.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "sortation_by_shipment_line")
public class SortationByShipmentLine extends AuditibleEntity<String> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "sortation_by_shipment_line_id")
    @JsonProperty(value="id")
    private Long id;


    @OneToMany(
            mappedBy = "sortationByShipmentLine",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    private List<SortationByShipmentLineHistory> sortationByOrderLineHistories = new ArrayList<>();

    @JsonIgnore
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "sortation_by_shipment_id")
    private SortationByShipment sortationByShipment;


    // sort by shipment line
    @ManyToOne
    @JsonIgnore
    @JoinColumn(name="shipment_line_id")
    private ShipmentLine shipmentLine;


    @Column(name = "expected_quantity")
    private Long expectedQuantity;

    @Column(name = "arrived_quantity")
    private Long arrivedQuantity;

    @Column(name = "sorted_quantity")
    private Long sortedQuantity;

    public Long getShipmentId() {
        return getSortationByShipment().getShipment().getId();
    }

    public Long getItemId() {
        return getShipmentLine().getOrderLine().getItemId();
    }
    public String getItemName() {
        return Objects.isNull(getShipmentLine().getOrderLine().getItem()) ?
                "" : getShipmentLine().getOrderLine().getItem().getName();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public List<SortationByShipmentLineHistory> getSortationByOrderLineHistories() {
        return sortationByOrderLineHistories;
    }

    public void setSortationByOrderLineHistories(List<SortationByShipmentLineHistory> sortationByOrderLineHistories) {
        this.sortationByOrderLineHistories = sortationByOrderLineHistories;
    }

    public SortationByShipment getSortationByShipment() {
        return sortationByShipment;
    }

    public void setSortationByShipment(SortationByShipment sortationByShipment) {
        this.sortationByShipment = sortationByShipment;
    }

    public ShipmentLine getShipmentLine() {
        return shipmentLine;
    }

    public void setShipmentLine(ShipmentLine shipmentLine) {
        this.shipmentLine = shipmentLine;
    }

    public Long getExpectedQuantity() {
        return expectedQuantity;
    }

    public void setExpectedQuantity(Long expectedQuantity) {
        this.expectedQuantity = expectedQuantity;
    }

    public Long getArrivedQuantity() {
        return arrivedQuantity;
    }

    public void setArrivedQuantity(Long arrivedQuantity) {
        this.arrivedQuantity = arrivedQuantity;
    }

    public Long getSortedQuantity() {
        return sortedQuantity;
    }

    public void setSortedQuantity(Long sortedQuantity) {
        this.sortedQuantity = sortedQuantity;
    }
}
