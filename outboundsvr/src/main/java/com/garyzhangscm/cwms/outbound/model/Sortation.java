package com.garyzhangscm.cwms.outbound.model;

import org.codehaus.jackson.annotate.JsonProperty;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "sortation")
public class Sortation  extends AuditibleEntity<String> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "sortation_id")
    @JsonProperty(value="id")
    private Long id;


    @Column(name = "warehouse_id")
    private Long warehouseId;

    @Column(name = "number")
    private String number;

    @ManyToOne
    @JoinColumn(name="wave_id")
    private Wave wave;

    @Transient
    private Location location;

    @Column(name = "location_id")
    private Long locationId;

    @OneToMany(
            mappedBy = "sortation",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.EAGER
    )
    List<SortationByShipment> sortationByShipments = new ArrayList<>();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getWarehouseId() {
        return warehouseId;
    }

    public void setWarehouseId(Long warehouseId) {
        this.warehouseId = warehouseId;
    }

    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    public Wave getWave() {
        return wave;
    }

    public void setWave(Wave wave) {
        this.wave = wave;
    }

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    public Long getLocationId() {
        return locationId;
    }

    public void setLocationId(Long locationId) {
        this.locationId = locationId;
    }

    public List<SortationByShipment> getSortationByShipments() {
        return sortationByShipments;
    }

    public void setSortationByShipments(List<SortationByShipment> sortationByShipments) {
        this.sortationByShipments = sortationByShipments;
    }

    public void addSortationByShipment(SortationByShipment sortationByShipment) {
        sortationByShipments.add(sortationByShipment);
    }
}
