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
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.*;

@Entity
@Table(name = "rf_app_version")
public class RFAppVersion extends AuditibleEntity<String>  {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "rf_app_version_id")
    private Long id;

    @Column(name = "version_number")
    private String versionNumber;
    @Column(name = "file_name")
    private String fileName;

    // used to show the progress when downloading the file
    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "is_latest_version")
    private Boolean isLatestVersion;

    @Column(name = "company_id")
    private Long companyId;

    @Column(name = "release_note")
    private String releaseNote;

    @Column(name = "release_date")
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private LocalDateTime releaseDate;


    // if the version is only for certain RF code
    @OneToMany(
            mappedBy = "rfAppVersion",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY)
    private List<RFAppVersionByRFCode> rfAppVersionByRFCodes = new ArrayList<>();

    public boolean isNewerThan(RFAppVersion anotherVersion) {
        return getReleaseDate().isAfter(anotherVersion.getReleaseDate());
    }
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getVersionNumber() {
        return versionNumber;
    }

    public void setVersionNumber(String versionNumber) {
        this.versionNumber = versionNumber;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public Boolean getLatestVersion() {
        return isLatestVersion;
    }

    public void setLatestVersion(Boolean latestVersion) {
        isLatestVersion = latestVersion;
    }

    public Long getCompanyId() {
        return companyId;
    }

    public void setCompanyId(Long companyId) {
        this.companyId = companyId;
    }

    public String getReleaseNote() {
        return releaseNote;
    }

    public void setReleaseNote(String releaseNote) {
        this.releaseNote = releaseNote;
    }

    public LocalDateTime getReleaseDate() {
        return releaseDate;
    }

    public void setReleaseDate(LocalDateTime releaseDate) {
        this.releaseDate = releaseDate;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    public List<RFAppVersionByRFCode> getRfAppVersionByRFCodes() {
        return rfAppVersionByRFCodes;
    }

    public void setRfAppVersionByRFCodes(List<RFAppVersionByRFCode> rfAppVersionByRFCodes) {
        this.rfAppVersionByRFCodes = rfAppVersionByRFCodes;
    }
}
