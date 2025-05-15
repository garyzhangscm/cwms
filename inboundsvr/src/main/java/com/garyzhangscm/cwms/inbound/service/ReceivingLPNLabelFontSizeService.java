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

package com.garyzhangscm.cwms.inbound.service;

import com.garyzhangscm.cwms.inbound.model.*;
import com.garyzhangscm.cwms.inbound.repository.ReceivingLPNLabelFontSizeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.*;

@Service
public class ReceivingLPNLabelFontSizeService {
    private static final Logger logger = LoggerFactory.getLogger(ReceivingLPNLabelFontSizeService.class);

    @Autowired
    private ReceivingLPNLabelFontSizeRepository receivingLPNLabelFontSizeRepository;

    public ReceivingLPNLabelFontSize getReceivingLPNLabelFontSize(Long warehouseId, ReceivingLPNLabelFontType type) {
        ReceivingLPNLabelFontSize receivingLPNLabelFontSize =
                receivingLPNLabelFontSizeRepository.findByWarehouseId(warehouseId, type.name());
        if (Objects.nonNull(receivingLPNLabelFontSize)) {

            logger.debug("Found font size for type {} of warehouse {}, \n {}",
                    type, warehouseId, receivingLPNLabelFontSize);

            return receivingLPNLabelFontSize;
        }

        logger.debug("CANNOT find font size for type {} of warehouse {}, we will return the default ",
                type, warehouseId);
        switch (type) {
            case COLOR:
                return new ReceivingLPNLabelFontSize(
                        warehouseId,
                        ReceivingLPNLabelFontType.COLOR,
                        7, 100,
                        3, 10);
            case STYLE:
                return new ReceivingLPNLabelFontSize(
                        warehouseId,
                        ReceivingLPNLabelFontType.STYLE,
                        8, 105, 3, 10);
            case PRODUCT_SIZE:
                return new ReceivingLPNLabelFontSize(
                        warehouseId,
                        ReceivingLPNLabelFontType.PRODUCT_SIZE,
                        8, 85, 3, 10);
        }
        return new ReceivingLPNLabelFontSize(
                warehouseId,
                ReceivingLPNLabelFontType.COLOR,
                7, 100,
                3, 10);
    }
}
