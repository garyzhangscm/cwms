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

package com.garyzhangscm.cwms.resources;


import com.garyzhangscm.cwms.resources.model.Printer;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@ConfigurationProperties(prefix="site.printers")
public class PrinterConfiguration   {

    private String url;

    private Boolean testPrintersOnly;

    private List<Printer> testPrinters;

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public Boolean getTestPrintersOnly() {
        return testPrintersOnly;
    }

    public void setTestPrintersOnly(Boolean testPrintersOnly) {
        this.testPrintersOnly = testPrintersOnly;
    }

    public List<Printer> getTestPrinters() {
        return testPrinters;
    }

    public void setTestPrinters(List<Printer> testPrinters) {
        this.testPrinters = testPrinters;
    }
}