package com.garyzhangscm.cwms.resources.controller;

import com.garyzhangscm.cwms.resources.ResponseBodyWrapper;
import com.garyzhangscm.cwms.resources.model.BillableEndpoint;
import com.garyzhangscm.cwms.resources.model.RFAppVersion;
import com.garyzhangscm.cwms.resources.model.SiteInformation;
import com.garyzhangscm.cwms.resources.service.RFAppVersionService;
import com.garyzhangscm.cwms.resources.service.SiteInformationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

@RestController
public class RFAppVersionController {
    private static final Logger logger = LoggerFactory.getLogger(RFAppVersionController.class);

    @Autowired
    private RFAppVersionService rfAppVersionService;


    @RequestMapping(value = "/rf-app-versions", method = RequestMethod.GET)
    public List<RFAppVersion> getRFAppVersions(@RequestParam  Long companyId,
                                               @RequestParam(name = "isLatestVersion", required = false, defaultValue = "") Boolean isLatestVersion,
                                               @RequestParam(name = "versionNumber", required = false, defaultValue = "") String versionNumber) {
        return rfAppVersionService.findAll(companyId, isLatestVersion, versionNumber);
    }

    @RequestMapping(value = "/rf-app-versions/{id}", method = RequestMethod.GET)
    public RFAppVersion getRFAppVersion(@PathVariable Long id) {
        return rfAppVersionService.findById(id);
    }


    @RequestMapping(value = "/rf-app-version/latest-version", method = RequestMethod.GET)
    public RFAppVersion getLatestRFAppVersion(@RequestParam Long companyId,
                                               @RequestParam(name = "rfCode", required = false, defaultValue = "") String rfCode) {
        return rfAppVersionService.getLatestRFAppVersion(companyId, rfCode);
    }

    @BillableEndpoint
    @RequestMapping(value = "/rf-app-versions", method = RequestMethod.PUT)
    public RFAppVersion addRFAppVersion(@RequestBody RFAppVersion rfAppVersion) throws IOException {
        return rfAppVersionService.addRFAppVersion(rfAppVersion);
    }

    @BillableEndpoint
    @RequestMapping(value = "/rf-app-versions/{id}", method = RequestMethod.POST)
    public RFAppVersion changeRFAppVersion(
            @PathVariable Long id,
            @RequestBody RFAppVersion rfAppVersion) throws IOException {
        return rfAppVersionService.changeRFAppVersion(id, rfAppVersion);
    }

    @BillableEndpoint
    @RequestMapping(value = "/rf-app-versions/{id}", method = RequestMethod.DELETE)
    public Boolean removeRFAppVersion(@PathVariable Long id) {
        rfAppVersionService.removeRFAppVersion(id);
        return true;
    }

    @BillableEndpoint
    @RequestMapping(method=RequestMethod.POST, value="/rf-app-version/new/apk-files")
    public ResponseBodyWrapper uploadAPKFile(
            @RequestParam Long companyId,
            @RequestParam("file") MultipartFile file) throws IOException {


        String filePath = rfAppVersionService.uploadAPKFile(companyId, file);
        return  ResponseBodyWrapper.success(filePath);
    }

    @RequestMapping(value="/rf-app-version/new/apk-files/{companyId}/{fileName}", method = RequestMethod.GET)
    public ResponseEntity<Resource> getAPKFromTempFolder(@PathVariable Long companyId,
                                                              @PathVariable String fileName) throws FileNotFoundException {

        File apkFile = rfAppVersionService.getAPKFromTempFolder(companyId, fileName);

        InputStreamResource resource
                = new InputStreamResource(new FileInputStream(apkFile));
        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment;fileName=" + fileName)
                .contentLength(apkFile.length())
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);

    }


    @RequestMapping(value = "/rf-apk-files/{id}", method = RequestMethod.GET)
    public ResponseEntity<Resource> getAPKFile(@PathVariable Long id) throws FileNotFoundException {

        File apkFile = rfAppVersionService.getAPKFile(id);

        InputStreamResource resource
                = new InputStreamResource(new FileInputStream(apkFile));
        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment;fileName=" + apkFile.getName())
                .contentLength(apkFile.length())
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);

    }

    @RequestMapping(value = "/rf-apk-files", method = RequestMethod.GET)
    public ResponseEntity<Resource> getAPKFile(@RequestParam Long companyId,
                                               @RequestParam String versionNumber) throws IOException {

        File apkFile = rfAppVersionService.getAPKFile(companyId, versionNumber);

        InputStreamResource resource
                = new InputStreamResource(new FileInputStream(apkFile));

        logger.debug("get apk file {}, file length is {}",
                apkFile.getName(), apkFile.length());
        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment;fileName=" + apkFile.getName())
                .contentLength(apkFile.length())
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);

    }



}
