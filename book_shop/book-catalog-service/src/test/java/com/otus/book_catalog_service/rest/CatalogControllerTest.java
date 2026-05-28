package com.otus.book_catalog_service.rest;

import com.otus.book_catalog_service.dto.BookRequest;
import com.otus.book_catalog_service.dto.StockUpdateResponseDTO;
import com.otus.book_catalog_service.entity.Book;
import com.otus.book_catalog_service.entity.Category;
import com.otus.book_catalog_service.service.CatalogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CatalogController.class)
class CatalogControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CatalogService catalogService;

    private Book book;
    private Category category;
    private BookRequest bookRequest;

    @BeforeEach
    public void setUp() {
        book = new Book();
        book.setId(1L);
        book.setIsbn("978-0-123456-78-9");
        book.setTitle("Test Book");
        book.setAuthor("Test Author");
        book.setPrice(new BigDecimal("29.99"));
        book.setStockQuantity(10);
        book.setCategoryId(1L);

        category = new Category();
        category.setId(1L);
        category.setCode("FIC");
        category.setName("Fiction");
        category.setDescription("Books with fictional stories");

        bookRequest = new BookRequest();
        bookRequest.setIsbn("978-0-123456-78-9");
        bookRequest.setTitle("Test Book");
        bookRequest.setAuthor("Test Author");
        bookRequest.setPrice(new BigDecimal("29.99"));
        bookRequest.setStockQuantity(10);
        bookRequest.setCategoryId(1L);
    }

    @Test
    public void getBook_ExistingId_ReturnsBook() throws Exception {
        when(catalogService.getBook(1L)).thenReturn(book);

        mockMvc.perform(get("/api/catalog/books/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Test Book"))
                .andExpect(jsonPath("$.author").value("Test Author"))
                .andExpect(jsonPath("$.price").value(29.99))
                .andExpect(jsonPath("$.stockQuantity").value(10));

        verify(catalogService, times(1)).getBook(1L);
    }

    @Test
    public void getBook_NonExistingId_ReturnsNotFound() throws Exception {
        when(catalogService.getBook(999L)).thenReturn(null);

        mockMvc.perform(get("/api/catalog/books/999"))
                .andExpect(status().isNotFound());

        verify(catalogService, times(1)).getBook(999L);
    }

    @Test
    public void getAllBooks_ReturnsAllBooks() throws Exception {
        when(catalogService.getAllBooks()).thenReturn(Arrays.asList(book));

        mockMvc.perform(get("/api/catalog/books"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].title").value("Test Book"));

        verify(catalogService, times(1)).getAllBooks();
    }

    @Test
    public void createBook_ValidRequest_CreatesBook() throws Exception {
        when(catalogService.createBook(any(BookRequest.class))).thenReturn(book);

        mockMvc.perform(post("/api/catalog/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"isbn\":\"978-0-123456-78-9\",\"title\":\"Test Book\",\"author\":\"Test Author\",\"price\":29.99,\"stockQuantity\":10,\"categoryId\":1}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Test Book"));

        verify(catalogService, times(1)).createBook(any(BookRequest.class));
    }

    @Test
    public void createBook_InvalidRequest_ReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/catalog/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"isbn\":\"\",\"title\":\"\",\"author\":\"\",\"price\":-10,\"stockQuantity\":-5,\"categoryId\":-1}"))
                .andExpect(status().isBadRequest());

        verify(catalogService, never()).createBook(any(BookRequest.class));
    }

    @Test
    public void updateStock_ValidRequest_UpdatesStock() throws Exception {
        when(catalogService.updateBookStock(1L, 5)).thenReturn(book);
        book.setStockQuantity(5);

        mockMvc.perform(put("/api/catalog/books/1/stock")
                        .param("stock", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stockQuantity").value(5));

        verify(catalogService, times(1)).updateBookStock(1L, 5);
    }

    @Test
    public void updateStock_NonExistingBook_ReturnsNotFound() throws Exception {
        when(catalogService.updateBookStock(999L, 5)).thenReturn(null);

        mockMvc.perform(put("/api/catalog/books/999/stock")
                        .param("stock", "5"))
                .andExpect(status().isNotFound());

        verify(catalogService, times(1)).updateBookStock(999L, 5);
    }

    @Test
    public void checkStock_SufficientStock_ReturnsTrue() throws Exception {
        when(catalogService.checkAndReduceStock(1L, 5)).thenReturn(true);

        mockMvc.perform(post("/api/catalog/books/1/check-stock")
                        .param("quantity", "5"))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));

        verify(catalogService, times(1)).checkAndReduceStock(1L, 5);
    }

    @Test
    public void checkStock_InsufficientStock_ReturnsFalse() throws Exception {
        when(catalogService.checkAndReduceStock(1L, 15)).thenReturn(false);

        mockMvc.perform(post("/api/catalog/books/1/check-stock")
                        .param("quantity", "15"))
                .andExpect(status().isOk())
                .andExpect(content().string("false"));

        verify(catalogService, times(1)).checkAndReduceStock(1L, 15);
    }

    @Test
    public void returnStock_ValidRequest_ReturnsSuccessResponse() throws Exception {
        StockUpdateResponseDTO response = new StockUpdateResponseDTO(true, "Successfully returned 5 units to stock", 15);
        when(catalogService.returnStock(1L, 5)).thenReturn(response);

        mockMvc.perform(post("/api/catalog/books/1/return-stock")
                        .param("quantity", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Successfully returned 5 units to stock"))
                .andExpect(jsonPath("$.newStockLevel").value(15));

        verify(catalogService, times(1)).returnStock(1L, 5);
    }

    @Test
    public void returnStock_NegativeQuantity_ReturnsBadRequest() throws Exception {
        when(catalogService.returnStock(1L, -5)).thenThrow(new IllegalArgumentException("Quantity must be positive"));

        mockMvc.perform(post("/api/catalog/books/1/return-stock")
                        .param("quantity", "-5"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Quantity must be positive"));

        verify(catalogService, times(1)).returnStock(1L, -5);
    }

    @Test
    public void returnStock_NonExistingBook_ReturnsInternalServerError() throws Exception {
        when(catalogService.returnStock(999L, 5)).thenThrow(new RuntimeException("Book not found with id: 999"));

        mockMvc.perform(post("/api/catalog/books/999/return-stock")
                        .param("quantity", "5"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Book not found with id: 999"));

        verify(catalogService, times(1)).returnStock(999L, 5);
    }

    @Test
    public void getAllCategories_ReturnsAllCategories() throws Exception {
        when(catalogService.getAllCategories()).thenReturn(Arrays.asList(category));

        mockMvc.perform(get("/api/catalog/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Fiction"))
                .andExpect(jsonPath("$[0].code").value("FIC"));

        verify(catalogService, times(1)).getAllCategories();
    }

    @Test
    public void getCategory_ExistingCode_ReturnsCategory() throws Exception {
        when(catalogService.getCategoryByCode("FIC")).thenReturn(category);

        mockMvc.perform(get("/api/catalog/categories/FIC"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Fiction"))
                .andExpect(jsonPath("$.code").value("FIC"))
                .andExpect(jsonPath("$.description").value("Books with fictional stories"));

        verify(catalogService, times(1)).getCategoryByCode("FIC");
    }

    @Test
    public void getCategory_NonExistingCode_ReturnsNotFound() throws Exception {
        when(catalogService.getCategoryByCode("NONEXIST")).thenReturn(null);

        mockMvc.perform(get("/api/catalog/categories/NONEXIST"))
                .andExpect(status().isNotFound());

        verify(catalogService, times(1)).getCategoryByCode("NONEXIST");
    }
}