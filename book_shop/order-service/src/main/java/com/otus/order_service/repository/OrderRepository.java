package com.otus.order_service.repository;

import com.otus.order_service.entity.Order;
import com.otus.order_service.enums.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    // Basic queries
    Optional<Order> findByOrderNumber(String orderNumber);
    List<Order> findByUserId(Long userId);
    List<Order> findByStatus(OrderStatus status);
    List<Order> findByUserIdAndStatus(Long userId, OrderStatus status);

    // Date range queries
    List<Order> findByOrderDateBetween(LocalDateTime startDate, LocalDateTime endDate);
    List<Order> findByOrderDateAfter(LocalDateTime date);
    List<Order> findByOrderDateBefore(LocalDateTime date);

    // Complex queries with JPQL
    @Query("SELECT o FROM Order o WHERE o.userId = :userId AND o.orderDate >= :startDate ORDER BY o.orderDate DESC")
    List<Order> findUserOrdersAfterDate(@Param("userId") Long userId, @Param("startDate") LocalDateTime startDate);

    @Query("SELECT o FROM Order o WHERE o.status = :status AND o.orderDate < :date")
    List<Order> findOrdersByStatusAndOlderThan(@Param("status") OrderStatus status, @Param("date") LocalDateTime date);

    // Aggregate queries
    @Query("SELECT COUNT(o) FROM Order o WHERE o.userId = :userId")
    Long countOrdersByUser(@Param("userId") Long userId);

    @Query("SELECT SUM(o.totalAmount) FROM Order o WHERE o.userId = :userId AND o.status = 'COMPLETED'")
    Double getTotalSpentByUser(@Param("userId") Long userId);

    @Query("SELECT o.status, COUNT(o) FROM Order o GROUP BY o.status")
    List<Object[]> getOrderStatusStatistics();

    // Update queries
    @Modifying
    @Transactional
    @Query("UPDATE Order o SET o.status = :status WHERE o.id = :orderId")
    int updateOrderStatus(@Param("orderId") Long orderId, @Param("status") OrderStatus status);

    @Modifying
    @Transactional
    @Query("UPDATE Order o SET o.status = :status WHERE o.orderNumber = :orderNumber")
    int updateOrderStatusByNumber(@Param("orderNumber") String orderNumber, @Param("status") OrderStatus status);

    // Native queries
    @Query(value = "SELECT * FROM orders WHERE user_id = :userId AND status = :status LIMIT :limit", nativeQuery = true)
    List<Order> findUserOrdersWithLimit(@Param("userId") Long userId, @Param("status") String status, @Param("limit") int limit);

    @Query(value = "SELECT DATE(order_date) as order_day, COUNT(*) as order_count, SUM(total_amount) as daily_total " +
            "FROM orders WHERE order_date >= CURRENT_DATE - INTERVAL '7 days' " +
            "GROUP BY DATE(order_date) ORDER BY order_day DESC", nativeQuery = true)
    List<Object[]> getWeeklyOrderStatistics();

    // Existence checks
    boolean existsByOrderNumber(String orderNumber);
    boolean existsByUserIdAndStatus(Long userId, OrderStatus status);

    // Bulk operations
    @Modifying
    @Transactional
    @Query("DELETE FROM Order o WHERE o.status = 'CANCELLED' AND o.orderDate < :date")
    int deleteCancelledOrdersOlderThan(@Param("date") LocalDateTime date);
}
