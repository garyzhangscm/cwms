package com.garyzhangscm.cwms.adminserver.service.datatransfer;

import com.garyzhangscm.cwms.adminserver.clients.CommonServiceRestemplateClient;
import com.garyzhangscm.cwms.adminserver.clients.WarehouseLayoutServiceRestemplateClient;
import com.garyzhangscm.cwms.adminserver.model.DataTransferRequest;
import com.garyzhangscm.cwms.adminserver.model.DataTransferRequestTable;
import com.garyzhangscm.cwms.adminserver.model.wms.Company;
import com.garyzhangscm.cwms.adminserver.model.wms.Policy;
import com.garyzhangscm.cwms.adminserver.model.wms.SystemControlledNumber;
import com.garyzhangscm.cwms.adminserver.model.wms.Warehouse;
import com.garyzhangscm.cwms.adminserver.service.FileService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

@Service
public class DataTransferExportSystemControlledNumberService implements DataTransferExportService{

    private static final Logger logger = LoggerFactory.getLogger(DataTransferExportSystemControlledNumberService.class);
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
            List<SystemControlledNumber> systemControlledNumbers = commonServiceRestemplateClient.getSystemControlledNumberByWarehouseId(
                    warehouse.getId()
            );
            systemControlledNumbers.forEach(
                    systemControlledNumber -> csvData.add(getCSVDataLine(company, warehouse, systemControlledNumber))
            );
        }
        logger.debug("will write data into system controlled number");
        logger.debug("=============   CSV   Data   ==============");
        csvData.forEach(
                csvLine -> logger.debug(Arrays.deepToString(csvLine))
        );
        return csvData;
    }

    private Object[] getCSVDataLine(Company company, Warehouse warehouse, SystemControlledNumber systemControlledNumber) {
        // "company,warehouse,variable,prefix,postfix,length,currentNumber,rollover";

        return new Object[]{
                company.getCode(),
                warehouse.getName(),
                systemControlledNumber.getVariable(),
                systemControlledNumber.getPrefix(),
                systemControlledNumber.getPostfix(),
                systemControlledNumber.getLength(),
                systemControlledNumber.getCurrentNumber(),
                String.valueOf(systemControlledNumber.getRollover()).toLowerCase()
        };
    }

    @Override
    public int getSequence() {
        return 2;
    }

    @Override
    public DataTransferRequestTable getTablesName() {
        return DataTransferRequestTable.SYSTEM_CONTROLLED_NUMBER;
    }

    @Override
    public String getDescription() {
        return DataTransferRequestTable.SYSTEM_CONTROLLED_NUMBER.toString();
    }

    private String[] getCSVHeader() {
        // "company,warehouse,variable,prefix,postfix,length,currentNumber,rollover";
        return new String[] {
            "company",
                "warehouse",
                "variable",
                "prefix",
                "postfix",
                "length",
                "currentNumber",
                "rollover"
        };
    }
    private String getFileName() {
        return getTablesName() + ".csv";
    }
    private String getFilePath(String dataTransferRequestNumber) {
        return dataTransferFolder + "/" + dataTransferRequestNumber + "/" + getFileName();
    }
}
