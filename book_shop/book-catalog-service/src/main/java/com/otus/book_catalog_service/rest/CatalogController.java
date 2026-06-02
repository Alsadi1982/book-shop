package com.otus.book_catalog_service.rest;

import com.otus.book_catalog_service.dto.BookRequest;
import com.otus.book_catalog_service.dto.StockUpdateResponseDTO;
import com.otus.book_catalog_service.entity.Book;
import com.otus.book_catalog_service.entity.Category;
import com.otus.book_catalog_service.service.CatalogService;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/catalog")
public class CatalogController {

    @Autowired
    private CatalogService catalogService;

    @GetMapping("/books/{id}")
    @CircuitBreaker(name = "catalogService", fallbackMethod = "getBookFallback")
    @RateLimiter(name = "catalogService")
    @Retry(name = "catalogService")
    public ResponseEntity<Book> getBook(@PathVariable Long id) {
        Book book = catalogService.getBook(id);
        return book != null ? ResponseEntity.ok(book) : ResponseEntity.notFound().build();
    }

    public ResponseEntity<?> getBookFallback(Long id, Throwable t) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of(
                        "error", "Service temporarily unavailable",
                        "reason", t.getMessage()
                ));
    }

    @GetMapping("/books")
    @CircuitBreaker(name = "catalogService", fallbackMethod = "getAllBooksFallback")
    @RateLimiter(name = "catalogService")
    public ResponseEntity<List<Book>> getAllBooks() {
        return ResponseEntity.ok(catalogService.getAllBooks());
    }

    @PostMapping("/books")
    @CircuitBreaker(name = "catalogService", fallbackMethod = "createBookFallback")
    @RateLimiter(name = "catalogService")
    public ResponseEntity<?> createBook(@Valid @RequestBody BookRequest book) {
        try {
            Book savedBook = catalogService.createBook(book);
            return ResponseEntity.status(HttpStatus.CREATED).body(savedBook);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    public ResponseEntity<?> createBookFallback(BookRequest book, Throwable t) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of(
                        "error", "Cannot create book at this moment",
                        "reason", "Service overloaded or unavailable"
                ));
    }

    @PutMapping("/books/{id}/stock")
    @CircuitBreaker(name = "catalogService", fallbackMethod = "updateStockFallback")
    @RateLimiter(name = "catalogService")
    public ResponseEntity<Book> updateStock(@PathVariable Long id, @RequestParam Integer stock) {
        Book book = catalogService.updateBookStock(id, stock);
        return book != null ? ResponseEntity.ok(book) : ResponseEntity.notFound().build();
    }

    public ResponseEntity<?> updateStockFallback(Long id, Integer stock, Throwable t) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of("error", "Cannot update stock at this moment"));
    }

    @PostMapping("/books/{id}/check-stock")
    @CircuitBreaker(name = "catalogService", fallbackMethod = "checkStockFallback")
    @RateLimiter(name = "catalogService")
    public ResponseEntity<Boolean> checkStock(@PathVariable Long id, @RequestParam Integer quantity) {
        boolean available = catalogService.checkAndReduceStock(id, quantity);
        return ResponseEntity.ok(available);
    }

    public ResponseEntity<?> checkStockFallback(Long id, Integer quantity, Throwable t) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of("error", "Cannot check stock at this moment"));
    }

    @PostMapping("/books/{id}/return-stock")
    @CircuitBreaker(name = "catalogService", fallbackMethod = "returnStockFallback")
    @RateLimiter(name = "catalogService")
    public ResponseEntity<StockUpdateResponseDTO> returnStock(
            @PathVariable Long id,
            @RequestParam Integer quantity) {

        try {
            StockUpdateResponseDTO response = catalogService.returnStock(id, quantity);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new StockUpdateResponseDTO(false, e.getMessage(), null));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new StockUpdateResponseDTO(false, e.getMessage(), null));
        }
    }

    public ResponseEntity<?> returnStockFallback(Long id, Integer quantity, Throwable t) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(new StockUpdateResponseDTO(false, "Service temporarily unavailable", null));
    }

    @GetMapping("/categories")
    @CircuitBreaker(name = "catalogService", fallbackMethod = "getAllCategoriesFallback")
    @RateLimiter(name = "catalogService")
    public ResponseEntity<List<Category>> getAllCategories() {
        return ResponseEntity.ok(catalogService.getAllCategories());
    }

    public ResponseEntity<?> getAllCategoriesFallback(Throwable t) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of("error", "Cannot fetch categories at this moment"));
    }

    @GetMapping("/categories/{code}")
    @CircuitBreaker(name = "catalogService", fallbackMethod = "getCategoryFallback")
    @RateLimiter(name = "catalogService")
    public ResponseEntity<Category> getCategory(@PathVariable("code") String code) {
        Category category = catalogService.getCategoryByCode(code);
        return category != null ? ResponseEntity.ok(category) : ResponseEntity.notFound().build();
    }

    public ResponseEntity<?> getCategoryFallback(String code, Throwable t) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of("error", "Cannot fetch category at this moment"));
    }
}
