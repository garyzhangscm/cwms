package com.garyzhangscm.cwms.adminserver.service.datatransfer;

import com.garyzhangscm.cwms.adminserver.model.DataTransferRequest;
import com.garyzhangscm.cwms.adminserver.model.DataTransferRequestTable;
import com.garyzhangscm.cwms.adminserver.model.wms.Company;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public interface DataTransferExportService {


    public void exportData(DataTransferRequest dataTransferRequest) throws IOException;

    public int getSequence();

    public DataTransferRequestTable getTablesName();

    public String getDescription();


}
