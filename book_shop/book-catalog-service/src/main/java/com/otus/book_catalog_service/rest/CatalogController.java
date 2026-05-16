package com.otus.book_catalog_service.rest;

import com.otus.book_catalog_service.dto.BookRequest;
import com.otus.book_catalog_service.dto.StockUpdateResponseDTO;
import com.otus.book_catalog_service.entity.Book;
import com.otus.book_catalog_service.entity.Category;
import com.otus.book_catalog_service.service.CatalogService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/catalog")
public class CatalogController {

    @Autowired
    private CatalogService catalogService;

    @GetMapping("/books/{id}")
    public ResponseEntity<Book> getBook(@PathVariable Long id) {
        Book book = catalogService.getBook(id);
        return book != null ? ResponseEntity.ok(book) : ResponseEntity.notFound().build();
    }

    @GetMapping("/books")
    public ResponseEntity<List<Book>> getAllBooks() {
        return ResponseEntity.ok(catalogService.getAllBooks());
    }

    @PostMapping("/books")
    public ResponseEntity<Book> createBook(@Valid @RequestBody BookRequest book) {
        return ResponseEntity.status(HttpStatus.CREATED).body(catalogService.createBook(book));
    }

    @PutMapping("/books/{id}/stock")
    public ResponseEntity<Book> updateStock(@PathVariable Long id, @RequestParam Integer stock) {
        Book book = catalogService.updateBookStock(id, stock);
        return book != null ? ResponseEntity.ok(book) : ResponseEntity.notFound().build();
    }

    @PostMapping("/books/{id}/check-stock")
    public ResponseEntity<Boolean> checkStock(@PathVariable Long id, @RequestParam Integer quantity) {
        boolean available = catalogService.checkAndReduceStock(id, quantity);
        return ResponseEntity.ok(available);
    }

    @PostMapping("/books/{id}/return-stock")
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

    @GetMapping("/categories")
    public ResponseEntity<List<Category>> getAllCategories() {
        return ResponseEntity.ok(catalogService.getAllCategories());
    }

    @GetMapping("/categories/{id}")
    public ResponseEntity<Category> getCategory(@PathVariable Long id) {
        Category category = catalogService.getCategory(id);
        return category != null ? ResponseEntity.ok(category) : ResponseEntity.notFound().build();
    }
}
