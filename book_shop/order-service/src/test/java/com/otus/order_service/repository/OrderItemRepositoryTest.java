package com.otus.order_service.repository;

import com.otus.order_service.entity.Order;
import com.otus.order_service.entity.OrderItem;
import com.otus.order_service.enums.OrderStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
class OrderItemRepositoryTest {

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private OrderRepository orderRepository; // Добавляем репозиторий для Order

    private OrderItem orderItem;
    private Order savedOrder; // Сохраненный заказ для использования в тестах

    @BeforeEach
    public void setUp() {
        // Создаем и сохраняем Order
        Order order = new Order();
        order.setOrderNumber("ORD-" + UUID.randomUUID().toString().substring(0, 8));
        order.setUserId(1L);
        order.setStatus(OrderStatus.PENDING); // или другой подходящий статус
        order.setOrderDate(LocalDateTime.now());
        order.setTotalAmount(new BigDecimal("59.98"));
        // Установите другие обязательные поля вашего Order
        savedOrder = orderRepository.save(order);

        // Создаем OrderItem с реальным orderId
        orderItem = new OrderItem();
        orderItem.setOrderId(savedOrder.getId()); // Используем ID сохраненного заказа
        orderItem.setBookId(1L);
        orderItem.setBookTitle("Test Book");
        orderItem.setQuantity(2);
        orderItem.setPrice(new BigDecimal("29.99"));
        orderItem.setSubtotal(new BigDecimal("59.98"));
    }

    @Test
    public void findByOrderId_ExistingOrder_ReturnsItems() {
        // Given
        orderItemRepository.save(orderItem);

        // When
        List<OrderItem> result = orderItemRepository.findByOrderId(savedOrder.getId());

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Test Book", result.get(0).getBookTitle());
        assertEquals(2, result.get(0).getQuantity());
        assertEquals(new BigDecimal("59.98"), result.get(0).getSubtotal());
    }

    @Test
    public void findByOrderId_NonExistingOrder_ReturnsEmptyList() {
        // When
        List<OrderItem> result = orderItemRepository.findByOrderId(999L);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void findByBookId_ExistingBook_ReturnsItems() {
        // Given
        orderItemRepository.save(orderItem);

        // When
        List<OrderItem> result = orderItemRepository.findByBookId(1L);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(savedOrder.getId(), result.get(0).getOrderId());
        assertEquals(2, result.get(0).getQuantity());
    }

    @Test
    public void findByBookId_NonExistingBook_ReturnsEmptyList() {
        // When
        List<OrderItem> result = orderItemRepository.findByBookId(999L);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void findAllByOrderId_ExistingOrder_ReturnsItems() {
        // Given
        orderItemRepository.save(orderItem);

        // When
        List<OrderItem> result = orderItemRepository.findAllByOrderId(savedOrder.getId());

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Test Book", result.get(0).getBookTitle());
    }

    @Test
    public void findAllByOrderId_NonExistingOrder_ReturnsEmptyList() {
        // When
        List<OrderItem> result = orderItemRepository.findAllByOrderId(999L);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void getBestSellingBooks_ValidOrders_ReturnsBooks() {
        // Given
        orderItemRepository.save(orderItem);

        // When
        List<Object[]> result = orderItemRepository.getBestSellingBooks(List.of(savedOrder.getId()));

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        Object[] bestSeller = result.get(0);
        assertEquals(1L, bestSeller[0]); // bookId
        assertEquals(2L, bestSeller[1]); // totalSold
    }

    @Test
    public void getBestSellingBooks_NoOrders_ReturnsEmptyList() {
        // When
        List<Object[]> result = orderItemRepository.getBestSellingBooks(List.of());

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void deleteByOrderId_ExistingOrder_DeletesItems() {
        // Given
        orderItemRepository.save(orderItem);

        // When
        orderItemRepository.deleteByOrderId(savedOrder.getId());

        // Then
        List<OrderItem> remaining = orderItemRepository.findByOrderId(savedOrder.getId());
        assertTrue(remaining.isEmpty());
    }

    @Test
    public void deleteByOrderId_NonExistingOrder_DoesNothing() {
        // When
        orderItemRepository.deleteByOrderId(999L);

        // Then
        // No exception should be thrown
        List<OrderItem> allItems = orderItemRepository.findAll();
        assertTrue(allItems.isEmpty());
    }

    @Test
    public void getTotalQuantitySoldForBook_ExistingBook_ReturnsQuantity() {
        // Given
        orderItemRepository.save(orderItem);

        // When
        Integer totalSold = orderItemRepository.getTotalQuantitySoldForBook(1L);

        // Then
        assertNotNull(totalSold);
        assertEquals(2, totalSold);
    }

    @Test
    public void getTotalQuantitySoldForBook_NonExistingBook_ReturnsNull() {
        // When
        Integer totalSold = orderItemRepository.getTotalQuantitySoldForBook(999L);

        // Then
        assertNull(totalSold);
    }

    @Test
    public void save_OrderItem_PersistsToDatabase() {
        // When
        OrderItem savedItem = orderItemRepository.save(orderItem);

        // Then
        assertNotNull(savedItem.getId());
        assertEquals(savedOrder.getId(), savedItem.getOrderId());
        assertEquals(1L, savedItem.getBookId());
        assertTrue(orderItemRepository.findById(savedItem.getId()).isPresent());
    }

    @Test
    public void delete_OrderItem_RemovesFromDatabase() {
        // Given
        OrderItem savedItem = orderItemRepository.save(orderItem);

        // When
        orderItemRepository.deleteById(savedItem.getId());

        // Then
        assertFalse(orderItemRepository.findById(savedItem.getId()).isPresent());
    }

    @Test
    public void existsById_ExistingId_ReturnsTrue() {
        // Given
        OrderItem savedItem = orderItemRepository.save(orderItem);

        // When
        boolean exists = orderItemRepository.existsById(savedItem.getId());

        // Then
        assertTrue(exists);
    }

    @Test
    public void existsById_NonExistingId_ReturnsFalse() {
        // When
        boolean exists = orderItemRepository.existsById(999L);

        // Then
        assertFalse(exists);
    }
}