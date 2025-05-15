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

package com.garyzhangscm.cwms.inventory.service;

import com.garyzhangscm.cwms.inventory.model.LPNLabelFontSize;
import com.garyzhangscm.cwms.inventory.model.LPNLabelFontType;
import com.garyzhangscm.cwms.inventory.repository.LPNLabelFontSizeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
public class LPNLabelFontSizeService {
    private static final Logger logger = LoggerFactory.getLogger(LPNLabelFontSizeService.class);

    @Autowired
    private LPNLabelFontSizeRepository lpnLabelFontSizeRepository;

    public LPNLabelFontSize getReceivingLPNLabelFontSize(Long warehouseId, LPNLabelFontType type) {
        LPNLabelFontSize lpnLabelFontSize =
                lpnLabelFontSizeRepository.findByWarehouseId(warehouseId, type.name());
        if (Objects.nonNull(lpnLabelFontSize)) {

            logger.debug("Found font size for type {} of warehouse {}, \n {}",
                    type, warehouseId, lpnLabelFontSize);

            return lpnLabelFontSize;
        }

        logger.debug("CANNOT find font size for type {} of warehouse {}, we will return the default ",
                type, warehouseId);
        switch (type) {
            case COLOR:
                return new LPNLabelFontSize(
                        warehouseId,
                        LPNLabelFontType.COLOR,
                        7, 100,
                        3, 10);
            case STYLE:
                return new LPNLabelFontSize(
                        warehouseId,
                        LPNLabelFontType.STYLE,
                        8, 105, 3, 10);
            case PRODUCT_SIZE:
                return new LPNLabelFontSize(
                        warehouseId,
                        LPNLabelFontType.PRODUCT_SIZE,
                        8, 85, 3, 10);
        }
        return new LPNLabelFontSize(
                warehouseId,
                LPNLabelFontType.COLOR,
                7, 100,
                3, 10);
    }
}
