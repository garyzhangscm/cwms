package com.garyzhangscm.cwms.resources.model;


import javax.persistence.*;

@Entity
@Table(name = "printer")
public class Printer extends AuditibleEntity<String> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "printer_id")
    private Long id;


    @Column(name = "warehouse_id")
    private Long warehouseId;
    @Transient
    private Warehouse warehouse;


    @Column(name = "name")
    private String name;

    @Column(name = "description")
    private String description;

    @ManyToOne
    @JoinColumn(name="printer_type_id")
    private PrinterType printerType;


    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getWarehouseId() {
        return warehouseId;
    }

    public void setWarehouseId(Long warehouseId) {
        this.warehouseId = warehouseId;
    }

    public Warehouse getWarehouse() {
        return warehouse;
    }

    public void setWarehouse(Warehouse warehouse) {
        this.warehouse = warehouse;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public PrinterType getPrinterType() {
        return printerType;
    }

    public void setPrinterType(PrinterType printerType) {
        this.printerType = printerType;
    }
}
