package com.garyzhangscm.cwms.common.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import javax.persistence.*;

@Entity
@Table(name = "carrier_service_level")
public class CarrierServiceLevel extends AuditibleEntity<String>  {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "carrier_service_level_id")
    @JsonProperty(value="id")
    private Long id;

    @Column(name = "name")
    private String name;

    @Column(name = "description")
    private String description;


    @Column(name = "type")
    private CarrierServiceLevelType type;

    @ManyToOne
    @JoinColumn(name = "carrier_id")
    @JsonIgnore
    private Carrier carrier;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public CarrierServiceLevelType getType() {
        return type;
    }

    public void setType(CarrierServiceLevelType type) {
        this.type = type;
    }

    public Carrier getCarrier() {
        return carrier;
    }

    public void setCarrier(Carrier carrier) {
        this.carrier = carrier;
    }
}
