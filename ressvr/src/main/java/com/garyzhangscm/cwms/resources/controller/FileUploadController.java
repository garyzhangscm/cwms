package com.garyzhangscm.cwms.resources.controller;

import com.garyzhangscm.cwms.resources.model.FileUploadTemplateColumn;
import com.garyzhangscm.cwms.resources.model.FileUploadType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.util.List;

@RestController
@RequestMapping("/file-upload/types")
public class FileUploadController {


    @RequestMapping(method = RequestMethod.GET)
    public List<FileUploadType> getFileUploadTypes() {
        return FileUploadType.getAvailableFileUploadTypes();
    }
}
