package com.garyzhangscm.cwms.adminserver.service.datatransfer;

import com.garyzhangscm.cwms.adminserver.clients.CommonServiceRestemplateClient;
import com.garyzhangscm.cwms.adminserver.clients.WarehouseLayoutServiceRestemplateClient;
import com.garyzhangscm.cwms.adminserver.model.DataTransferRequest;
import com.garyzhangscm.cwms.adminserver.model.DataTransferRequestTable;
import com.garyzhangscm.cwms.adminserver.model.wms.Company;
import com.garyzhangscm.cwms.adminserver.model.wms.Location;
import com.garyzhangscm.cwms.adminserver.model.wms.LocationGroup;
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
import java.util.Objects;

@Service
public class DataTransferExportLocationService implements DataTransferExportService{

    private static final Logger logger = LoggerFactory.getLogger(DataTransferExportLocationService.class);
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
            List<Location> locations = warehouseLayoutServiceRestemplateClient.getLocationsByWarehouseId(
                    warehouse.getId()
            );
            locations.forEach(
                    location -> csvData.add(getCSVDataLine(company, warehouse, location))
            );
        }
        return csvData;
    }

    private Object[] getCSVDataLine(Company company, Warehouse warehouse, Location location) {

        // "company,warehouse,name,aisle,x,y,z,length,width,height,pickSequence,putawaySequence,countSequence,capacity,fillPercentage,locationGroup,enabled";
        return new Object[]{
                company.getCode(),
                warehouse.getName(),
                location.getName(),
                location.getAisle(),
                location.getX(),
                location.getY(),
                location.getZ(),
                location.getLength(),
                location.getWidth(),
                location.getHeight(),
                location.getPickSequence(),
                location.getPutawaySequence(),
                location.getCountSequence(),
                location.getCapacity(),
                location.getFillPercentage(),
                location.getLocationGroup().getName(),
                location.getEnabled()
        };
    }


    private String[] getCSVHeader() {
        // "company,warehouse,name,aisle,x,y,z,length,width,height,pickSequence,putawaySequence,countSequence,capacity,fillPercentage,locationGroup,enabled";
        return new String[] {
            "company",
                "warehouse",
                "name",
                "aisle",
                "x",
                "y",
                "z",
                "length",
                "width",
                "height",
                "pickSequence",
                "putawaySequence",
                "countSequence",
                "capacity",
                "fillPercentage",
                "locationGroup",
                "enabled"
        };
    }
    @Override
    public int getSequence() {
        return 11;
    }
    @Override
    public DataTransferRequestTable getTablesName() {
        return DataTransferRequestTable.LOCATION;
    }

    @Override
    public String getDescription() {
        return DataTransferRequestTable.LOCATION.toString();
    }

    private String getFileName() {
        return getTablesName() + ".csv";
    }
    private String getFilePath(String dataTransferRequestNumber) {
        return dataTransferFolder + "/" + dataTransferRequestNumber + "/" + getFileName();
    }
}
