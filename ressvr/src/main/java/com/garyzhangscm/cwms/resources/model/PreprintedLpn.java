package com.garyzhangscm.cwms.resources.model;
import javax.persistence.*;
@Entity
@Table(name="preprinted_lpn", uniqueConstraints=@UniqueConstraint(columnNames={"warehouse_id","lpn"}))
public class PreprintedLpn {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @Column(name="warehouse_id",nullable=false) private Long warehouseId;
 @Column(nullable=false,length=12) private String lpn;
 @Column(name="print_batch",nullable=false,length=80) private String batch;
 @Column(name="reserved_request_id",length=36) private String reservedRequestId;
 public String getReservedRequestId(){return reservedRequestId;}
 public void setReservedRequestId(String value){reservedRequestId=value;}
 public Long getId(){return id;}
 protected PreprintedLpn() {}
 public PreprintedLpn(Long warehouseId,String lpn,String batch) {this.warehouseId=warehouseId;this.lpn=lpn;this.batch=batch;}
 public String getLpn(){return lpn;}
}
