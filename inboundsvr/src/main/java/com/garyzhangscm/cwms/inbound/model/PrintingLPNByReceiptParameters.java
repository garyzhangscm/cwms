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

package com.garyzhangscm.cwms.inbound.model;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PrintingLPNByReceiptParameters implements Serializable {

    // key: receipt line id
    // value: lable count
    private Map<Long, Integer> lpnLabelCountByReceiptLines;

    // key: receipt line id
    // value: quantity to be printedon label, can be null
    private Map<Long, Long> lpnQuantityOnLabelByReceiptLines;

    // key: receipt line id
    // value: whether to ignore the lpn quantity on the label
    private Map<Long, Boolean> ignoreInventoryQuantityByReceiptLines;


    public int getTotalLPNCount() {
        if (Objects.isNull(lpnLabelCountByReceiptLines)) {
            return 0;
        }
        return lpnLabelCountByReceiptLines.entrySet().stream().map(
                mapEntry -> mapEntry.getValue()
        ).mapToInt(Integer::intValue).sum();
    }
    @Override
    public String toString() {
        try {
            return new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(this);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return null;
    }

    public Map<Long, Integer> getLpnLabelCountByReceiptLines() {
        return lpnLabelCountByReceiptLines;
    }

    public void setLpnLabelCountByReceiptLines(Map<Long, Integer> lpnLabelCountByReceiptLines) {
        this.lpnLabelCountByReceiptLines = lpnLabelCountByReceiptLines;
    }

    public Map<Long, Long> getLpnQuantityOnLabelByReceiptLines() {
        return lpnQuantityOnLabelByReceiptLines;
    }

    public void setLpnQuantityOnLabelByReceiptLines(Map<Long, Long> lpnQuantityOnLabelByReceiptLines) {
        this.lpnQuantityOnLabelByReceiptLines = lpnQuantityOnLabelByReceiptLines;
    }

    public Map<Long, Boolean> getIgnoreInventoryQuantityByReceiptLines() {
        return ignoreInventoryQuantityByReceiptLines;
    }

    public void setIgnoreInventoryQuantityByReceiptLines(Map<Long, Boolean> ignoreInventoryQuantityByReceiptLines) {
        this.ignoreInventoryQuantityByReceiptLines = ignoreInventoryQuantityByReceiptLines;
    }
}
