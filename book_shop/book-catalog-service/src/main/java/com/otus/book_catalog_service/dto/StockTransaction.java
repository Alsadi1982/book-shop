package com.otus.book_catalog_service.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class StockTransaction {

    private Long bookId;
    private int oldStock;
    private int newStock;
    private String type;
    private String reason;
    private LocalDateTime timestamp;
}
