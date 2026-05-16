package com.otus.book_catalog_service.repository;

import com.otus.book_catalog_service.entity.Book;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface BookRepository extends JpaRepository<Book, Long> {
    Optional<Book> findByIsbn(String isbn);
    List<Book> findByAuthor(String author);
    List<Book> findByStockQuantityLessThan(Integer threshold);
    List<Book> findByCategoryId(Long categoryId);
}
