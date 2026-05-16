package com.otus.book_catalog_service.dto;



import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class BookRequest {

    private String isbn;
    private String title;
    private String author;
    private String description;
    private BigDecimal price;
    private Integer stockQuantity;
    private Long categoryId;
    private Long publisherId;
}
