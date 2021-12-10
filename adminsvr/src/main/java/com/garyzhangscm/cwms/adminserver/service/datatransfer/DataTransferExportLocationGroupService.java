package com.garyzhangscm.cwms.adminserver.service.datatransfer;

import com.garyzhangscm.cwms.adminserver.clients.CommonServiceRestemplateClient;
import com.garyzhangscm.cwms.adminserver.clients.WarehouseLayoutServiceRestemplateClient;
import com.garyzhangscm.cwms.adminserver.model.DataTransferRequest;
import com.garyzhangscm.cwms.adminserver.model.DataTransferRequestTable;
import com.garyzhangscm.cwms.adminserver.model.wms.Company;
import com.garyzhangscm.cwms.adminserver.model.wms.LocationGroup;
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
import java.util.List;
import java.util.Objects;

@Service
public class DataTransferExportLocationGroupService implements DataTransferExportService{

    private static final Logger logger = LoggerFactory.getLogger(DataTransferExportLocationGroupService.class);
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
            List<LocationGroup> locationGroups = warehouseLayoutServiceRestemplateClient.getLocationGroupByWarehouseId(
                    warehouse.getId()
            );
            locationGroups.forEach(
                    locationGroup -> csvData.add(getCSVDataLine(company, warehouse, locationGroup))
            );
        }
        return csvData;
    }

    private Object[] getCSVDataLine(Company company, Warehouse warehouse, LocationGroup locationGroup) {
        // "company,warehouse,name, description, locationGroupType, pickable, storable, countable,trackingVolume, volumeTrackingPolicy,inventoryConsolidationStrategy,allowCartonization,adjustable";

        return new Object[]{
                company.getCode(),
                warehouse.getName(),
                locationGroup.getName(),
                locationGroup.getDescription(),
                locationGroup.getLocationGroupType().getName(),
                locationGroup.getPickable(),
                locationGroup.getStorable(),
                locationGroup.getCountable(),
                locationGroup.getTrackingVolume(),
                locationGroup.getVolumeTrackingPolicy(),
                locationGroup.getInventoryConsolidationStrategy(),
                locationGroup.getAllowCartonization(),
                locationGroup.getAdjustable()
        };
    }


    private String[] getCSVHeader() {
        // "company,warehouse,name, description, locationGroupType, pickable, storable, countable,trackingVolume, volumeTrackingPolicy,inventoryConsolidationStrategy,allowCartonization,adjustable";
        return new String[] {
            "company",
                "warehouse",
                "name",
                "description",
                "locationGroupType",
                "pickable",
                "storable",
                "countable",
                "trackingVolume",
                "volumeTrackingPolicy",
                "inventoryConsolidationStrategy",
                "allowCartonization",
                "adjustable"
        };
    }
    @Override
    public int getSequence() {
        return 10;
    }
    @Override
    public DataTransferRequestTable getTablesName() {
        return DataTransferRequestTable.LOCATION_GROUP;
    }

    @Override
    public String getDescription() {
        return DataTransferRequestTable.LOCATION_GROUP.toString();
    }

    private String getFileName() {
        return getTablesName() + ".csv";
    }
    private String getFilePath(String dataTransferRequestNumber) {
        return dataTransferFolder + "/" + dataTransferRequestNumber + "/" + getFileName();
    }
}
