package com.garyzhangscm.cwms.workorder.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.codehaus.jackson.annotate.JsonProperty;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "work_order_assignment")
public class WorkOrderAssignment extends AuditibleEntity<String> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "work_order_assignment_id")
    @JsonProperty(value="id")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "work_order_id")
    @JsonIgnore
    private WorkOrder workOrder;

    @Column(name = "user_id")
    private Long userId;

    @Transient
    private User user;

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

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }
}
