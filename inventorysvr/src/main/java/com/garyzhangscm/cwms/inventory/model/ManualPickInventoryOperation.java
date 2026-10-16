package com.garyzhangscm.cwms.inventory.model;
import jakarta.persistence.*;
@Entity @Table(name="manual_pick_inventory_operation",uniqueConstraints=@UniqueConstraint(columnNames={"warehouse_id","new_lpn"}))
public class ManualPickInventoryOperation {
 @Id @Column(name="request_id",length=36) public String requestId;
 @Column(name="warehouse_id",nullable=false) public Long warehouseId;
 @Column(nullable=false,length=64) public String fingerprint;
 @Column(name="new_lpn",length=12) public String newLpn;
 @Column(name="picked_inventory_id",nullable=false) public Long pickedInventoryId;
 @Column(name="picked_lpn",nullable=false,length=12) public String pickedLpn;
 @Column(nullable=false) public Long quantity;
}
