package com.otus.book_catalog_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class StockUpdateResponseDTO {
    private boolean success;
    private String message;
    private Integer newStockLevel;
}
