package com.garyzhangscm.cwms.adminserver.service.datatransfer;

import com.garyzhangscm.cwms.adminserver.clients.CommonServiceRestemplateClient;
import com.garyzhangscm.cwms.adminserver.clients.WarehouseLayoutServiceRestemplateClient;
import com.garyzhangscm.cwms.adminserver.model.DataTransferRequest;
import com.garyzhangscm.cwms.adminserver.model.DataTransferRequestTable;
import com.garyzhangscm.cwms.adminserver.model.wms.Company;
import com.garyzhangscm.cwms.adminserver.model.wms.Policy;
import com.garyzhangscm.cwms.adminserver.model.wms.Warehouse;
import com.garyzhangscm.cwms.adminserver.service.FileService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class DataTransferExportPolicyService implements DataTransferExportService{

    private static final Logger logger = LoggerFactory.getLogger(DataTransferExportPolicyService.class);
    @Autowired
    private CommonServiceRestemplateClient commonServiceRestemplateClient;
    @Autowired
    private WarehouseLayoutServiceRestemplateClient warehouseLayoutServiceRestemplateClient;

    @Value("${admin.dataTransfer.folder}")
    private String dataTransferFolder;

    @Autowired
    private FileService fileService;

    @Override
    public void exportData(DataTransferRequest dataTransferRequest) throws IOException {


        logger.debug("start to write {} into file {}",
                getTablesName(), getFilePath(dataTransferRequest.getNumber()));
        // logger.debug("CSV HEADER: \n{}", getCSVHeader());
        // logger.debug("CSV content: \n {}", getCSVData(dataTransferRequest.getCompany()));

        fileService.createCSVFile(
                getFilePath(dataTransferRequest.getNumber()),
                getCSVHeader(),
                getCSVData(dataTransferRequest.getCompany())

        );

    }

    private List<Object[]> getCSVData(Company company) {
        List<Warehouse> warehouses = warehouseLayoutServiceRestemplateClient.getWarehouseByCompany(company.getId());
        List<Object[]> csvData = new ArrayList<>();
        for(Warehouse warehouse : warehouses) {
            List<Policy> policies = commonServiceRestemplateClient.getPoliciesByWarehouseId(
                    warehouse.getId()
            );
            policies.forEach(
                    policy -> csvData.add(getCSVDataLine(company, warehouse, policy))
            );
        }
        return csvData;
    }

    private Object[] getCSVDataLine(Company company, Warehouse warehouse, Policy policy) {
        // "company,warehouse,key,value,description";

        return new Object[]{
                company.getCode(),
                warehouse.getName(),
                policy.getKey(),
                policy.getValue(),
                policy.getDescription()
        };
    }

    @Override
    public int getSequence() {
        return 1;
    }

    @Override
    public DataTransferRequestTable getTablesName() {
        return DataTransferRequestTable.POLICY;
    }

    @Override
    public String getDescription() {
        return DataTransferRequestTable.POLICY.toString();
    }

    private String[] getCSVHeader() {
        return new String[] {
            "company",
                "warehouse",
                "key",
                "value",
                "description"
        };
    }
    private String getFileName() {
        return getTablesName() + ".csv";
    }
    private String getFilePath(String dataTransferRequestNumber) {
        return dataTransferFolder + "/" + dataTransferRequestNumber + "/" + getFileName();
    }
}
