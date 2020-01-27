package com.garyzhangscm.cwms.outbound.model;

import org.springframework.security.core.context.SecurityContextHolder;

import java.io.Serializable;
import java.time.LocalDateTime;

public class OrderActivity  implements Serializable {
    private LocalDateTime activityDateTime;
    private String username;

    private Order order = null;

    private OrderActivityType orderActivityType;

    public OrderActivity(){};

    public static OrderActivity build() {

    return new OrderActivity()
            .withUsername(SecurityContextHolder.getContext().getAuthentication().getName())
            .withActivityDateTime(LocalDateTime.now());
    }

    public OrderActivity withActivityDateTime(LocalDateTime activityDateTime) {
        setActivityDateTime(activityDateTime);
        return this;
    }
    public OrderActivity withUsername(String username) {
        setUsername(username);
        return this;
    }
    public OrderActivity withOrder(Order order) {
        setOrder(order);
        return this;
    }
    public OrderActivity withOrderActivityType(OrderActivityType orderActivityType) {
        setOrderActivityType(orderActivityType);
        return this;
    }
    public LocalDateTime getActivityDateTime() {
        return activityDateTime;
    }

    public void setActivityDateTime(LocalDateTime activityDateTime) {
        this.activityDateTime = activityDateTime;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public Order getOrder() {
        return order;
    }

    public void setOrder(Order order) {
        this.order = order;
    }

    public OrderActivityType getOrderActivityType() {
        return orderActivityType;
    }

    public void setOrderActivityType(OrderActivityType orderActivityType) {
        this.orderActivityType = orderActivityType;
    }

}
