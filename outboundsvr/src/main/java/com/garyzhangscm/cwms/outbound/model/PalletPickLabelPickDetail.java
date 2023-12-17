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
import java.util.Objects;

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
    private PalletPickLabelContent palletPickLabelContent;

    // if the pick needs to be picks with multiple pallet
    // then the pickQuantity is only for this pallet
    @Column(name = "pick_quantity")
    private Long pickQuantity;

    @Column(name = "case_quantity")
    private Long caseQuantity;
    @Column(name = "case_unit_of_measure_name")
    private String caseUnitOfMeasureName;

    // estimated volume
    @Column(name = "volume")
    private Double volume;

    public PalletPickLabelPickDetail() {}
    public PalletPickLabelPickDetail(PalletPickLabelContent PalletPickLabelContent, Pick pick, Long pickQuantity) {
        this.pick = pick;
        this.palletPickLabelContent = palletPickLabelContent;
        this.pickQuantity = pickQuantity;


        if (pick.getItem() == null) {
            setVolume(0.0);
            setCaseQuantity(pickQuantity);
        }
        else {
            ItemPackageType itemPackageType = pick.getItemPackageType();
            if (Objects.isNull(itemPackageType)) {
                itemPackageType = pick.getItem().getDefaultItemPackageType();
            }
            if (Objects.nonNull(itemPackageType)) {
                ItemUnitOfMeasure stockItemUnitOfMeasure = itemPackageType.getStockItemUnitOfMeasures();
                ItemUnitOfMeasure caseItemUnitOfMeasure = itemPackageType.getCaseItemUnitOfMeasure();
                setVolume(
                        (pickQuantity / stockItemUnitOfMeasure.getQuantity())
                                * stockItemUnitOfMeasure.getLength()
                                * stockItemUnitOfMeasure.getWidth()
                                * stockItemUnitOfMeasure.getHeight()
                );
                if (Objects.nonNull(caseItemUnitOfMeasure)) {
                    setCaseQuantity(
                            (long)Math.ceil(pickQuantity / caseItemUnitOfMeasure.getQuantity())
                    );
                }
                else {
                    setCaseQuantity(pickQuantity);
                }
            }



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

    public PalletPickLabelContent getPalletPickLabelContent() {
        return palletPickLabelContent;
    }

    public void setPalletPickLabelContent(PalletPickLabelContent palletPickLabelContent) {
        this.palletPickLabelContent = palletPickLabelContent;
    }

    public Double getVolume() {
        return volume;
    }

    public void setVolume(Double volume) {
        this.volume = volume;
    }

    public Long getCaseQuantity() {
        return caseQuantity;
    }

    public void setCaseQuantity(Long caseQuantity) {
        this.caseQuantity = caseQuantity;
    }

    public String getCaseUnitOfMeasureName() {
        return caseUnitOfMeasureName;
    }

    public void setCaseUnitOfMeasureName(String caseUnitOfMeasureName) {
        this.caseUnitOfMeasureName = caseUnitOfMeasureName;
    }
}
