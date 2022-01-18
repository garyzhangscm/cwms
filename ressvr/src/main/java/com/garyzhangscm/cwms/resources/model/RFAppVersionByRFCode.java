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

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.TreeSet;

@Entity
@Table(name = "rf_app_version_by_rf_code")
public class RFAppVersionByRFCode extends AuditibleEntity<String>  {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "rf_app_version_by_rf_code_id")
    private Long id;

    @ManyToOne
    @JoinColumn(name="rf_id")
    private RF rf;

    @ManyToOne
    @JoinColumn(name = "rf_app_version_id")
    @JsonIgnore
    private RFAppVersion rfAppVersion;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public RF getRf() {
        return rf;
    }

    public void setRf(RF rf) {
        this.rf = rf;
    }

    public RFAppVersion getRfAppVersion() {
        return rfAppVersion;
    }

    public void setRfAppVersion(RFAppVersion rfAppVersion) {
        this.rfAppVersion = rfAppVersion;
    }
}
