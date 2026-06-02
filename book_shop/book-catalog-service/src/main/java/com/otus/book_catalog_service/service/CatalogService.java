package com.otus.book_catalog_service.service;

import com.otus.book_catalog_service.dto.BookRequest;
import com.otus.book_catalog_service.dto.StockTransaction;
import com.otus.book_catalog_service.dto.StockUpdateResponseDTO;
import com.otus.book_catalog_service.entity.Book;
import com.otus.book_catalog_service.entity.Category;
import com.otus.book_catalog_service.repository.BookRepository;
import com.otus.book_catalog_service.repository.CategoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Service
public class CatalogService {

    private static final Logger logger = LoggerFactory.getLogger(CatalogService.class);

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    // Ручное кэширование с java.util.concurrent
    private final ConcurrentHashMap<Long, Book> bookCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Category> categoryCache = new ConcurrentHashMap<>();
    private final ReentrantReadWriteLock cacheLock = new ReentrantReadWriteLock();
    private final ConcurrentHashMap<Long, List<StockTransaction>> stockTransactions = new ConcurrentHashMap<>();

    @Cacheable(value = "books", key = "#id")
    public Book getBook(Long id) {
        logger.debug("Fetching book with id: {}", id);
        Book book = bookRepository.findById(id).orElse(null);
        if (book != null) {
            logger.debug("Book found: id={}, title={}", id, book.getTitle());
        }
        return book;
    }

    public List<Book> getAllBooks() {
        logger.info("Fetching all books from database");
        List<Book> books = bookRepository.findAll();
        logger.debug("Retrieved {} books from database", books.size());
        return books;
    }

    @Cacheable(value = "categories", key = "#code")
    public Category getCategoryByCode(String code) {
        logger.debug("Fetching category by code: {}", code);
        Category category = categoryRepository.findByCode(code).orElse(null);
        if (category != null) {
            logger.debug("Category found: code={}, name={}", code, category.getName());
        }
        return category;
    }

    public List<Category> getAllCategories() {
        logger.info("Fetching all categories");
        return categoryRepository.findAll();
    }

    @Transactional
    public Book createBook(BookRequest requestBook) {
        logger.info("Creating new book: title={}", requestBook.getTitle());
        Book book = new Book();
        book.setIsbn(requestBook.getIsbn());
        book.setTitle(requestBook.getTitle());
        book.setAuthor(requestBook.getAuthor());
        book.setDescription(requestBook.getDescription());
        book.setPrice(requestBook.getPrice());
        book.setStockQuantity(requestBook.getStockQuantity());
        book.setCategoryId(requestBook.getCategoryId());
        book.setPublisherId(requestBook.getPublisherId());
        book.setCreatedAt(LocalDateTime.now());
        book.setUpdatedAt(LocalDateTime.now());
        Book saved = bookRepository.save(book);
        logger.info("Book created successfully: id={}, title={}", saved.getId(), saved.getTitle());

        // Обновляем ручной кэш
        cacheLock.writeLock().lock();
        try {
            bookCache.put(saved.getId(), saved);
        } finally {
            cacheLock.writeLock().unlock();
        }

        return saved;
    }

    @CacheEvict(value = "books", key = "#id")
    @Transactional
    public Book updateBookStock(Long id, Integer newStock) {
        logger.info("Updating stock for book: id={}, newStock={}", id, newStock);
        Book book = bookRepository.findById(id).orElse(null);
        if (book == null) {
            logger.warn("Book not found for stock update: id={}", id);
            return null;
        }
        int oldStock = book.getStockQuantity();
        book.setStockQuantity(newStock);
        book.setUpdatedAt(LocalDateTime.now());
        book = bookRepository.save(book);

        logStockTransaction(id, oldStock, newStock, "MANUAL_UPDATE", "Manual stock update");

        // Обновляем ручной кэш
        cacheLock.writeLock().lock();
        try {
            bookCache.put(id, book);
        } finally {
            cacheLock.writeLock().unlock();
        }
        logger.info("Stock updated for book: id={}, oldStock={}, newStock={}", id, oldStock, newStock);

        return book;
    }

    @CacheEvict(value = "books", key = "#id")
    @Transactional
    public boolean checkAndReduceStock(Long id, Integer quantity) {
        logger.debug("Checking stock for book: id={}, quantity={}", id, quantity);
        cacheLock.readLock().lock();
        Book book;
        try {
            book = bookCache.get(id);
            if (book == null) {
                book = bookRepository.findById(id).orElse(null);
            }
        } finally {
            cacheLock.readLock().unlock();
        }

        if (book != null && book.getStockQuantity() >= quantity) {
            int oldStock = book.getStockQuantity();
            int newStock = oldStock - quantity;
            book.setStockQuantity(book.getStockQuantity() - quantity);
            book.setUpdatedAt(LocalDateTime.now());
            bookRepository.save(book);

            logStockTransaction(id, oldStock, newStock, "RESERVE", "Reserved for order");

            cacheLock.writeLock().lock();
            try {
                bookCache.put(id, book);
            } finally {
                cacheLock.writeLock().unlock();
            }
            logger.debug("Stock reserved for book: id={}, quantity={}", id, quantity);
            return true;
        }
        logger.warn("Insufficient stock for book: id={}, available={}, requested={}", 
                id, book != null ? book.getStockQuantity() : "not found", quantity);
        return false;
    }

    @Transactional
    public StockUpdateResponseDTO returnStock(Long id, Integer quantity) {
        logger.info("Returning stock for book: id={}, quantity={}", id, quantity);
        if (quantity == null || quantity <= 0) {
            logger.warn("Invalid quantity for stock return: {}", quantity);
            throw new IllegalArgumentException("Quantity must be positive");
        }

        Book book = bookRepository.findById(id).orElse(null);
        if (book == null) {
            logger.error("Book not found for stock return: id={}", id);
            throw new RuntimeException("Book not found with id: " + id);
        }

        int oldStock = book.getStockQuantity();
        int newStock = oldStock + quantity;

        book.setStockQuantity(newStock);
        book.setUpdatedAt(LocalDateTime.now());
        bookRepository.save(book);

        // Log stock return
        logStockTransaction(id, oldStock, newStock, "RETURN", "Stock returned from cancelled order");

        // Update cache
        cacheLock.writeLock().lock();
        try {
            bookCache.put(id, book);
        } finally {
            cacheLock.writeLock().unlock();
        }

        logger.info("Stock returned for book {}: +%d (old: %d, new: %d)",
                id, quantity, oldStock, newStock);

        return new StockUpdateResponseDTO(true,
                String.format("Successfully returned %d units to stock", quantity),
                newStock);
    }

    private void logStockTransaction(Long bookId, int oldStock, int newStock, String type, String reason) {
        StockTransaction transaction = new StockTransaction();
        transaction.setBookId(bookId);
        transaction.setOldStock(oldStock);
        transaction.setNewStock(newStock);
        transaction.setType(type);
        transaction.setReason(reason);
        transaction.setTimestamp(LocalDateTime.now());

        stockTransactions.computeIfAbsent(bookId, k -> new ArrayList<>()).add(transaction);

        // Keep only last 100 transactions per book to avoid memory issues
        List<StockTransaction> transactions = stockTransactions.get(bookId);
        if (transactions.size() > 100) {
            transactions.remove(0);
        }

        logger.debug("Stock transaction: Book={}, Type={}, Change={} ({}->{})",
                bookId, type, newStock - oldStock, oldStock, newStock);
    }

    // Метод для работы с ручным кэшем
    public Book getBookWithManualCache(Long id) {
        cacheLock.readLock().lock();
        try {
            Book cached = bookCache.get(id);
            if (cached != null) {
                logger.debug("Book retrieved from cache: id={}", id);
                return cached;
            }
        } finally {
            cacheLock.readLock().unlock();
        }

        Book book = bookRepository.findById(id).orElse(null);
        if (book != null) {
            cacheLock.writeLock().lock();
            try {
                bookCache.put(id, book);
                logger.debug("Book cached: id={}", id);
            } finally {
                cacheLock.writeLock().unlock();
            }
        }
        return book;
    }
}
