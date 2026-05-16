package com.otus.book_catalog_service.service;

import com.otus.book_catalog_service.dto.BookRequest;
import com.otus.book_catalog_service.dto.StockTransaction;
import com.otus.book_catalog_service.dto.StockUpdateResponseDTO;
import com.otus.book_catalog_service.entity.Book;
import com.otus.book_catalog_service.entity.Category;
import com.otus.book_catalog_service.repository.BookRepository;
import com.otus.book_catalog_service.repository.CategoryRepository;
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
import java.util.logging.Logger;

@Service
public class CatalogService {

    private static final Logger logger = Logger.getLogger(CatalogService.class.getName());

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
        return bookRepository.findById(id).orElse(null);
    }

    public List<Book> getAllBooks() {
        return bookRepository.findAll();
    }

    @Cacheable(value = "categories", key = "#code")
    public Category getCategoryByCode(String code) {
        return categoryRepository.findByCode(code).orElse(null);
    }

    public Category getCategory(Long id) {
        return categoryRepository.findById(id).orElse(null);
    }

    public List<Category> getAllCategories() {
        return categoryRepository.findAll();
    }

    @CacheEvict(value = "books", key = "#book.id")
    @Transactional
    public Book createBook(BookRequest requestBook) {
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
        Book book = bookRepository.findById(id).orElse(null);
        if (book != null) {
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
        }
        return book;
    }

    @CacheEvict(value = "books", key = "#id")
    @Transactional
    public boolean checkAndReduceStock(Long id, Integer quantity) {
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
            return true;
        }
        return false;
    }

    @Transactional
    public StockUpdateResponseDTO returnStock(Long id, Integer quantity) {
        if (quantity == null || quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }

        Book book = bookRepository.findById(id).orElse(null);
        if (book == null) {
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

        logger.info(String.format("Stock returned for book %d: +%d (old: %d, new: %d)",
                id, quantity, oldStock, newStock));

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

        logger.info(String.format("Stock transaction: Book=%d, Type=%s, Change=%d (%d->%d), Reason=%s",
                bookId, type, newStock - oldStock, oldStock, newStock, reason));
    }

    // Метод для работы с ручным кэшем
    public Book getBookWithManualCache(Long id) {
        cacheLock.readLock().lock();
        try {
            Book cached = bookCache.get(id);
            if (cached != null) {
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
            } finally {
                cacheLock.writeLock().unlock();
            }
        }
        return book;
    }
}
