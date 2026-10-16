package com.garyzhangscm.cwms.outbound.model;
import javax.persistence.*;
@Entity @Table(name="manual_pick_operation")
public class ManualPickOperation {
 @Id @Column(name="request_id",length=36) public String requestId;
 @Column(name="warehouse_id",nullable=false) public Long warehouseId;
 @Column(name="order_id",nullable=false) public Long orderId;
 @Column(name="source_inventory_id",nullable=false) public Long sourceInventoryId;
 @Column(name="source_lpn",nullable=false,length=12) public String sourceLpn;
 @Column(name="new_lpn",length=12) public String newLpn;
 @Column(name="keep_original",nullable=false) public boolean keepOriginal;
 @Column(nullable=false) public Long quantity;
 @Column(name="source_quantity",nullable=false) public Long sourceQuantity;
 @Column(name="destination_id",nullable=false) public Long destinationId;
 @Column(name="pick_id") public Long pickId;
 @Column(nullable=false) public boolean completed;
 @Column(nullable=false) public boolean cancelled;
}
