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

package com.garyzhangscm.cwms.resources.model;


import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.persistence.*;
import java.util.Objects;

/**
 * This class will control whether we will print a report at certain
 * point
 * TO-DO
 */
@Entity
@Table(name = "printing_report_check_point_filter")
public class PrintingReportCheckPointFilterDetail extends AuditibleEntity<String>    {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "printing_report_check_point_filter_id")
    private Long id;

    @Column(name = "printing_report_check_point")
    @Enumerated(EnumType.STRING)
    private PRINTING_REPORT_CHECK_POINT printingReportCheckPoint;

    @Column(name = "field_name")
    private String  fileName;

    @Column(name = "value")
    private String value;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public PRINTING_REPORT_CHECK_POINT getPrintingReportCheckPoint() {
        return printingReportCheckPoint;
    }

    public void setPrintingReportCheckPoint(PRINTING_REPORT_CHECK_POINT printingReportCheckPoint) {
        this.printingReportCheckPoint = printingReportCheckPoint;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
