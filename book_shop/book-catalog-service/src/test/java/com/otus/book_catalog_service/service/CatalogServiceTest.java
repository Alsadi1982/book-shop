package com.otus.book_catalog_service.service;

import com.otus.book_catalog_service.dto.BookRequest;
import com.otus.book_catalog_service.dto.StockUpdateResponseDTO;
import com.otus.book_catalog_service.entity.Book;
import com.otus.book_catalog_service.entity.Category;
import com.otus.book_catalog_service.repository.BookRepository;
import com.otus.book_catalog_service.repository.CategoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CatalogServiceTest {

    @Mock
    private BookRepository bookRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private CatalogService catalogService;

    private Book book;
    private BookRequest bookRequest;
    private Category category;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        category = new Category();
        category.setId(1L);
        category.setCode("FIC");
        category.setName("Fiction");

        book = new Book();
        book.setId(1L);
        book.setIsbn("978-0-123456-78-9");
        book.setTitle("Test Book");
        book.setAuthor("Test Author");
        book.setPrice(new BigDecimal("29.99"));
        book.setStockQuantity(10);
        book.setCategoryId(1L);

        bookRequest = new BookRequest();
        bookRequest.setIsbn("978-0-123456-78-9");
        bookRequest.setTitle("Test Book");
        bookRequest.setAuthor("Test Author");
        bookRequest.setPrice(new BigDecimal("29.99"));
        bookRequest.setStockQuantity(10);
        bookRequest.setCategoryId(1L);
    }

    @Test
    public void getBook_ExistingId_ReturnsBook() {
        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));

        Book result = catalogService.getBook(1L);

        assertNotNull(result);
        assertEquals("Test Book", result.getTitle());
        verify(bookRepository, times(1)).findById(1L);
    }

    @Test
    public void getBook_NonExistingId_ReturnsNull() {
        when(bookRepository.findById(999L)).thenReturn(Optional.empty());

        Book result = catalogService.getBook(999L);

        assertNull(result);
        verify(bookRepository, times(1)).findById(999L);
    }

    @Test
    public void getAllBooks_ReturnsAllBooks() {
        when(bookRepository.findAll()).thenReturn(java.util.Arrays.asList(book));

        java.util.List<Book> result = catalogService.getAllBooks();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Test Book", result.get(0).getTitle());
        verify(bookRepository, times(1)).findAll();
    }

    @Test
    public void getCategoryByCode_ExistingCode_ReturnsCategory() {
        when(categoryRepository.findByCode("FIC")).thenReturn(Optional.of(category));

        Category result = catalogService.getCategoryByCode("FIC");

        assertNotNull(result);
        assertEquals("Fiction", result.getName());
        verify(categoryRepository, times(1)).findByCode("FIC");
    }

    @Test
    public void getCategoryByCode_NonExistingCode_ReturnsNull() {
        when(categoryRepository.findByCode("NONEXIST")).thenReturn(Optional.empty());

        Category result = catalogService.getCategoryByCode("NONEXIST");

        assertNull(result);
        verify(categoryRepository, times(1)).findByCode("NONEXIST");
    }

    @Test
    public void getAllCategories_ReturnsAllCategories() {
        when(categoryRepository.findAll()).thenReturn(java.util.Arrays.asList(category));

        java.util.List<Category> result = catalogService.getAllCategories();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Fiction", result.get(0).getName());
        verify(categoryRepository, times(1)).findAll();
    }

    @Test
    public void createBook_ValidRequest_CreatesBook() {
        when(bookRepository.save(any(Book.class))).thenReturn(book);

        Book result = catalogService.createBook(bookRequest);

        assertNotNull(result);
        assertEquals("Test Book", result.getTitle());
        assertEquals(10, result.getStockQuantity());
        verify(bookRepository, times(1)).save(any(Book.class));
    }

    @Test
    public void updateBookStock_ExistingId_UpdatesStock() {
        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));
        when(bookRepository.save(any(Book.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Book result = catalogService.updateBookStock(1L, 5);

        assertNotNull(result);
        assertEquals(5, result.getStockQuantity());
        verify(bookRepository, times(1)).findById(1L);
        verify(bookRepository, times(1)).save(any(Book.class));
    }

    @Test
    public void updateBookStock_NonExistingId_ReturnsNull() {
        when(bookRepository.findById(999L)).thenReturn(Optional.empty());

        Book result = catalogService.updateBookStock(999L, 5);

        assertNull(result);
        verify(bookRepository, times(1)).findById(999L);
        verify(bookRepository, never()).save(any(Book.class));
    }

    @Test
    public void checkAndReduceStock_SufficientStock_ReducesStock() {
        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));
        when(bookRepository.save(any(Book.class))).thenAnswer(invocation -> invocation.getArgument(0));

        boolean result = catalogService.checkAndReduceStock(1L, 5);

        assertTrue(result);
        assertEquals(5, book.getStockQuantity());
        verify(bookRepository, times(1)).findById(1L);
        verify(bookRepository, times(1)).save(any(Book.class));
    }

    @Test
    public void checkAndReduceStock_InsufficientStock_ReturnsFalse() {
        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));

        boolean result = catalogService.checkAndReduceStock(1L, 15);

        assertFalse(result);
        assertEquals(10, book.getStockQuantity());
        verify(bookRepository, times(1)).findById(1L);
        verify(bookRepository, never()).save(any(Book.class));
    }

    @Test
    public void checkAndReduceStock_NonExistingBook_ReturnsFalse() {
        when(bookRepository.findById(999L)).thenReturn(Optional.empty());

        boolean result = catalogService.checkAndReduceStock(999L, 5);

        assertFalse(result);
        verify(bookRepository, times(1)).findById(999L);
        verify(bookRepository, never()).save(any(Book.class));
    }

    @Test
    public void returnStock_ValidRequest_ReturnsSuccessResponse() {
        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));
        when(bookRepository.save(any(Book.class))).thenAnswer(invocation -> invocation.getArgument(0));

        StockUpdateResponseDTO result = catalogService.returnStock(1L, 5);

        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertEquals("Successfully returned 5 units to stock", result.getMessage());
        assertEquals(Integer.valueOf(15), result.getNewStockLevel());
        verify(bookRepository, times(1)).findById(1L);
        verify(bookRepository, times(1)).save(any(Book.class));
    }

    @Test
    public void returnStock_NonExistingBook_ThrowsException() {
        when(bookRepository.findById(999L)).thenReturn(Optional.empty());

        Exception exception = assertThrows(RuntimeException.class, () -> {
            catalogService.returnStock(999L, 5);
        });

        assertEquals("Book not found with id: 999", exception.getMessage());
        verify(bookRepository, times(1)).findById(999L);
        verify(bookRepository, never()).save(any(Book.class));
    }

    @Test
    public void returnStock_NegativeQuantity_ThrowsException() {
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            catalogService.returnStock(1L, -5);
        });

        assertEquals("Quantity must be positive", exception.getMessage());
        verify(bookRepository, never()).findById(anyLong());
        verify(bookRepository, never()).save(any(Book.class));
    }

    @Test
    public void returnStock_ZeroQuantity_ThrowsException() {
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            catalogService.returnStock(1L, 0);
        });

        assertEquals("Quantity must be positive", exception.getMessage());
        verify(bookRepository, never()).findById(anyLong());
        verify(bookRepository, never()).save(any(Book.class));
    }

    @Test
    public void getBookWithManualCache_CacheMiss_LoadsFromRepository() {
        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));

        Book result = catalogService.getBookWithManualCache(1L);

        assertNotNull(result);
        assertEquals("Test Book", result.getTitle());
        verify(bookRepository, times(1)).findById(1L);
    }

    @Test
    public void getBookWithManualCache_CacheHit_ReturnsFromCache() {
        // First call to populate cache
        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));
        catalogService.getBookWithManualCache(1L);

        // Second call should use cache
        Book result = catalogService.getBookWithManualCache(1L);

        assertNotNull(result);
        assertEquals("Test Book", result.getTitle());
        verify(bookRepository, times(1)).findById(1L); // Only called once
    }
}