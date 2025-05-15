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

package com.garyzhangscm.cwms.inbound.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import javax.persistence.*;


@Entity
@Table(name = "receiving_lpn_label_font_size")
public class ReceivingLPNLabelFontSize extends AuditibleEntity<String>{


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "receiving_lpn_label_font_size_id")
    @JsonProperty(value="id")
    private Long id;

    @Column(name = "type")
    @Enumerated(EnumType.STRING)
    private ReceivingLPNLabelFontType type;

    @Column(name = "characters_per_line")
    private Integer charactersPerLine;
    @Column(name = "base_size")
    private Integer baseSize;
    @Column(name = "base_size_line_count")
    private Integer baseSizeLineCount;
    @Column(name = "step")
    private Integer step;

    @Column(name = "warehouse_id")
    private Long warehouseId;

    public ReceivingLPNLabelFontSize(){}
    public ReceivingLPNLabelFontSize(Long warehouseId,
                                     ReceivingLPNLabelFontType type,
                                     Integer charactersPerLine, Integer baseSize,
                                     Integer baseSizeLineCount, Integer step) {
        this.type = type;
        this.charactersPerLine = charactersPerLine;
        this.baseSize = baseSize;
        this.baseSizeLineCount = baseSizeLineCount;
        this.step = step;
        this.warehouseId = warehouseId;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public ReceivingLPNLabelFontType getType() {
        return type;
    }

    public void setType(ReceivingLPNLabelFontType type) {
        this.type = type;
    }

    public Integer getCharactersPerLine() {
        return charactersPerLine;
    }

    public void setCharactersPerLine(Integer charactersPerLine) {
        this.charactersPerLine = charactersPerLine;
    }

    public Integer getBaseSize() {
        return baseSize;
    }

    public void setBaseSize(Integer baseSize) {
        this.baseSize = baseSize;
    }

    public Integer getBaseSizeLineCount() {
        return baseSizeLineCount;
    }

    public void setBaseSizeLineCount(Integer baseSizeLineCount) {
        this.baseSizeLineCount = baseSizeLineCount;
    }

    public Integer getStep() {
        return step;
    }

    public void setStep(Integer step) {
        this.step = step;
    }

    public Long getWarehouseId() {
        return warehouseId;
    }

    public void setWarehouseId(Long warehouseId) {
        this.warehouseId = warehouseId;
    }
}
