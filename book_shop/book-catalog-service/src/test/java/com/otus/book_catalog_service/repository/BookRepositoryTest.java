package com.otus.book_catalog_service.repository;

import com.otus.book_catalog_service.entity.Book;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
@org.springframework.test.context.jdbc.Sql(scripts = "classpath:schema.sql", executionPhase = org.springframework.test.context.jdbc.Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class BookRepositoryTest {

    @Autowired
    private BookRepository bookRepository;

    private Book book;

    @BeforeEach
    public void setUp() {
        book = new Book();
        book.setIsbn("978-0-123456-78-9");
        book.setTitle("Test Book");
        book.setAuthor("Test Author");
        book.setPrice(new BigDecimal("29.99"));
        book.setStockQuantity(10);
        book.setCategoryId(1L);
    }

    @Test
    public void findByIsbn_ExistingIsbn_ReturnsBook() {
        // Given
        bookRepository.save(book);

        // When
        Optional<Book> result = bookRepository.findByIsbn("978-0-123456-78-9");

        // Then
        assertTrue(result.isPresent());
        assertEquals("Test Book", result.get().getTitle());
    }

    @Test
    public void findByIsbn_NonExistingIsbn_ReturnsEmpty() {
        // When
        Optional<Book> result = bookRepository.findByIsbn("978-0-000000-00-0");

        // Then
        assertFalse(result.isPresent());
    }

    @Test
    public void findByAuthor_ExistingAuthor_ReturnsBooks() {
        // Given
        bookRepository.save(book);

        // When
        List<Book> result = bookRepository.findByAuthor("Test Author");

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Test Book", result.get(0).getTitle());
    }

    @Test
    public void findByAuthor_NonExistingAuthor_ReturnsEmptyList() {
        // When
        List<Book> result = bookRepository.findByAuthor("Nonexistent Author");

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void findByStockQuantityLessThan_Threshold_ReturnsBooks() {
        // Given
        book.setStockQuantity(5);
        bookRepository.save(book);

        // When
        List<Book> result = bookRepository.findByStockQuantityLessThan(10);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Test Book", result.get(0).getTitle());
    }

    @Test
    public void findByStockQuantityLessThan_NoBooksBelowThreshold_ReturnsEmptyList() {
        // Given
        book.setStockQuantity(15);
        bookRepository.save(book);

        // When
        List<Book> result = bookRepository.findByStockQuantityLessThan(10);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void findByCategoryId_ExistingCategoryId_ReturnsBooks() {
        // Given
        bookRepository.save(book);

        // When
        List<Book> result = bookRepository.findByCategoryId(1L);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Test Book", result.get(0).getTitle());
    }

    @Test
    public void findByCategoryId_NonExistingCategoryId_ReturnsEmptyList() {
        // When
        List<Book> result = bookRepository.findByCategoryId(999L);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void save_Book_PersistsToDatabase() {
        // When
        Book savedBook = bookRepository.save(book);

        // Then
        assertNotNull(savedBook.getId());
        assertEquals("Test Book", savedBook.getTitle());
        assertTrue(bookRepository.findById(savedBook.getId()).isPresent());
    }

    @Test
    public void delete_Book_RemovesFromDatabase() {
        // Given
        Book savedBook = bookRepository.save(book);

        // When
        bookRepository.deleteById(savedBook.getId());

        // Then
        assertFalse(bookRepository.findById(savedBook.getId()).isPresent());
    }

    @Test
    public void existsById_ExistingId_ReturnsTrue() {
        // Given
        Book savedBook = bookRepository.save(book);

        // When
        boolean exists = bookRepository.existsById(savedBook.getId());

        // Then
        assertTrue(exists);
    }

    @Test
    public void existsById_NonExistingId_ReturnsFalse() {
        // When
        boolean exists = bookRepository.existsById(999L);

        // Then
        assertFalse(exists);
    }
}