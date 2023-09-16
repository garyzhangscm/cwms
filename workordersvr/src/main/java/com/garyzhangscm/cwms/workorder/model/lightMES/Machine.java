package com.garyzhangscm.cwms.workorder.model.lightMES;

import com.garyzhangscm.cwms.workorder.model.ProductionLine;
import com.garyzhangscm.cwms.workorder.model.ProductionLineAssignment;
import com.garyzhangscm.cwms.workorder.model.WorkOrder;

import java.util.ArrayList;
import java.util.List;

public class Machine {

    String mid;
    String machineNo;
    String machineName;

    String sim;

    Integer status;

    // 三色灯状态码：001-绿灯，010-黄灯，100-红灯，000-关灯
    String currentState;
    List<MachineStatistics> machineStatistics = new ArrayList<>();

    String machineBrand;
    String machineModel;
    String machineFunction;
    String machineImage;
    String serialNumber;
    String fixedAssetsCode;
    String supplier;
    String manufacturer;
    String productionDate;
    String receiveDate;
    String firstDate;
    String documents;
    String useTime;

    // last hour pulse count
    private int lastHourPulseCount;

    // cycle time in seconds
    private int lastHourCycleTime;

    // last hour pulse count
    private int shiftPulseCount;

    // cycle time in seconds
    private int shiftCycleTime;


    public String getMid() {
        return mid;
    }

    public void setMid(String mid) {
        this.mid = mid;
    }

    public String getMachineNo() {
        return machineNo;
    }

    public void setMachineNo(String machineNo) {
        this.machineNo = machineNo;
    }

    public String getMachineName() {
        return machineName;
    }

    public void setMachineName(String machineName) {
        this.machineName = machineName;
    }

    public String getSim() {
        return sim;
    }

    public void setSim(String sim) {
        this.sim = sim;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getMachineBrand() {
        return machineBrand;
    }

    public void setMachineBrand(String machineBrand) {
        this.machineBrand = machineBrand;
    }

    public String getMachineModel() {
        return machineModel;
    }

    public void setMachineModel(String machineModel) {
        this.machineModel = machineModel;
    }

    public String getMachineFunction() {
        return machineFunction;
    }

    public void setMachineFunction(String machineFunction) {
        this.machineFunction = machineFunction;
    }

    public String getMachineImage() {
        return machineImage;
    }

    public void setMachineImage(String machineImage) {
        this.machineImage = machineImage;
    }

    public String getSerialNumber() {
        return serialNumber;
    }

    public void setSerialNumber(String serialNumber) {
        this.serialNumber = serialNumber;
    }

    public String getFixedAssetsCode() {
        return fixedAssetsCode;
    }

    public void setFixedAssetsCode(String fixedAssetsCode) {
        this.fixedAssetsCode = fixedAssetsCode;
    }

    public String getSupplier() {
        return supplier;
    }

    public void setSupplier(String supplier) {
        this.supplier = supplier;
    }

    public String getManufacturer() {
        return manufacturer;
    }

    public void setManufacturer(String manufacturer) {
        this.manufacturer = manufacturer;
    }

    public String getProductionDate() {
        return productionDate;
    }

    public void setProductionDate(String productionDate) {
        this.productionDate = productionDate;
    }

    public String getReceiveDate() {
        return receiveDate;
    }

    public void setReceiveDate(String receiveDate) {
        this.receiveDate = receiveDate;
    }

    public String getFirstDate() {
        return firstDate;
    }

    public void setFirstDate(String firstDate) {
        this.firstDate = firstDate;
    }

    public String getDocuments() {
        return documents;
    }

    public void setDocuments(String documents) {
        this.documents = documents;
    }

    public String getUseTime() {
        return useTime;
    }

    public void setUseTime(String useTime) {
        this.useTime = useTime;
    }

    public String getCurrentState() {
        return currentState;
    }

    public void setCurrentState(String currentState) {
        this.currentState = currentState;
    }

    public List<MachineStatistics> getMachineStatistics() {
        return machineStatistics;
    }

    public void setMachineStatistics(List<MachineStatistics> machineStatistics) {
        this.machineStatistics = machineStatistics;
    }

    public void addMachineStatistics(MachineStatistics machineStatistics) {
        this.machineStatistics.add(machineStatistics);
    }

    public int getLastHourPulseCount() {
        return lastHourPulseCount;
    }

    public void setLastHourPulseCount(int lastHourPulseCount) {
        this.lastHourPulseCount = lastHourPulseCount;
    }

    public int getLastHourCycleTime() {
        return lastHourCycleTime;
    }

    public void setLastHourCycleTime(int lastHourCycleTime) {
        this.lastHourCycleTime = lastHourCycleTime;
    }

    public int getShiftPulseCount() {
        return shiftPulseCount;
    }

    public void setShiftPulseCount(int shiftPulseCount) {
        this.shiftPulseCount = shiftPulseCount;
    }

    public int getShiftCycleTime() {
        return shiftCycleTime;
    }

    public void setShiftCycleTime(int shiftCycleTime) {
        this.shiftCycleTime = shiftCycleTime;
    }
}
