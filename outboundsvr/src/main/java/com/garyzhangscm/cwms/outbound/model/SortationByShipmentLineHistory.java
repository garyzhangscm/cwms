package com.garyzhangscm.cwms.outbound.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.codehaus.jackson.annotate.JsonProperty;

import javax.persistence.*;

@Entity
@Table(name = "sortation_by_shipment_line_history")
public class SortationByShipmentLineHistory extends AuditibleEntity<String> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "sortation_by_shipment_line_history_id")
    @JsonProperty(value="id")
    private Long id;


    @JsonIgnore
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "sortation_by_shipment_line_id")
    private SortationByShipmentLine sortationByShipmentLine;


    @Column(name = "barcode")
    private String barcode;

    @Column(name = "sorted_quantity")
    private Integer sortedQuantity;

    @Column(name = "sorted_username")
    private String sortedUsername;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public SortationByShipmentLine getSortationByShipmentLine() {
        return sortationByShipmentLine;
    }

    public void setSortationByShipmentLine(SortationByShipmentLine sortationByShipmentLine) {
        this.sortationByShipmentLine = sortationByShipmentLine;
    }

    public String getBarcode() {
        return barcode;
    }

    public void setBarcode(String barcode) {
        this.barcode = barcode;
    }

    public Integer getSortedQuantity() {
        return sortedQuantity;
    }

    public void setSortedQuantity(Integer sortedQuantity) {
        this.sortedQuantity = sortedQuantity;
    }

    public String getSortedUsername() {
        return sortedUsername;
    }

    public void setSortedUsername(String sortedUsername) {
        this.sortedUsername = sortedUsername;
    }
}
