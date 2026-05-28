package com.otus.order_service.repository;

import com.otus.order_service.entity.Order;
import com.otus.order_service.enums.OrderStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
@org.springframework.test.context.jdbc.Sql(scripts = "classpath:schema.sql", executionPhase = org.springframework.test.context.jdbc.Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class OrderRepositoryTest {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private TestEntityManager entityManager;

    private Order order;

    @BeforeEach
    public void setUp() {
        order = new Order();
        order.setUserId(1L);
        order.setOrderNumber("ORD-12345678");
        order.setTotalAmount(new BigDecimal("59.98"));
        order.setStatus(OrderStatus.PENDING);
        order.setOrderDate(LocalDateTime.now().minusDays(1));
        order.setShippingAddress("123 Test St, Test City");
        order.setPaymentMethod("CREDIT_CARD");
    }

    @Test
    public void findByOrderNumber_ExistingNumber_ReturnsOrder() {
        // Given
        orderRepository.save(order);

        // When
        Optional<Order> result = orderRepository.findByOrderNumber("ORD-12345678");

        // Then
        assertTrue(result.isPresent());
        assertEquals(1L, result.get().getUserId());
        assertEquals(OrderStatus.PENDING, result.get().getStatus());
    }

    @Test
    public void findByOrderNumber_NonExistingNumber_ReturnsEmpty() {
        // When
        Optional<Order> result = orderRepository.findByOrderNumber("ORD-NONEXIST");

        // Then
        assertFalse(result.isPresent());
    }

    @Test
    public void findByUserId_ExistingUser_ReturnsOrders() {
        // Given
        orderRepository.save(order);

        // When
        List<Order> result = orderRepository.findByUserId(1L);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("ORD-12345678", result.get(0).getOrderNumber());
    }

    @Test
    public void findByUserId_NonExistingUser_ReturnsEmptyList() {
        // When
        List<Order> result = orderRepository.findByUserId(999L);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void findByStatus_ExistingStatus_ReturnsOrders() {
        // Given
        orderRepository.save(order);

        // When
        List<Order> result = orderRepository.findByStatus(OrderStatus.PENDING);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("ORD-12345678", result.get(0).getOrderNumber());
    }

    @Test
    public void findByStatus_NonExistingStatus_ReturnsEmptyList() {
        // Given
        orderRepository.save(order);

        // When
        List<Order> result = orderRepository.findByStatus(OrderStatus.COMPLETED);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void findByUserIdAndStatus_ExistingUserAndStatus_ReturnsOrders() {
        // Given
        orderRepository.save(order);

        // When
        List<Order> result = orderRepository.findByUserIdAndStatus(1L, OrderStatus.PENDING);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("ORD-12345678", result.get(0).getOrderNumber());
    }

    @Test
    public void findByUserIdAndStatus_NonExistingUser_ReturnsEmptyList() {
        // When
        List<Order> result = orderRepository.findByUserIdAndStatus(999L, OrderStatus.PENDING);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void findByUserIdAndStatus_NonExistingStatus_ReturnsEmptyList() {
        // Given
        orderRepository.save(order);

        // When
        List<Order> result = orderRepository.findByUserIdAndStatus(1L, OrderStatus.COMPLETED);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void findByOrderDateBetween_ValidRange_ReturnsOrders() {
        // Given
        orderRepository.save(order);
        LocalDateTime startDate = order.getOrderDate().minusHours(1);
        LocalDateTime endDate = order.getOrderDate().plusHours(1);

        // When
        List<Order> result = orderRepository.findByOrderDateBetween(startDate, endDate);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("ORD-12345678", result.get(0).getOrderNumber());
    }

    @Test
    public void findByOrderDateBetween_OutOfRange_ReturnsEmptyList() {
        // Given
        orderRepository.save(order);
        LocalDateTime startDate = order.getOrderDate().plusDays(1);
        LocalDateTime endDate = order.getOrderDate().plusDays(2);

        // When
        List<Order> result = orderRepository.findByOrderDateBetween(startDate, endDate);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void findByOrderDateAfter_FutureDate_ReturnsEmptyList() {
        // Given
        orderRepository.save(order);
        LocalDateTime date = order.getOrderDate().plusDays(1);

        // When
        List<Order> result = orderRepository.findByOrderDateAfter(date);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void findByOrderDateAfter_PastDate_ReturnsOrders() {
        // Given
        orderRepository.save(order);
        LocalDateTime date = order.getOrderDate().minusDays(2);

        // When
        List<Order> result = orderRepository.findByOrderDateAfter(date);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("ORD-12345678", result.get(0).getOrderNumber());
    }

    @Test
    public void findByOrderDateBefore_FutureDate_ReturnsOrders() {
        // Given
        orderRepository.save(order);
        LocalDateTime date = order.getOrderDate().plusDays(1);

        // When
        List<Order> result = orderRepository.findByOrderDateBefore(date);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("ORD-12345678", result.get(0).getOrderNumber());
    }

    @Test
    public void findByOrderDateBefore_PastDate_ReturnsEmptyList() {
        // Given
        orderRepository.save(order);
        LocalDateTime date = order.getOrderDate().minusDays(2);

        // When
        List<Order> result = orderRepository.findByOrderDateBefore(date);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void findUserOrdersAfterDate_ValidDate_ReturnsOrders() {
        // Given
        orderRepository.save(order);
        LocalDateTime startDate = order.getOrderDate().minusHours(1);

        // When
        List<Order> result = orderRepository.findUserOrdersAfterDate(1L, startDate);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("ORD-12345678", result.get(0).getOrderNumber());
    }

    @Test
    public void findUserOrdersAfterDate_FutureDate_ReturnsEmptyList() {
        // Given
        orderRepository.save(order);
        LocalDateTime startDate = order.getOrderDate().plusDays(1);

        // When
        List<Order> result = orderRepository.findUserOrdersAfterDate(1L, startDate);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void findOrdersByStatusAndOlderThan_ValidStatusAndDate_ReturnsOrders() {
        // Given
        orderRepository.save(order);
        LocalDateTime date = order.getOrderDate().plusHours(1);

        // When
        List<Order> result = orderRepository.findOrdersByStatusAndOlderThan(OrderStatus.PENDING, date);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("ORD-12345678", result.get(0).getOrderNumber());
    }

    @Test
    public void findOrdersByStatusAndOlderThan_DifferentStatus_ReturnsEmptyList() {
        // Given
        orderRepository.save(order);
        LocalDateTime date = order.getOrderDate().plusHours(1);

        // When
        List<Order> result = orderRepository.findOrdersByStatusAndOlderThan(OrderStatus.COMPLETED, date);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void countOrdersByUser_ExistingUser_ReturnsCount() {
        // Given
        orderRepository.save(order);

        // When
        Long count = orderRepository.countOrdersByUser(1L);

        // Then
        assertEquals(1L, count);
    }

    @Test
    public void countOrdersByUser_NonExistingUser_ReturnsZero() {
        // When
        Long count = orderRepository.countOrdersByUser(999L);

        // Then
        assertEquals(0L, count);
    }

    @Test
    public void getTotalSpentByUser_ExistingUser_ReturnsTotal() {
        // Given
        order.setStatus(OrderStatus.COMPLETED);
        orderRepository.save(order);

        // When
        Double totalSpent = orderRepository.getTotalSpentByUser(1L);

        // Then
        assertEquals(59.98, totalSpent, 0.01);
    }

    @Test
    public void getTotalSpentByUser_NonExistingUser_ReturnsNull() {
        // When
        Double totalSpent = orderRepository.getTotalSpentByUser(999L);

        // Then
        assertNull(totalSpent);
    }

    @Test
    public void getOrderStatusStatistics_ReturnsStatistics() {
        // Given
        orderRepository.save(order);

        // When
        List<Object[]> result = orderRepository.getOrderStatusStatistics();

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        Object[] stats = result.get(0);
        assertEquals(OrderStatus.PENDING, stats[0]);
        assertEquals(1L, stats[1]);
    }

    @Test
    public void updateOrderStatus_ExistingOrder_UpdatesStatus() {
        // Given
        Order savedOrder = orderRepository.save(order);
        orderRepository.flush();
        entityManager.clear();

        // When
        int updatedRows = orderRepository.updateOrderStatus(savedOrder.getId(), OrderStatus.PROCESSING);

        // Then
        assertEquals(1, updatedRows);
        Optional<Order> updatedOrder = orderRepository.findById(savedOrder.getId());
        assertTrue(updatedOrder.isPresent());
        assertEquals(OrderStatus.PROCESSING, updatedOrder.get().getStatus());
    }

    @Test
    public void updateOrderStatusByNumber_ExistingOrder_UpdatesStatus() {
        // Given
        orderRepository.save(order);
        orderRepository.flush();
        entityManager.clear();

        // When
        int updatedRows = orderRepository.updateOrderStatusByNumber("ORD-12345678", OrderStatus.PROCESSING);

        // Then
        assertEquals(1, updatedRows);
        Optional<Order> updatedOrder = orderRepository.findByOrderNumber("ORD-12345678");
        assertTrue(updatedOrder.isPresent());
        assertEquals(OrderStatus.PROCESSING, updatedOrder.get().getStatus());
    }

    @Test
    public void findUserOrdersWithLimit_ValidRequest_ReturnsLimitedOrders() {
        // Given
        orderRepository.save(order);

        // When
        List<Order> result = orderRepository.findUserOrdersWithLimit(1L, "PENDING", 10);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("ORD-12345678", result.get(0).getOrderNumber());
    }

    @Test
    public void existsByOrderNumber_ExistingNumber_ReturnsTrue() {
        // Given
        orderRepository.save(order);

        // When
        boolean exists = orderRepository.existsByOrderNumber("ORD-12345678");

        // Then
        assertTrue(exists);
    }

    @Test
    public void existsByOrderNumber_NonExistingNumber_ReturnsFalse() {
        // When
        boolean exists = orderRepository.existsByOrderNumber("ORD-NONEXIST");

        // Then
        assertFalse(exists);
    }

    @Test
    public void existsByUserIdAndStatus_ExistingUserAndStatus_ReturnsTrue() {
        // Given
        orderRepository.save(order);

        // When
        boolean exists = orderRepository.existsByUserIdAndStatus(1L, OrderStatus.PENDING);

        // Then
        assertTrue(exists);
    }

    @Test
    public void existsByUserIdAndStatus_NonExistingUser_ReturnsFalse() {
        // When
        boolean exists = orderRepository.existsByUserIdAndStatus(999L, OrderStatus.PENDING);

        // Then
        assertFalse(exists);
    }

    @Test
    public void deleteCancelledOrdersOlderThan_ValidDate_DeletesOrders() {
        // Given
        order.setStatus(OrderStatus.CANCELLED);
        order.setOrderDate(LocalDateTime.now().minusDays(10));
        Order savedOrder = orderRepository.save(order);
        orderRepository.flush();
        entityManager.clear();

        // When
        int deletedRows = orderRepository.deleteCancelledOrdersOlderThan(LocalDateTime.now().minusDays(5));


        // Then
        assertEquals(1, deletedRows);
        assertFalse(orderRepository.findById(savedOrder.getId()).isPresent());
    }

    @Test
    public void deleteCancelledOrdersOlderThan_NoMatchingOrders_ReturnsZero() {
        // Given
        orderRepository.save(order);

        // When
        int deletedRows = orderRepository.deleteCancelledOrdersOlderThan(LocalDateTime.now().plusDays(1));


        // Then
        assertEquals(0, deletedRows);
        assertTrue(orderRepository.findById(order.getId()).isPresent());
    }
}