package com.garyzhangscm.cwms.outbound.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.codehaus.jackson.annotate.JsonProperty;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "sortation_by_shipment")
public class SortationByShipment extends AuditibleEntity<String> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "sortation_by_shipment_id")
    @JsonProperty(value="id")
    private Long id;


    @JsonIgnore
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "sortation_id")
    private Sortation sortation;


    // sort by shipment
    @ManyToOne
    @JoinColumn(name="shipment_id")
    private Shipment shipment;


    @OneToMany(
            mappedBy = "sortationByShipment",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.EAGER
    )
    private List<SortationByShipmentLine> sortationByShipmentLines = new ArrayList<>();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Sortation getSortation() {
        return sortation;
    }

    public void setSortation(Sortation sortation) {
        this.sortation = sortation;
    }

    public Shipment getShipment() {
        return shipment;
    }

    public void setShipment(Shipment shipment) {
        this.shipment = shipment;
    }

    public List<SortationByShipmentLine> getSortationByShipmentLines() {
        return sortationByShipmentLines;
    }

    public void setSortationByShipmentLines(List<SortationByShipmentLine> sortationByShipmentLines) {
        this.sortationByShipmentLines = sortationByShipmentLines;
    }

    public List<SortationByShipmentLine> getSortationByOrderLines() {
        return sortationByShipmentLines;
    }

    public void setSortationByOrderLines(List<SortationByShipmentLine> sortationByShipmentLines) {
        this.sortationByShipmentLines = sortationByShipmentLines;
    }

    public void addSortationByShipmentLine(SortationByShipmentLine sortationByShipmentLine) {
        sortationByShipmentLines.add(sortationByShipmentLine);
    }
}
