package com.otus.book_catalog_service.dto;



import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class BookRequest {

    @NotBlank(message = "ISBN is required")
    @Size(min = 14, max = 17, message = "ISBN must be between 10 and 17 characters")
    private String isbn;

    @NotBlank(message = "Title is required")
    @Size(min = 1, max = 255, message = "Title must be between 1 and 255 characters")
    private String title;

    @NotBlank(message = "Author is required")
    @Size(min = 1, max = 255, message = "Author must be between 1 and 255 characters")
    private String author;
    private String description;

    @NotNull(message = "Price is required")
    @PositiveOrZero(message = "Price must be zero or positive")
    private BigDecimal price;

    @NotNull(message = "Stock quantity is required")
    @PositiveOrZero(message = "Stock quantity must be zero or positive")
    private Integer stockQuantity;

    @NotNull(message = "Category ID is required")
    @Positive(message = "Category ID must be positive")
    private Long categoryId;
    private Long publisherId;
}
