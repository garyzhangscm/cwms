package com.garyzhangscm.cwms.adminserver.service.datatransfer;

import com.garyzhangscm.cwms.adminserver.clients.WarehouseLayoutServiceRestemplateClient;
import com.garyzhangscm.cwms.adminserver.model.DataTransferRequest;
import com.garyzhangscm.cwms.adminserver.model.DataTransferRequestTable;
import com.garyzhangscm.cwms.adminserver.model.wms.Company;
import com.garyzhangscm.cwms.adminserver.model.wms.Warehouse;
import com.garyzhangscm.cwms.adminserver.service.DataTransferRequestService;
import com.garyzhangscm.cwms.adminserver.service.FileService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class DataTransferExportWarehouseService implements DataTransferExportService{

    private static final Logger logger = LoggerFactory.getLogger(DataTransferExportWarehouseService.class);
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
        return warehouses.stream().map(warehouse -> getCSVDataLine(company, warehouse)).collect(Collectors.toList());
    }

    private Object[] getCSVDataLine(Company company, Warehouse warehouse) {
        // "company,name,size,addressCountry,addressState,addressCounty,addressCity,addressDistrict,addressLine1,addressLine2,addressPostcode";

        return new Object[]{
                company.getCode(),
                warehouse.getName(),
                String.valueOf(warehouse.getSize()),
                warehouse.getAddressCountry(),
                warehouse.getAddressState(),
                warehouse.getAddressCounty(),
                warehouse.getAddressCity(),
                warehouse.getAddressDistrict(),
                warehouse.getAddressLine1(),
                warehouse.getAddressLine2(),
                warehouse.getAddressPostcode()
        };
    }

    @Override
    public int getSequence() {
        return 0;
    }

    @Override
    public DataTransferRequestTable getTablesName() {
        return DataTransferRequestTable.WAREHOUSE;
    }

    @Override
    public String getDescription() {
        return DataTransferRequestTable.WAREHOUSE.toString();
    }

    private String[] getCSVHeader() {
        return new String[] {
            "company",
                "name",
                "size",
                "addressCountry",
                "addressState",
                "addressCounty",
                "addressCity",
                "addressDistrict",
                "addressLine1",
                "addressLine2",
                "addressPostcode"
        };
    }
    private String getFileName() {
        return getTablesName() + ".csv";
    }
    private String getFilePath(String dataTransferRequestNumber) {
        return dataTransferFolder + "/" + dataTransferRequestNumber + "/" + getFileName();
    }
}
