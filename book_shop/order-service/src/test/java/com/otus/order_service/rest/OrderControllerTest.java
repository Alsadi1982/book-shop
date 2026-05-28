package com.otus.order_service.rest;

import com.otus.order_service.dto.*;
import com.otus.order_service.enums.OrderStatus;
import com.otus.order_service.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(OrderController.class)
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OrderService orderService;

    private OrderRequestDTO orderRequestDTO;
    private OrderResponseDTO orderResponseDTO;
    private OrderSummaryDTO orderSummaryDTO;
    private OrderStatusUpdateDTO orderStatusUpdateDTO;

    @BeforeEach
    public void setUp() {
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
        orderResponseDTO.setOrderDate(LocalDateTime.now());
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

        // Setup order summary DTO
        orderSummaryDTO = new OrderSummaryDTO();
        orderSummaryDTO.setId(1L);
        orderSummaryDTO.setOrderNumber("ORD-12345678");
        orderSummaryDTO.setTotalAmount(new BigDecimal("59.98"));
        orderSummaryDTO.setStatus(OrderStatus.PENDING);
        orderSummaryDTO.setOrderDate(LocalDateTime.now());
        orderSummaryDTO.setItemCount(1);

        // Setup order status update DTO
        orderStatusUpdateDTO = new OrderStatusUpdateDTO();
        orderStatusUpdateDTO.setStatus(OrderStatus.PROCESSING);
    }

    @Test
    public void createOrder_ValidRequest_CreatesOrder() throws Exception {
        when(orderService.createOrder(any(OrderRequestDTO.class))).thenReturn(orderResponseDTO);

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":1,\"shippingAddress\":\"123 Test St, Test City\",\"paymentMethod\":\"CREDIT_CARD\",\"items\":[{\"bookId\":1,\"quantity\":2}]}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orderNumber").value("ORD-12345678"))
                .andExpect(jsonPath("$.totalAmount").value(59.98))
                .andExpect(jsonPath("$.status").value("PENDING"));

        verify(orderService, times(1)).createOrder(any(OrderRequestDTO.class));
    }

    @Test
    public void createOrder_InvalidRequest_ReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":-1,\"shippingAddress\":\"\",\"paymentMethod\":\"\",\"items\":[]}"))
                .andExpect(status().isBadRequest());

        verify(orderService, never()).createOrder(any(OrderRequestDTO.class));
    }

    @Test
    public void getOrder_ExistingId_ReturnsOrder() throws Exception {
        when(orderService.getOrder(1L)).thenReturn(orderResponseDTO);

        mockMvc.perform(get("/api/orders/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderNumber").value("ORD-12345678"))
                .andExpect(jsonPath("$.totalAmount").value(59.98))
                .andExpect(jsonPath("$.status").value("PENDING"));

        verify(orderService, times(1)).getOrder(1L);
    }

    @Test
    public void getOrder_NonExistingId_ReturnsNotFound() throws Exception {
        when(orderService.getOrder(999L)).thenReturn(null);

        mockMvc.perform(get("/api/orders/999"))
                .andExpect(status().isNotFound());

        verify(orderService, times(1)).getOrder(999L);
    }

    @Test
    public void getOrderByNumber_ExistingNumber_ReturnsOrder() throws Exception {
        when(orderService.getOrderByNumber("ORD-12345678")).thenReturn(orderResponseDTO);

        mockMvc.perform(get("/api/orders/number/ORD-12345678"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.shippingAddress").value("123 Test St, Test City"));

        verify(orderService, times(1)).getOrderByNumber("ORD-12345678");
    }

    @Test
    public void getOrderByNumber_NonExistingNumber_ReturnsNotFound() throws Exception {
        when(orderService.getOrderByNumber("ORD-NONEXIST")).thenReturn(null);

        mockMvc.perform(get("/api/orders/number/ORD-NONEXIST"))
                .andExpect(status().isNotFound());

        verify(orderService, times(1)).getOrderByNumber("ORD-NONEXIST");
    }

    @Test
    public void getUserOrders_ReturnsUserOrders() throws Exception {
        when(orderService.getUserOrders(1L)).thenReturn(Arrays.asList(orderSummaryDTO));

        mockMvc.perform(get("/api/orders/user/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].orderNumber").value("ORD-12345678"))
                .andExpect(jsonPath("$[0].totalAmount").value(59.98));

        verify(orderService, times(1)).getUserOrders(1L);
    }

    @Test
    public void getOrdersByStatus_ReturnsOrdersByStatus() throws Exception {
        when(orderService.getOrdersByStatus(OrderStatus.PENDING)).thenReturn(Arrays.asList(orderSummaryDTO));

        mockMvc.perform(get("/api/orders/status/PENDING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].status").value("PENDING"));

        verify(orderService, times(1)).getOrdersByStatus(OrderStatus.PENDING);
    }

    @Test
    public void getUserOrdersByStatus_ReturnsUserOrdersByStatus() throws Exception {
        when(orderService.getUserOrdersByStatus(1L, OrderStatus.PENDING)).thenReturn(Arrays.asList(orderSummaryDTO));

        mockMvc.perform(get("/api/orders/user/1/status/PENDING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].status").value("PENDING"));

        verify(orderService, times(1)).getUserOrdersByStatus(1L, OrderStatus.PENDING);
    }

    @Test
    public void updateStatus_ExistingOrder_UpdatesStatus() throws Exception {
        OrderResponseDTO updatedOrderResponseDTO = new OrderResponseDTO();
        updatedOrderResponseDTO.setId(1L);
        updatedOrderResponseDTO.setUserId(1L);
        updatedOrderResponseDTO.setOrderNumber("ORD-12345678");
        updatedOrderResponseDTO.setTotalAmount(new BigDecimal("59.98"));
        updatedOrderResponseDTO.setStatus(OrderStatus.PROCESSING);
        updatedOrderResponseDTO.setOrderDate(LocalDateTime.now());
        updatedOrderResponseDTO.setShippingAddress("123 Test St, Test City");
        updatedOrderResponseDTO.setPaymentMethod("CREDIT_CARD");

        OrderItemResponseDTO itemResponseDTO = new OrderItemResponseDTO();
        itemResponseDTO.setId(1L);
        itemResponseDTO.setBookId(1L);
        itemResponseDTO.setBookTitle("Test Book");
        itemResponseDTO.setQuantity(2);
        itemResponseDTO.setPrice(new BigDecimal("29.99"));
        itemResponseDTO.setSubtotal(new BigDecimal("59.98"));
        updatedOrderResponseDTO.setItems(Arrays.asList(itemResponseDTO));
        when(orderService.updateOrderStatus(1L, orderStatusUpdateDTO)).thenReturn(updatedOrderResponseDTO);

        mockMvc.perform(put("/api/orders/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"PROCESSING\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PROCESSING"));

        verify(orderService, times(1)).updateOrderStatus(1L, orderStatusUpdateDTO);
    }

    @Test
    public void updateStatus_NonExistingOrder_ReturnsNotFound() throws Exception {
        when(orderService.updateOrderStatus(999L, orderStatusUpdateDTO)).thenReturn(null);

        mockMvc.perform(put("/api/orders/999/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"PROCESSING\"}"))
                .andExpect(status().isNotFound());

        verify(orderService, times(1)).updateOrderStatus(999L, orderStatusUpdateDTO);
    }

    @Test
    public void updateStatusByNumber_ExistingOrder_UpdatesStatus() throws Exception {
        OrderResponseDTO updatedOrderResponseDTO = new OrderResponseDTO();
        updatedOrderResponseDTO.setId(1L);
        updatedOrderResponseDTO.setUserId(1L);
        updatedOrderResponseDTO.setOrderNumber("ORD-12345678");
        updatedOrderResponseDTO.setTotalAmount(new BigDecimal("59.98"));
        updatedOrderResponseDTO.setStatus(OrderStatus.PROCESSING);
        updatedOrderResponseDTO.setOrderDate(LocalDateTime.now());
        updatedOrderResponseDTO.setShippingAddress("123 Test St, Test City");
        updatedOrderResponseDTO.setPaymentMethod("CREDIT_CARD");

        OrderItemResponseDTO itemResponseDTO = new OrderItemResponseDTO();
        itemResponseDTO.setId(1L);
        itemResponseDTO.setBookId(1L);
        itemResponseDTO.setBookTitle("Test Book");
        itemResponseDTO.setQuantity(2);
        itemResponseDTO.setPrice(new BigDecimal("29.99"));
        itemResponseDTO.setSubtotal(new BigDecimal("59.98"));
        updatedOrderResponseDTO.setItems(Arrays.asList(itemResponseDTO));

        when(orderService.updateOrderStatusByNumber("ORD-12345678", orderStatusUpdateDTO)).thenReturn(updatedOrderResponseDTO);

        mockMvc.perform(put("/api/orders/number/ORD-12345678/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"PROCESSING\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PROCESSING"));

        verify(orderService, times(1)).updateOrderStatusByNumber("ORD-12345678", orderStatusUpdateDTO);
    }

    @Test
    public void updateStatusByNumber_NonExistingOrder_ReturnsNotFound() throws Exception {
        when(orderService.updateOrderStatusByNumber("ORD-NONEXIST", orderStatusUpdateDTO)).thenReturn(null);

        mockMvc.perform(put("/api/orders/number/ORD-NONEXIST/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"PROCESSING\"}"))
                .andExpect(status().isNotFound());

        verify(orderService, times(1)).updateOrderStatusByNumber("ORD-NONEXIST", orderStatusUpdateDTO);
    }

    @Test
    public void updateOrder_ExistingOrder_UpdatesOrder() throws Exception {
        when(orderService.updateOrder(1L, orderRequestDTO)).thenReturn(orderResponseDTO);

        mockMvc.perform(put("/api/orders/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":1,\"shippingAddress\":\"123 Test St, Test City\",\"paymentMethod\":\"CREDIT_CARD\",\"items\":[{\"bookId\":1,\"quantity\":2}]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderNumber").value("ORD-12345678"));

        verify(orderService, times(1)).updateOrder(1L, orderRequestDTO);
    }

    @Test
    public void updateOrder_NonExistingOrder_ReturnsBadRequest() throws Exception {
        when(orderService.updateOrder(999L, orderRequestDTO)).thenReturn(null);

        mockMvc.perform(put("/api/orders/999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":1,\"shippingAddress\":\"123 Test St, Test City\",\"paymentMethod\":\"CREDIT_CARD\",\"items\":[{\"bookId\":1,\"quantity\":2}]}"))
                .andExpect(status().isBadRequest());

        verify(orderService, times(1)).updateOrder(999L, orderRequestDTO);
    }

    @Test
    public void cancelOrder_ExistingOrder_CancelsOrder() throws Exception {
        when(orderService.cancelOrder(1L, "Changed mind")).thenReturn(true);

        mockMvc.perform(delete("/api/orders/1")
                        .param("reason", "Changed mind"))
                .andExpect(status().isNoContent());

        verify(orderService, times(1)).cancelOrder(1L, "Changed mind");
    }

    @Test
    public void cancelOrder_NonExistingOrder_ReturnsNotFound() throws Exception {
        when(orderService.cancelOrder(999L, "Test reason")).thenReturn(false);

        mockMvc.perform(delete("/api/orders/999")
                        .param("reason", "Test reason"))
                .andExpect(status().isNotFound());

        verify(orderService, times(1)).cancelOrder(999L, "Test reason");
    }

    @Test
    public void getOrderStatistics_ReturnsStatistics() throws Exception {
        OrderStatisticsDTO statisticsDTO = new OrderStatisticsDTO();
        statisticsDTO.setTotalOrders(100L);
        statisticsDTO.setCompletedOrders(80L);
        statisticsDTO.setPendingOrders(15L);
        statisticsDTO.setCancelledOrders(5L);
        statisticsDTO.setProcessingOrders(0L);
        statisticsDTO.setTotalRevenue(new BigDecimal("5000.00"));
        statisticsDTO.setAverageOrderValue(new BigDecimal("62.50"));

        Map<String, Long> ordersByStatus = new HashMap<>();
        ordersByStatus.put("PENDING", 15L);
        ordersByStatus.put("PROCESSING", 0L);
        ordersByStatus.put("COMPLETED", 80L);
        ordersByStatus.put("CANCELLED", 5L);
        statisticsDTO.setOrdersByStatus(ordersByStatus);

        when(orderService.getOrderStatistics()).thenReturn(statisticsDTO);

        mockMvc.perform(get("/api/orders/statistics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalOrders").value(100))
                .andExpect(jsonPath("$.completedOrders").value(80))
                .andExpect(jsonPath("$.totalRevenue").value(5000.00))
                .andExpect(jsonPath("$.ordersByStatus.PENDING").value(15));

        verify(orderService, times(1)).getOrderStatistics();
    }

    @Test
    public void getUserStatistics_ReturnsUserStatistics() throws Exception {
        UserOrderStatisticsDTO userStatisticsDTO = new UserOrderStatisticsDTO();
        userStatisticsDTO.setUserId(1L);
        userStatisticsDTO.setTotalOrders(5L);
        userStatisticsDTO.setCompletedOrders(4L);
        userStatisticsDTO.setPendingOrders(1L);
        userStatisticsDTO.setCancelledOrders(0L);
        userStatisticsDTO.setTotalSpent(new BigDecimal("250.00"));
        userStatisticsDTO.setAverageOrderValue(new BigDecimal("62.50"));

        when(orderService.getUserOrderStatistics(1L)).thenReturn(userStatisticsDTO);

        mockMvc.perform(get("/api/orders/user/1/statistics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalOrders").value(5))
                .andExpect(jsonPath("$.totalSpent").value(250.00))
                .andExpect(jsonPath("$.averageOrderValue").value(62.50));

        verify(orderService, times(1)).getUserOrderStatistics(1L);
    }

    @Test
    public void getOrdersByDateRange_ValidRange_ReturnsOrders() throws Exception {
        when(orderService.getUserOrdersByStatus(1L, OrderStatus.PENDING)).thenReturn(Arrays.asList(orderSummaryDTO));

        mockMvc.perform(get("/api/orders/date-range")
                        .param("start", "2023-01-01T00:00:00")
                        .param("end", "2023-12-31T23:59:59"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }
}