package com.garyzhangscm.cwms.resources.controller;

import com.garyzhangscm.cwms.resources.ResponseBodyWrapper;
import com.garyzhangscm.cwms.resources.model.BillableEndpoint;
import com.garyzhangscm.cwms.resources.model.FileUploadType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
public class FileUploadController {


    @RequestMapping(value="/file-upload/types", method = RequestMethod.GET)
    public List<FileUploadType> getFileUploadTypes() {
        return FileUploadType.getAvailableFileUploadTypes();
    }


    @BillableEndpoint
    @RequestMapping(value="/file-upload/validate-csv-file", method = RequestMethod.POST)
    public ResponseBodyWrapper<String> validateCSVFile(@RequestParam Long warehouseId,
                                                       @RequestParam String type,
                                                       @RequestParam String headers) {

        return ResponseBodyWrapper.success(
                FileUploadType.validateCSVFile(type, headers)
        );
    }
}
