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

package com.garyzhangscm.cwms.resources.service;

import com.garyzhangscm.cwms.resources.model.ReportType;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


@Service
public class PrinterService  {
    private static final Logger logger = LoggerFactory.getLogger(PrinterService.class);

    @Autowired
    private ReportPrinterConfigurationService reportPrinterConfigurationService;
    public String getPrinter(Long companyId, Long warehouseId, ReportType reportType, String findPrinterByValue, String printerName) {
        // if the printer name is passed it, return it
        if (Strings.isNotBlank(printerName)) {
            return printerName;
        }
        // otherwise, get from the configuration
        return reportPrinterConfigurationService.getPrinterName(
                warehouseId, reportType, findPrinterByValue
        );
    }
}
