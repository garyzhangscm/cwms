package com.garyzhangscm.cwms.resources.controller;

import com.garyzhangscm.cwms.resources.ResponseBodyWrapper;
import com.garyzhangscm.cwms.resources.model.BillableEndpoint;
import com.garyzhangscm.cwms.resources.model.FileUploadType;
import com.garyzhangscm.cwms.resources.service.FileUploadService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
public class FileUploadController {


    @Autowired
    private FileUploadService fileUploadService;

    @RequestMapping(value="/file-upload/types", method = RequestMethod.GET)
    public List<FileUploadType> getFileUploadTypes(@RequestParam Long companyId,
                                                   @RequestParam Long warehouseId) {
        return fileUploadService.getFileUploadTypes(companyId, warehouseId);
    }

    @RequestMapping(value="/file-upload/types/{typename}", method = RequestMethod.GET)
    public FileUploadType getFileUploadType(@PathVariable String typename,
                                            @RequestParam Long companyId,
                                            @RequestParam Long warehouseId) {
        return fileUploadService.getFileUploadType(companyId, warehouseId, typename);
    }

    @BillableEndpoint
    @RequestMapping(value="/file-upload/validate-csv-file", method = RequestMethod.POST)
    public ResponseBodyWrapper<String> validateCSVFile(@RequestParam Long companyId,
                                                       @RequestParam Long warehouseId,
                                                       @RequestParam String type,
                                                       @RequestParam String headers) {

        try {
            if (fileUploadService.validateCSVFile(companyId, warehouseId, type, headers)) {
                return ResponseBodyWrapper.success("");
            }
            else {
                return ResponseBodyWrapper.success("validation fail");
            }

        }
        catch(Exception ex) {
            return ResponseBodyWrapper.success(ex.getMessage());
        }
    }
}
