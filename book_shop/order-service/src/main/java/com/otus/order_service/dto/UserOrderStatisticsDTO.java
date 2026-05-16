package com.otus.order_service.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class UserOrderStatisticsDTO {
    private Long userId;
    private Long totalOrders;
    private Long completedOrders;
    private Long pendingOrders;
    private Long cancelledOrders;
    private BigDecimal totalSpent;
    private BigDecimal averageOrderValue;
}
