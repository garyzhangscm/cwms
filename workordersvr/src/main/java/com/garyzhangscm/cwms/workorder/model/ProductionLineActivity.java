package com.garyzhangscm.cwms.workorder.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import org.codehaus.jackson.annotate.JsonProperty;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

@Entity
@Table(name = "production_line_activity")
public class ProductionLineActivity extends AuditibleEntity<String>{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "production_line_activity_id")
    @JsonProperty(value="id")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "work_order_id")
    private WorkOrder workOrder;

    @ManyToOne
    @JoinColumn(name = "production_line_id")
    private ProductionLine productionLine;

    @Column(name = "warehouse_id")
    private Long warehouseId;



    @Column(name = "username")
    private String username;

    @Transient
    private User user;

    @Column(name = "type")
    @Enumerated(EnumType.STRING)
    private ProductionLineActivityType type;


    @Column(name = "working_team_member_count")
    private Integer workingTeamMemberCount;

    @Column(name = "transaction_time")
    @JsonDeserialize(using = CustomZonedDateTimeDeserializer.class)
    @JsonSerialize(using = CustomZonedDateTimeSerializer.class)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private ZonedDateTime transactionTime;

    // @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    // @JsonSerialize(using = LocalDateTimeSerializer.class)
//@DateTimeFormat(pattern =  "YYYY-MM-DDTHH:mm:ss.SSSZ")
    // @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    // private LocalDateTime transactionTime;

    public ProductionLineActivity(){}

    public ProductionLineActivity(
            Long warehouseId, WorkOrder workOrder,
            ProductionLine productionLine, String username, Integer workingTeamMemberCount,
            ProductionLineActivityType type) {
        this.warehouseId = warehouseId;
        this.workOrder = workOrder;
        this.productionLine = productionLine;
        this.username = username;
        this.workingTeamMemberCount = workingTeamMemberCount;
        this.type = type;
        this.transactionTime = LocalDateTime.now().atZone(ZoneOffset.UTC);

    }
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public WorkOrder getWorkOrder() {
        return workOrder;
    }

    public void setWorkOrder(WorkOrder workOrder) {
        this.workOrder = workOrder;
    }

    public ProductionLine getProductionLine() {
        return productionLine;
    }

    public void setProductionLine(ProductionLine productionLine) {
        this.productionLine = productionLine;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public ProductionLineActivityType getType() {
        return type;
    }

    public void setType(ProductionLineActivityType type) {
        this.type = type;
    }

    public Integer getWorkingTeamMemberCount() {
        return workingTeamMemberCount;
    }

    public void setWorkingTeamMemberCount(Integer workingTeamMemberCount) {
        this.workingTeamMemberCount = workingTeamMemberCount;
    }

    public ZonedDateTime getTransactionTime() {
        return transactionTime;
    }

    public void setTransactionTime(ZonedDateTime transactionTime) {
        this.transactionTime = transactionTime;
    }

    public Long getWarehouseId() {
        return warehouseId;
    }

    public void setWarehouseId(Long warehouseId) {
        this.warehouseId = warehouseId;
    }
}
