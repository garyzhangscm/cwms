package com.garyzhangscm.cwms.workorder.model.lightMES;


import com.garyzhangscm.cwms.workorder.model.ProductionLine;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Machine {

    String mid;
    String machineNo;
    String machineName;

    String sim;

    Integer status;

    // 三色灯状态码：001-绿灯，010-黄灯，100-红灯，000-关灯
    String currentState;
    List<MachineStatistics> machineStatistics = new ArrayList<>();

    String productionLineTypeName;

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

    // last time window pulse count
    private int lastTimeWindowPulseCount;

    // cycle time in seconds
    private int lastTimeWindowCycleTime;

    // last hour pulse count
    private int shiftPulseCount;

    // cycle time in seconds
    private int shiftCycleTime;


    public Machine(){}
    public Machine(ProductionLine productionLine) {
        this.machineNo = productionLine.getName();
        this.productionLineTypeName = Objects.isNull(productionLine) ? "" : productionLine.getType().getName();
    }
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

    public int getLastTimeWindowPulseCount() {
        return lastTimeWindowPulseCount;
    }

    public void setLastTimeWindowPulseCount(int lastTimeWindowPulseCount) {
        this.lastTimeWindowPulseCount = lastTimeWindowPulseCount;
    }

    public int getLastTimeWindowCycleTime() {
        return lastTimeWindowCycleTime;
    }

    public void setLastTimeWindowCycleTime(int lastTimeWindowCycleTime) {
        this.lastTimeWindowCycleTime = lastTimeWindowCycleTime;
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

    public String getProductionLineTypeName() {
        return productionLineTypeName;
    }

    public void setProductionLineTypeName(String productionLineTypeName) {
        this.productionLineTypeName = productionLineTypeName;
    }
}
