package com.otus.order_service.repository;

import com.otus.order_service.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    List<OrderItem> findByOrderId(Long orderId);
    List<OrderItem> findByBookId(Long bookId);

    @Query("SELECT oi FROM OrderItem oi WHERE oi.orderId = :orderId")
    List<OrderItem> findAllByOrderId(@Param("orderId") Long orderId);

    @Query("SELECT oi.bookId, SUM(oi.quantity) as totalSold " +
            "FROM OrderItem oi WHERE oi.orderId IN :orderIds " +
            "GROUP BY oi.bookId ORDER BY totalSold DESC")
    List<Object[]> getBestSellingBooks(@Param("orderIds") List<Long> orderIds);

    @Modifying
    @Transactional
    @Query("DELETE FROM OrderItem oi WHERE oi.orderId = :orderId")
    void deleteByOrderId(@Param("orderId") Long orderId);

    @Query("SELECT SUM(oi.quantity) FROM OrderItem oi WHERE oi.bookId = :bookId")
    Integer getTotalQuantitySoldForBook(@Param("bookId") Long bookId);
}
