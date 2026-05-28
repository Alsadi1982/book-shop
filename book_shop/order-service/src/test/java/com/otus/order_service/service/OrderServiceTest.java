package com.otus.order_service.service;

import com.otus.order_service.dto.*;
import com.otus.order_service.entity.Order;
import com.otus.order_service.entity.OrderItem;
import com.otus.order_service.enums.OrderStatus;
import com.otus.order_service.mapper.OrderMapper;
import com.otus.order_service.repository.OrderRepository;
import com.otus.order_service.repository.OrderItemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @Mock
    private CatalogServiceClient catalogServiceClient;

    @Mock
    private OrderMapper orderMapper;

    @InjectMocks
    private OrderService orderService;

    private OrderRequestDTO orderRequestDTO;
    private Order order;
    private OrderItem orderItem;
    private OrderResponseDTO orderResponseDTO;
    private Map<String, Object> user;
    private Map<String, Object> book;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);

        // Setup user data
        user = new HashMap<>();
        user.put("id", 1L);
        user.put("username", "testuser");
        user.put("email", "test@example.com");

        // Setup book data
        book = new HashMap<>();
        book.put("id", 1L);
        book.put("title", "Test Book");
        book.put("price", "29.99");

        // Setup order item
        orderItem = new OrderItem();
        orderItem.setId(1L);
        orderItem.setOrderId(1L);
        orderItem.setBookId(1L);
        orderItem.setBookTitle("Test Book");
        orderItem.setQuantity(2);
        orderItem.setPrice(new BigDecimal("29.99"));
        orderItem.setSubtotal(new BigDecimal("59.98"));

        // Setup order
        order = new Order();
        order.setId(1L);
        order.setUserId(1L);
        order.setOrderNumber("ORD-12345678");
        order.setTotalAmount(new BigDecimal("59.98"));
        order.setStatus(OrderStatus.PENDING);
        order.setOrderDate(LocalDateTime.now());
        order.setShippingAddress("123 Test St, Test City");
        order.setPaymentMethod("CREDIT_CARD");
        order.setItems(Arrays.asList(orderItem));

        // Setup order request DTO
        orderRequestDTO = new OrderRequestDTO();
        orderRequestDTO.setUserId(1L);
        orderRequestDTO.setShippingAddress("123 Test St, Test City");
        orderRequestDTO.setPaymentMethod("CREDIT_CARD");

        OrderItemRequestDTO itemRequestDTO = new OrderItemRequestDTO();
        itemRequestDTO.setBookId(1L);
        itemRequestDTO.setQuantity(2);
        orderRequestDTO.setItems(Arrays.asList(itemRequestDTO));

        // Setup order response DTO
        orderResponseDTO = new OrderResponseDTO();
        orderResponseDTO.setId(1L);
        orderResponseDTO.setUserId(1L);
        orderResponseDTO.setOrderNumber("ORD-12345678");
        orderResponseDTO.setTotalAmount(new BigDecimal("59.98"));
        orderResponseDTO.setStatus(OrderStatus.PENDING);
        orderResponseDTO.setOrderDate(order.getOrderDate());
        orderResponseDTO.setShippingAddress("123 Test St, Test City");
        orderResponseDTO.setPaymentMethod("CREDIT_CARD");

        OrderItemResponseDTO itemResponseDTO = new OrderItemResponseDTO();
        itemResponseDTO.setId(1L);
        itemResponseDTO.setBookId(1L);
        itemResponseDTO.setBookTitle("Test Book");
        itemResponseDTO.setQuantity(2);
        itemResponseDTO.setPrice(new BigDecimal("29.99"));
        itemResponseDTO.setSubtotal(new BigDecimal("59.98"));
        orderResponseDTO.setItems(Arrays.asList(itemResponseDTO));
    }

    private Order createTestOrder(Long id, String orderNumber, OrderStatus status) {
        Order order = new Order();
        order.setId(id);
        order.setUserId(1L);
        order.setOrderNumber(orderNumber);
        order.setTotalAmount(new BigDecimal("59.98"));
        order.setStatus(status);
        order.setOrderDate(LocalDateTime.now());
        order.setShippingAddress("123 Test St, Test City");
        order.setPaymentMethod("CREDIT_CARD");
        order.setItems(new ArrayList<>());
        return order;
    }

    // Вспомогательный метод для создания OrderResponseDTO
    private OrderResponseDTO createTestResponseDTO(Long id, String orderNumber, OrderStatus status) {
        OrderResponseDTO dto = new OrderResponseDTO();
        dto.setId(id);
        dto.setOrderNumber(orderNumber);
        dto.setTotalAmount(new BigDecimal("59.98"));
        dto.setStatus(status);
        dto.setUserId(1L);
        dto.setOrderDate(LocalDateTime.now());
        dto.setShippingAddress("123 Test St, Test City");
        dto.setPaymentMethod("CREDIT_CARD");
        dto.setItems(new ArrayList<>());
        return dto;
    }

    @Test
    public void getOrder_ExistingId_ReturnsOrder() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderMapper.toResponseDTO(order)).thenReturn(orderResponseDTO);

        OrderResponseDTO result = orderService.getOrder(1L);

        assertNotNull(result);
        assertEquals("ORD-12345678", result.getOrderNumber());
        assertEquals(new BigDecimal("59.98"), result.getTotalAmount());
        verify(orderRepository, times(1)).findById(1L);
        verify(orderMapper, times(1)).toResponseDTO(order);
    }

    @Test
    public void getOrder_NonExistingId_ReturnsNull() {
        when(orderRepository.findById(999L)).thenReturn(Optional.empty());

        OrderResponseDTO result = orderService.getOrder(999L);

        assertNull(result);
        verify(orderRepository, times(1)).findById(999L);
        verify(orderMapper, never()).toResponseDTO(any(Order.class));
    }

    @Test
    public void getOrderByNumber_ExistingNumber_ReturnsOrder() {
        when(orderRepository.findByOrderNumber("ORD-12345678")).thenReturn(Optional.of(order));
        when(orderMapper.toResponseDTO(order)).thenReturn(orderResponseDTO);

        OrderResponseDTO result = orderService.getOrderByNumber("ORD-12345678");

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("123 Test St, Test City", result.getShippingAddress());
        verify(orderRepository, times(1)).findByOrderNumber("ORD-12345678");
        verify(orderMapper, times(1)).toResponseDTO(order);
    }

    @Test
    public void getOrderByNumber_NonExistingNumber_ReturnsNull() {
        when(orderRepository.findByOrderNumber("ORD-NONEXIST")).thenReturn(Optional.empty());

        OrderResponseDTO result = orderService.getOrderByNumber("ORD-NONEXIST");

        assertNull(result);
        verify(orderRepository, times(1)).findByOrderNumber("ORD-NONEXIST");
        verify(orderMapper, never()).toResponseDTO(any(Order.class));
    }

    @Test
    public void getUserOrders_ReturnsUserOrders() {
        when(orderRepository.findByUserId(1L)).thenReturn(Arrays.asList(order));
        when(orderMapper.toSummaryDTOList(Arrays.asList(order))).thenReturn(Arrays.asList(new OrderSummaryDTO()));

        List<OrderSummaryDTO> result = orderService.getUserOrders(1L);

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(orderRepository, times(1)).findByUserId(1L);
        verify(orderMapper, times(1)).toSummaryDTOList(Arrays.asList(order));
    }

    @Test
    public void getOrdersByStatus_ReturnsOrdersByStatus() {
        when(orderRepository.findByStatus(OrderStatus.PENDING)).thenReturn(Arrays.asList(order));
        when(orderMapper.toSummaryDTOList(Arrays.asList(order))).thenReturn(Arrays.asList(new OrderSummaryDTO()));

        List<OrderSummaryDTO> result = orderService.getOrdersByStatus(OrderStatus.PENDING);

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(orderRepository, times(1)).findByStatus(OrderStatus.PENDING);
        verify(orderMapper, times(1)).toSummaryDTOList(Arrays.asList(order));
    }

    @Test
    public void getUserOrdersByStatus_ReturnsUserOrdersByStatus() {
        when(orderRepository.findByUserIdAndStatus(1L, OrderStatus.PENDING)).thenReturn(Arrays.asList(order));
        when(orderMapper.toSummaryDTOList(Arrays.asList(order))).thenReturn(Arrays.asList(new OrderSummaryDTO()));

        List<OrderSummaryDTO> result = orderService.getUserOrdersByStatus(1L, OrderStatus.PENDING);

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(orderRepository, times(1)).findByUserIdAndStatus(1L, OrderStatus.PENDING);
        verify(orderMapper, times(1)).toSummaryDTOList(Arrays.asList(order));
    }

    @Test
    public void createOrder_ValidRequest_CreatesOrder() {
        // Создаем Order, который вернет save()
        Order savedOrder = new Order();
        savedOrder.setId(1L);
        savedOrder.setUserId(1L);
        savedOrder.setOrderNumber("ORD-12345678");
        savedOrder.setTotalAmount(new BigDecimal("59.98"));
        savedOrder.setStatus(OrderStatus.PENDING);
        savedOrder.setOrderDate(LocalDateTime.now());
        savedOrder.setShippingAddress("123 Test St, Test City");
        savedOrder.setPaymentMethod("CREDIT_CARD");
        savedOrder.setItems(new ArrayList<>());

        // Настройка моков с использованием any()
        when(catalogServiceClient.getUser(1L)).thenReturn(user);
        when(catalogServiceClient.getBook(1L)).thenReturn(book);
        when(catalogServiceClient.checkAndReserveStock(1L, 2)).thenReturn(true);
        when(orderRepository.existsByOrderNumber(anyString())).thenReturn(false);
        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);
        when(orderItemRepository.saveAll(anyList())).thenReturn(new ArrayList<>());
        when(orderMapper.toResponseDTO(any(Order.class))).thenReturn(orderResponseDTO);

        // Выполнение
        OrderResponseDTO result = orderService.createOrder(orderRequestDTO);

        // Проверки
        assertNotNull(result);
        assertEquals("ORD-12345678", result.getOrderNumber());
        assertEquals(OrderStatus.PENDING, result.getStatus());

        verify(catalogServiceClient).getUser(1L);
        verify(catalogServiceClient).getBook(1L);
        verify(catalogServiceClient).checkAndReserveStock(1L, 2);
        verify(orderRepository).save(any(Order.class));
        verify(orderMapper).toResponseDTO(any(Order.class));
    }

    @Test
    public void createOrder_UserNotFound_ThrowsException() {
        when(catalogServiceClient.getUser(999L)).thenReturn(null);

        Exception exception = assertThrows(RuntimeException.class, () -> {
            OrderRequestDTO request = new OrderRequestDTO();
            request.setUserId(999L);
            orderService.createOrder(request);
        });

        assertEquals("User not found with id: 999", exception.getMessage());
        verify(catalogServiceClient, times(1)).getUser(999L);
        verify(catalogServiceClient, never()).getBook(anyLong());
        verify(catalogServiceClient, never()).checkAndReserveStock(anyLong(), anyInt());
        verify(orderRepository, never()).save(any(Order.class));
        verify(orderMapper, never()).toResponseDTO(any(Order.class));
    }

    @Test
    public void createOrder_BookNotFound_ThrowsException() {
        when(catalogServiceClient.getUser(1L)).thenReturn(user);
        when(catalogServiceClient.getBook(999L)).thenReturn(null);

        Exception exception = assertThrows(RuntimeException.class, () -> {
            OrderItemRequestDTO item = new OrderItemRequestDTO();
            item.setBookId(999L);
            item.setQuantity(1);
            orderRequestDTO.setItems(Arrays.asList(item));
            orderService.createOrder(orderRequestDTO);
        });

        assertEquals("Book not found: 999", exception.getMessage());
        verify(catalogServiceClient, times(1)).getUser(1L);
        verify(catalogServiceClient, times(1)).getBook(999L);
        verify(catalogServiceClient, never()).checkAndReserveStock(anyLong(), anyInt());
        verify(orderRepository, never()).save(any(Order.class));
        verify(orderMapper, never()).toResponseDTO(any(Order.class));
    }

    @Test
    public void createOrder_InsufficientStock_ThrowsException() {
        // Настройка моков
        when(catalogServiceClient.getUser(1L)).thenReturn(user);
        when(catalogServiceClient.getBook(1L)).thenReturn(book);
        when(catalogServiceClient.checkAndReserveStock(1L, 999)).thenReturn(false);

        // Проверка исключения
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            orderRequestDTO.getItems().get(0).setQuantity(999);
            orderService.createOrder(orderRequestDTO);
        });

        assertEquals("Insufficient stock for book: Test Book", exception.getMessage());

        // Проверяем, что заказ не был сохранен
        verify(orderRepository, never()).save(any(Order.class));
        verify(orderItemRepository, never()).save(any(OrderItem.class));
    }

    @Test
    public void updateOrderStatus_ExistingOrder_UpdatesStatus() {
        Long orderId = 1L;
        OrderStatusUpdateDTO updateDTO = new OrderStatusUpdateDTO();
        updateDTO.setStatus(OrderStatus.PROCESSING);

        Order existingOrder = new Order();
        existingOrder.setId(orderId);  // ID должен быть установлен
        existingOrder.setOrderNumber("ORD-12345678");
        existingOrder.setStatus(OrderStatus.PENDING);
        // ... другие поля

        Order updatedOrder = new Order();
        updatedOrder.setId(orderId);  // ID должен быть установлен
        updatedOrder.setOrderNumber("ORD-12345678");
        updatedOrder.setStatus(OrderStatus.PROCESSING);
        // ... другие поля

        OrderResponseDTO responseDTO = new OrderResponseDTO();
        responseDTO.setId(orderId);
        responseDTO.setOrderNumber("ORD-12345678");
        responseDTO.setStatus(OrderStatus.PROCESSING);

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(existingOrder));
        when(orderRepository.save(any(Order.class))).thenReturn(updatedOrder);
        when(orderMapper.toResponseDTO(any(Order.class))).thenReturn(responseDTO);
        doNothing().when(orderMapper).updateEntity(any(Order.class), any(OrderStatusUpdateDTO.class));

        OrderResponseDTO result = orderService.updateOrderStatus(orderId, updateDTO);

        assertNotNull(result);
        assertEquals(OrderStatus.PROCESSING, result.getStatus());
        verify(orderRepository).findById(orderId);
        verify(orderRepository).save(any(Order.class));
        verify(orderMapper).toResponseDTO(any(Order.class));
    }

    @Test
    public void updateOrderStatus_NonExistingOrder_ReturnsNull() {
        when(orderRepository.findById(999L)).thenReturn(Optional.empty());

        OrderResponseDTO result = orderService.updateOrderStatus(999L, new OrderStatusUpdateDTO());

        assertNull(result);
        verify(orderRepository, times(1)).findById(999L);
        verify(orderRepository, never()).save(any(Order.class));
        verify(orderMapper, never()).toResponseDTO(any(Order.class));
    }

    @Test
    public void updateOrderStatusByNumber_ExistingOrder_UpdatesStatus() {
        String orderNumber = "ORD-12345678";
        OrderStatusUpdateDTO updateDTO = new OrderStatusUpdateDTO();
        updateDTO.setStatus(OrderStatus.PROCESSING);

        Order existingOrder = createTestOrder(1L, orderNumber, OrderStatus.PENDING);
        Order updatedOrder = createTestOrder(1L, orderNumber, OrderStatus.PROCESSING);
        OrderResponseDTO expectedResponse = createTestResponseDTO(1L, orderNumber, OrderStatus.PROCESSING);

        // Выводим информацию для отладки
        System.out.println("=== Debug Info ===");
        System.out.println("existingOrder: " + existingOrder);
        System.out.println("existingOrder.getId(): " + existingOrder.getId());
        System.out.println("existingOrder.getOrderNumber(): " + existingOrder.getOrderNumber());
        System.out.println("existingOrder.getStatus(): " + existingOrder.getStatus());

        // Настройка моков
        when(orderRepository.findByOrderNumber(orderNumber)).thenReturn(Optional.of(existingOrder));
        when(orderRepository.save(any(Order.class))).thenReturn(updatedOrder);
        when(orderMapper.toResponseDTO(any(Order.class))).thenReturn(expectedResponse);

        // Используем doAnswer для updateEntity, чтобы увидеть, что происходит
        doAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            OrderStatusUpdateDTO dto = invocation.getArgument(1);
            System.out.println("updateEntity called with order: " + order);
            System.out.println("updateEntity called with dto: " + dto);
            return null;
        }).when(orderMapper).updateEntity(any(Order.class), any(OrderStatusUpdateDTO.class));

        System.out.println("Calling updateOrderStatusByNumber with orderNumber: " + orderNumber);

        // Выполнение
        OrderResponseDTO result = orderService.updateOrderStatusByNumber(orderNumber, updateDTO);

        System.out.println("Result: " + result);
        System.out.println("=== End Debug ===");

        // Проверки
        assertNotNull(result, "Result should not be null. Check if findByOrderNumber returns the order and save works correctly");
        assertEquals(OrderStatus.PROCESSING, result.getStatus());
        assertEquals(orderNumber, result.getOrderNumber());
    }

    @Test
    public void updateOrderStatusByNumber_NonExistingOrder_ReturnsNull() {
        when(orderRepository.findByOrderNumber("ORD-NONEXIST")).thenReturn(Optional.empty());

        OrderResponseDTO result = orderService.updateOrderStatusByNumber("ORD-NONEXIST", new OrderStatusUpdateDTO());

        assertNull(result);
        verify(orderRepository, times(1)).findByOrderNumber("ORD-NONEXIST");
        verify(orderRepository, never()).save(any(Order.class));
        verify(orderMapper, never()).toResponseDTO(any(Order.class));
    }

    @Test
    public void updateOrder_ExistingOrder_UpdatesOrder() {
        Long orderId = 1L;

        Order existingOrder = createTestOrder(orderId, "ORD-12345678", OrderStatus.PENDING);
        existingOrder.setShippingAddress("Old Address");
        existingOrder.setPaymentMethod("CASH");

        Order updatedOrder = createTestOrder(orderId, "ORD-12345678", OrderStatus.PENDING);
        updatedOrder.setShippingAddress("123 Test St, Test City");
        updatedOrder.setPaymentMethod("CREDIT_CARD");

        OrderResponseDTO expectedResponse = createTestResponseDTO(orderId, "ORD-12345678", OrderStatus.PENDING);

        // Настройка моков
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(existingOrder));
        when(orderRepository.save(any(Order.class))).thenReturn(updatedOrder);
        when(orderMapper.toResponseDTO(any(Order.class))).thenReturn(expectedResponse);

        // Выполнение
        OrderResponseDTO result = orderService.updateOrder(orderId, orderRequestDTO);

        // Проверки
        assertNotNull(result, "Result should not be null");
        assertEquals("ORD-12345678", result.getOrderNumber());
        assertEquals(OrderStatus.PENDING, result.getStatus());

        verify(orderRepository).findById(orderId);
        verify(orderRepository).save(any(Order.class));
        verify(orderMapper).toResponseDTO(any(Order.class));
    }

    @Test
    public void updateOrder_NonExistingOrder_ReturnsNull() {
        when(orderRepository.findById(999L)).thenReturn(Optional.empty());

        OrderResponseDTO result = orderService.updateOrder(999L, orderRequestDTO);

        assertNull(result);
        verify(orderRepository, times(1)).findById(999L);
        verify(orderRepository, never()).save(any(Order.class));
        verify(orderMapper, never()).toResponseDTO(any(Order.class));
    }

    @Test
    public void cancelOrder_ExistingPendingOrder_CancelsOrder() {
        order.setStatus(OrderStatus.PENDING);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(catalogServiceClient.returnStock(1L, 2)).thenReturn(true);

        boolean result = orderService.cancelOrder(1L, "Changed mind");

        assertTrue(result);
        verify(orderRepository, times(1)).findById(1L);
        verify(orderRepository, times(1)).save(order);
        verify(catalogServiceClient, times(1)).returnStock(1L, 2);
    }

    @Test
    public void cancelOrder_NonExistingOrder_ReturnsFalse() {
        when(orderRepository.findById(999L)).thenReturn(Optional.empty());

        boolean result = orderService.cancelOrder(999L, "Test reason");

        assertFalse(result);
        verify(orderRepository, times(1)).findById(999L);
        verify(orderRepository, never()).save(any(Order.class));
        verify(catalogServiceClient, never()).returnStock(anyLong(), anyInt());
    }

    @Test
    public void cancelOrder_NonPendingOrder_ReturnsFalse() {
        order.setStatus(OrderStatus.COMPLETED);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        boolean result = orderService.cancelOrder(1L, "Test reason");

        assertFalse(result);
        verify(orderRepository, times(1)).findById(1L);
        verify(orderRepository, never()).save(any(Order.class));
        verify(orderMapper, never()).toResponseDTO(any(Order.class));
    }
}
