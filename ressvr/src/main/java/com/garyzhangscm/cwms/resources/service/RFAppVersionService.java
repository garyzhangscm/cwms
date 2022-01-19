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

import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import com.garyzhangscm.cwms.resources.clients.LayoutServiceRestemplateClient;
import com.garyzhangscm.cwms.resources.exception.ResourceNotFoundException;
import com.garyzhangscm.cwms.resources.model.*;
import com.garyzhangscm.cwms.resources.repository.RFAppVersionRepository;
import com.garyzhangscm.cwms.resources.repository.RFRepository;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class RFAppVersionService {
    private static final Logger logger = LoggerFactory.getLogger(RFAppVersionService.class);
    @Autowired
    private RFAppVersionRepository rfAppVersionRepository;

    @Autowired
    private FileService fileService;


    @Value("${rf.app.apkTempFolder}")
    private String apkTempFolder;

    @Value("${rf.app.apkFolder}")
    private String apkFolder;


    public RFAppVersion findById(Long id) {
        RFAppVersion rfAppVersion =  rfAppVersionRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.raiseException("rf APP version not found by id: " + id));
        return rfAppVersion;
    }

    public List<RFAppVersion> findAll(Long companyId,
                                     Boolean isLatestVersion,
                                     String versionNumber) {

        return rfAppVersionRepository.findAll(
                (Root<RFAppVersion> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) -> {
                    List<Predicate> predicates = new ArrayList<Predicate>();

                    predicates.add(criteriaBuilder.equal(root.get("companyId"), companyId));

                    if (Objects.nonNull(isLatestVersion)) {

                        predicates.add(criteriaBuilder.equal(root.get("isLatestVersion"), isLatestVersion));
                    }
                    if (!StringUtils.isBlank(versionNumber)) {
                        predicates.add(criteriaBuilder.equal(root.get("versionNumber"), versionNumber));
                    }
                    Predicate[] p = new Predicate[predicates.size()];
                    return criteriaBuilder.and(predicates.toArray(p));
                },
                Sort.by(Sort.Direction.DESC, "releaseDate")
        );



    }

    public RFAppVersion save(RFAppVersion rfAppVersion) {

        return rfAppVersionRepository.save(rfAppVersion);
    }

    public RFAppVersion findByVersionNumber(Long companyId, String versionNumber) {
        return rfAppVersionRepository.findByCompanyIdAndVersionNumber(
                companyId, versionNumber
        );
    }

    public RFAppVersion saveOrUpdate(RFAppVersion rfAppVersion) {
        if (Objects.isNull(rfAppVersion.getId()) &&
                Objects.nonNull(findByVersionNumber(
                        rfAppVersion.getCompanyId(), rfAppVersion.getVersionNumber()))) {
            rfAppVersion.setId(
                    findByVersionNumber(rfAppVersion.getCompanyId(), rfAppVersion.getVersionNumber()).getId());
        }
        return save(rfAppVersion);
    }

    /**
     * Update the lastest RF APP whenever we add a new version or change a existing version
     * @param rfAppVersion
     */
    private void updateLastestRFAppVersion(RFAppVersion rfAppVersion) {

        // check if the rf App version is newer than the original one
        RFAppVersion latestRFAppVersion = rfAppVersionRepository.getLatestRFAppVersion(
                rfAppVersion.getCompanyId()
        );
        // if we don't have any version informaiton yet, or if we are upload
        // the latest version
        if (Objects.isNull(latestRFAppVersion)) {
            rfAppVersion.setLatestVersion(true);
        }
        else if (rfAppVersion.isNewerThan(latestRFAppVersion)) {
            // OK, this should be the new latest version
            rfAppVersion.setLatestVersion(true);
            latestRFAppVersion.setLatestVersion(false);
            saveOrUpdate(latestRFAppVersion);
        }
        else {

            rfAppVersion.setLatestVersion(false);
        }
    }

    public RFAppVersion addRFAppVersion(RFAppVersion rfAppVersion) throws IOException {
        updateLastestRFAppVersion(rfAppVersion);
        String destinationFilePath =
                getAPKFolder(rfAppVersion.getCompanyId()) +
                        rfAppVersion.getVersionNumber() + "/" + rfAppVersion.getFileName();
        fileService.copyFile(
                getAPKTempFolder(rfAppVersion.getCompanyId()) + rfAppVersion.getFileName(),
                destinationFilePath
        );
        // get the file size
        File file = new File(destinationFilePath);
        rfAppVersion.setFileSize(file.length());

        // setup all the RFs for this version
        rfAppVersion.getRfAppVersionByRFCodes().forEach(
                rfAppVersionByRFCode -> rfAppVersionByRFCode.setRfAppVersion(rfAppVersion)
        );
        RFAppVersion newRFAppVersion =  saveOrUpdate(rfAppVersion);
        return newRFAppVersion;

    }

    public RFAppVersion getLatestVersion(Long companyId) {
        return rfAppVersionRepository.getLatestRFAppVersion(companyId);
    }
    public void delete(Long id) {
        rfAppVersionRepository.deleteById(id);


    }

    public void removeRFAppVersion(Long id) {
        RFAppVersion rfAppVersion = findById(id);
        if (Boolean.TRUE.equals(rfAppVersion.getLatestVersion())) {
            // OK, this is the latest app version, we will need to prompt
            // another app version to be the latest app version after we remove this one
            List<RFAppVersion> rfAppVersions = findAll(
                    rfAppVersion.getCompanyId(),
                    false, null
            );
            if (rfAppVersions.size() > 0) {
                Collections.sort(rfAppVersions, (o1, o2) -> o2.getReleaseDate().compareTo(o1.getReleaseDate()));
                // get the latest one from those versions that not marked as isLatestVersion
                RFAppVersion latestRFAppVersion = rfAppVersions.get(0);
                latestRFAppVersion.setLatestVersion(true);
                saveOrUpdate(latestRFAppVersion);
            }
        }
        delete(id);


    }

    public String uploadAPKFile(Long companyId, MultipartFile file) throws IOException {

        String filePath = getAPKTempFolder(companyId);
        logger.debug("Save file to {}{}",
                filePath, file.getOriginalFilename());

        File savedFile =
                fileService.saveFile(
                        file, filePath, file.getOriginalFilename());

        logger.debug("File saved, path: {}",
                savedFile.getAbsolutePath());
        return file.getOriginalFilename();
    }

    private String getAPKTempFolder(Long companyId) {
        return apkTempFolder + "/" + companyId + "/";
    }

    private String getAPKFolder(Long companyId) {
        return apkFolder + "/" + companyId + "/";
    }

    public File getAPKFromTempFolder(Long companyId, String fileName) {

        String fileUrl = getAPKTempFolder(companyId) + fileName;

        logger.debug("Will return {} from apk temp folder to the client",
                fileUrl);
        return new File(fileUrl);
    }

    public File getAPKFile(Long id) {
        RFAppVersion rfAppVersion = findById(id);

        String fileUrl = getAPKFolder(rfAppVersion.getCompanyId()) +
                rfAppVersion.getVersionNumber() + "/" + rfAppVersion.getFileName();

        logger.debug("Will return {} from apk folder to the client",
                fileUrl);
        return new File(fileUrl);
    }

    public File getAPKFile(Long companyId,
                           String versionNumber) {
        RFAppVersion rfAppVersion = findByVersionNumber(companyId, versionNumber);

        String fileUrl = getAPKFolder(rfAppVersion.getCompanyId()) +
                rfAppVersion.getVersionNumber() + "/" + rfAppVersion.getFileName();

        logger.debug("Will return {} from apk folder to the client",
                fileUrl);
        return new File(fileUrl);
    }

    public RFAppVersion getLatestRFAppVersion(Long companyId, String rfCode) {
        List<RFAppVersion> rfAppVersions = findAll(companyId, null, null);

        if (Strings.isNotBlank(rfCode)) {
            // rf code is passed in, let's only return the app version that is not
            // restrained by rf code, or has the specific rf code listed
            rfAppVersions = rfAppVersions.stream().filter(
                    rfAppVersion ->  rfAppVersion.getRfAppVersionByRFCodes().isEmpty() ||
                                rfAppVersion.getRfAppVersionByRFCodes().stream().anyMatch(
                                        rfAppVersionByRFCode -> rfCode.equals(rfAppVersionByRFCode.getRf().getRfCode())
                                )

            ).sorted(Comparator.comparing(RFAppVersion::getReleaseDate).reversed())
                    .collect(Collectors.toList());
        }


        if (rfAppVersions.size() > 0) {
            return rfAppVersions.get(0);
        }
        return null;
    }

    public RFAppVersion changeRFAppVersion(Long id, RFAppVersion rfAppVersion) throws IOException {

        updateLastestRFAppVersion(rfAppVersion);

        String destinationFilePath =
                getAPKFolder(rfAppVersion.getCompanyId()) +
                        rfAppVersion.getVersionNumber() + "/" + rfAppVersion.getFileName();
        fileService.copyFile(
                getAPKTempFolder(rfAppVersion.getCompanyId()) + rfAppVersion.getFileName(),
                destinationFilePath
        );
        // get the file size
        File file = new File(destinationFilePath);
        rfAppVersion.setFileSize(file.length());

        rfAppVersion.getRfAppVersionByRFCodes().forEach(
                rfAppVersionByRFCode -> {
                    rfAppVersionByRFCode.setRfAppVersion(rfAppVersion);
                    // setup the primary key(id) if the record already exists
                    Optional<RFAppVersionByRFCode> rfAppVersionByRFCodeOptional =
                            findRFAppVersionByRFCode(id, rfAppVersionByRFCode.getRf().getId());
                    if (rfAppVersionByRFCodeOptional.isPresent()) {

                        rfAppVersionByRFCode.setId(
                                rfAppVersionByRFCodeOptional.get().getId()
                        );
                    }
                }
        );

        RFAppVersion newRFAppVersion =  saveOrUpdate(rfAppVersion);
        return newRFAppVersion;

    }

    private Optional<RFAppVersionByRFCode> findRFAppVersionByRFCode(Long rfAppVersionId, Long rfId) {
        return findRFAppVersionByRFCode(findById(rfAppVersionId), rfId);
    }
    private Optional<RFAppVersionByRFCode> findRFAppVersionByRFCode(RFAppVersion rfAppVersion, Long rfId) {
        return rfAppVersion.getRfAppVersionByRFCodes().stream().filter(
                rfAppVersionByRFCode -> rfAppVersionByRFCode.getRf().getId().equals(rfId)
        ).findFirst();
    }
}
