/**
 * Copyright 2018
 *
 * @author gzhang
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.garyzhangscm.cwms.inventory.model;


import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.persistence.*;
import java.io.Serializable;

@Entity
@Table(name = "inventory_mix_restriction_line")
public class InventoryMixRestrictionLine extends AuditibleEntity<String> implements Serializable {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "inventory_mix_restriction_line_id")
    @JsonProperty(value="id")
    private Long id;


    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inventory_mix_restriction_id")
    private InventoryMixRestriction inventoryMixRestriction;


    // whether we will check the inventory mix rule at
    // location or LPN.
    @Column(name = "type")
    @Enumerated(EnumType.STRING)
    private InventoryMixRestrictionLineType type;


    // which attribute that we are NOT allowed to be mix
    @Column(name = "attribute")
    @Enumerated(EnumType.STRING)
    private InventoryMixRestrictionAttribute attribute;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public InventoryMixRestriction getInventoryMixRestriction() {
        return inventoryMixRestriction;
    }

    public void setInventoryMixRestriction(InventoryMixRestriction inventoryMixRestriction) {
        this.inventoryMixRestriction = inventoryMixRestriction;
    }

    public InventoryMixRestrictionLineType getType() {
        return type;
    }

    public void setType(InventoryMixRestrictionLineType type) {
        this.type = type;
    }

    public InventoryMixRestrictionAttribute getAttribute() {
        return attribute;
    }

    public void setAttribute(InventoryMixRestrictionAttribute attribute) {
        this.attribute = attribute;
    }
}
