package com.otus.order_service.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class DailyOrderStatsDTO {
    private LocalDateTime date;
    private Long orderCount;
    private BigDecimal dailyTotal;
}
