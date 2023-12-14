/**
 * Copyright 2019
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

package com.garyzhangscm.cwms.outbound.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.codehaus.jackson.annotate.JsonProperty;

import javax.persistence.*;
import java.io.Serializable;

@Entity
@Table(name = "pallet_pick_label_pick_detail")
public class PalletPickLabelPickDetail extends AuditibleEntity<String> implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "pallet_pick_label_pick_detail_id")
    @JsonProperty(value="id")
    private Long id;

    @ManyToOne
    @JoinColumn(name="pick_id")
    private Pick pick;

    @JsonIgnore
    @ManyToOne
    @JoinColumn(name = "pallet_pick_label_content_id")
    private PalletPickLabelContent PalletPickLabelContent;

    // if the pick needs to be picks with multiple pallet
    // then the pickQuantity is only for this pallet
    @Column(name = "pick_quantity")
    private Long pickQuantity;

    // estimated volume
    @Column(name = "volume")
    private Double volume;

    public PalletPickLabelPickDetail() {}
    public PalletPickLabelPickDetail(PalletPickLabelContent PalletPickLabelContent, Pick pick, Long pickQuantity) {
        this.pick = pick;
        this.PalletPickLabelContent = PalletPickLabelContent;
        this.pickQuantity = pickQuantity;


        if (pick.getItem() == null) {
            setVolume(0.0);
        }
        else {

            ItemUnitOfMeasure stockItemUnitOfMeasure = pick.getItem().getDefaultItemPackageType().getStockItemUnitOfMeasures();
            setVolume(
                    (pickQuantity / stockItemUnitOfMeasure.getQuantity())
                    * stockItemUnitOfMeasure.getLength()
                    * stockItemUnitOfMeasure.getWidth()
                    * stockItemUnitOfMeasure.getHeight()
            );
        }


    }


    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Pick getPick() {
        return pick;
    }

    public void setPick(Pick pick) {
        this.pick = pick;
    }

    public Long getPickQuantity() {
        return pickQuantity;
    }

    public void setPickQuantity(Long pickQuantity) {
        this.pickQuantity = pickQuantity;
    }

    public com.garyzhangscm.cwms.outbound.model.PalletPickLabelContent getPalletPickLabelContent() {
        return PalletPickLabelContent;
    }

    public void setPalletPickLabelContent(com.garyzhangscm.cwms.outbound.model.PalletPickLabelContent palletPickLabelContent) {
        PalletPickLabelContent = palletPickLabelContent;
    }

    public Double getVolume() {
        return volume;
    }

    public void setVolume(Double volume) {
        this.volume = volume;
    }

}
