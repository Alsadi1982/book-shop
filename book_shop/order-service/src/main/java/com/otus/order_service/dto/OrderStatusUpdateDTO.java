package com.otus.order_service.dto;

import com.otus.order_service.enums.OrderStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class OrderStatusUpdateDTO {
    @NotNull(message = "Status is required")
    private OrderStatus status;

    private String cancellationReason;
}
