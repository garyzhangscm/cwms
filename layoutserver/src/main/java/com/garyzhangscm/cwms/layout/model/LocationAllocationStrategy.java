package com.garyzhangscm.cwms.layout.model;

import org.codehaus.jackson.annotate.JsonProperty;

import javax.persistence.*;

@Entity
@Table(name="location_allocation_strategy")
public class LocationAllocationStrategy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "location_allocation_strategy_id")
    @JsonProperty(value="id")
    private Long id;

    @OneToOne
    @JoinColumn(name="location_group_id")
    private LocationGroup locationGroup;




}
