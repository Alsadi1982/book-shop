package com.otus.order_service.rest;

import com.otus.order_service.dto.*;
import com.otus.order_service.enums.OrderStatus;
import com.otus.order_service.service.OrderService;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    @Autowired
    private OrderService orderService;

    // Create Order
    @PostMapping
    @CircuitBreaker(name = "orderService", fallbackMethod = "createOrderFallback")
    @RateLimiter(name = "orderService")
    @Retry(name = "orderService")
    public ResponseEntity<?> createOrder(@Valid @RequestBody OrderRequestDTO requestDTO) {
        try {
            OrderResponseDTO createdOrder = orderService.createOrder(requestDTO);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdOrder);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    public ResponseEntity<?> createOrderFallback(OrderRequestDTO requestDTO, Throwable t) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of(
                        "error", "Cannot create order at this moment",
                        "reason", "Service overloaded or unavailable"
                ));
    }

    // Get Order by ID
    @GetMapping("/{id}")
    @CircuitBreaker(name = "orderService", fallbackMethod = "getOrderFallback")
    @RateLimiter(name = "orderService")
    public ResponseEntity<OrderResponseDTO> getOrder(@PathVariable Long id) {
        OrderResponseDTO order = orderService.getOrder(id);
        return order != null ? ResponseEntity.ok(order) : ResponseEntity.notFound().build();
    }

    public ResponseEntity<?> getOrderFallback(Long id, Throwable t) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of(
                        "error", "Service temporarily unavailable",
                        "reason", t.getMessage()
                ));
    }

    // Get Order by Order Number
    @GetMapping("/number/{orderNumber}")
    @CircuitBreaker(name = "orderService", fallbackMethod = "getOrderByNumberFallback")
    @RateLimiter(name = "orderService")
    public ResponseEntity<OrderResponseDTO> getOrderByNumber(@PathVariable String orderNumber) {
        OrderResponseDTO order = orderService.getOrderByNumber(orderNumber);
        return order != null ? ResponseEntity.ok(order) : ResponseEntity.notFound().build();
    }

    public ResponseEntity<?> getOrderByNumberFallback(String orderNumber, Throwable t) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of(
                        "error", "Service temporarily unavailable",
                        "reason", t.getMessage()
                ));
    }

    // Get User Orders (Summary)
    @GetMapping("/user/{userId}")
    @CircuitBreaker(name = "orderService", fallbackMethod = "getUserOrdersFallback")
    @RateLimiter(name = "orderService")
    public ResponseEntity<List<OrderSummaryDTO>> getUserOrders(@PathVariable Long userId) {
        return ResponseEntity.ok(orderService.getUserOrders(userId));
    }

    public ResponseEntity<?> getUserOrdersFallback(Long userId, Throwable t) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of(
                        "error", "Cannot fetch user orders at this moment",
                        "reason", t.getMessage()
                ));
    }

    // Get Orders by Status (Summary)
    @GetMapping("/status/{status}")
    @CircuitBreaker(name = "orderService", fallbackMethod = "getOrdersByStatusFallback")
    @RateLimiter(name = "orderService")
    public ResponseEntity<List<OrderSummaryDTO>> getOrdersByStatus(@PathVariable OrderStatus status) {
        return ResponseEntity.ok(orderService.getOrdersByStatus(status));
    }

    public ResponseEntity<?> getOrdersByStatusFallback(OrderStatus status, Throwable t) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of(
                        "error", "Cannot fetch orders by status at this moment",
                        "reason", t.getMessage()
                ));
    }

    // Get User Orders by Status (Summary)
    @GetMapping("/user/{userId}/status/{status}")
    @CircuitBreaker(name = "orderService", fallbackMethod = "getUserOrdersByStatusFallback")
    @RateLimiter(name = "orderService")
    public ResponseEntity<List<OrderSummaryDTO>> getUserOrdersByStatus(
            @PathVariable Long userId,
            @PathVariable OrderStatus status) {
        return ResponseEntity.ok(orderService.getUserOrdersByStatus(userId, status));
    }

    public ResponseEntity<?> getUserOrdersByStatusFallback(Long userId, OrderStatus status, Throwable t) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of(
                        "error", "Cannot fetch user orders by status at this moment",
                        "reason", t.getMessage()
                ));
    }

    // Update Order Status
    @PutMapping("/{id}/status")
    @CircuitBreaker(name = "orderService", fallbackMethod = "updateStatusFallback")
    @RateLimiter(name = "orderService")
    @Retry(name = "orderService")
    public ResponseEntity<OrderResponseDTO> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody OrderStatusUpdateDTO updateDTO) {
        OrderResponseDTO order = orderService.updateOrderStatus(id, updateDTO);
        return order != null ? ResponseEntity.ok(order) : ResponseEntity.notFound().build();
    }

    public ResponseEntity<?> updateStatusFallback(Long id, OrderStatusUpdateDTO updateDTO, Throwable t) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of(
                        "error", "Cannot update order status at this moment",
                        "reason", t.getMessage()
                ));
    }

    // Update Order Status by Order Number
    @PutMapping("/number/{orderNumber}/status")
    @CircuitBreaker(name = "orderService", fallbackMethod = "updateStatusByNumberFallback")
    @RateLimiter(name = "orderService")
    @Retry(name = "orderService")
    public ResponseEntity<OrderResponseDTO> updateStatusByNumber(
            @PathVariable String orderNumber,
            @Valid @RequestBody OrderStatusUpdateDTO updateDTO) {
        OrderResponseDTO order = orderService.updateOrderStatusByNumber(orderNumber, updateDTO);
        return order != null ? ResponseEntity.ok(order) : ResponseEntity.notFound().build();
    }

    public ResponseEntity<?> updateStatusByNumberFallback(String orderNumber, OrderStatusUpdateDTO updateDTO, Throwable t) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of(
                        "error", "Cannot update order status at this moment",
                        "reason", t.getMessage()
                ));
    }

    // Update Order
    @PutMapping("/{id}")
    @CircuitBreaker(name = "orderService", fallbackMethod = "updateOrderFallback")
    @RateLimiter(name = "orderService")
    @Retry(name = "orderService")
    public ResponseEntity<OrderResponseDTO> updateOrder(
            @PathVariable Long id,
            @Valid @RequestBody OrderRequestDTO requestDTO) {
        OrderResponseDTO updated = orderService.updateOrder(id, requestDTO);
        return updated != null ? ResponseEntity.ok(updated) : ResponseEntity.badRequest().build();
    }

    public ResponseEntity<?> updateOrderFallback(Long id, OrderRequestDTO requestDTO, Throwable t) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of(
                        "error", "Cannot update order at this moment",
                        "reason", t.getMessage()
                ));
    }

    // Cancel Order
    @DeleteMapping("/{id}")
    @CircuitBreaker(name = "orderService", fallbackMethod = "cancelOrderFallback")
    @RateLimiter(name = "orderService")
    @Retry(name = "orderService")
    public ResponseEntity<Void> cancelOrder(
            @PathVariable Long id,
            @RequestParam(required = false) String reason) {
        boolean cancelled = orderService.cancelOrder(id, reason);
        return cancelled ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }

    public ResponseEntity<?> cancelOrderFallback(Long id, String reason, Throwable t) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of(
                        "error", "Cannot cancel order at this moment",
                        "reason", t.getMessage()
                ));
    }

    // Get Overall Order Statistics
    @GetMapping("/statistics")
    @CircuitBreaker(name = "orderService", fallbackMethod = "getOrderStatisticsFallback")
    @RateLimiter(name = "orderService")
    public ResponseEntity<OrderStatisticsDTO> getOrderStatistics() {
        return ResponseEntity.ok(orderService.getOrderStatistics());
    }

    public ResponseEntity<?> getOrderStatisticsFallback(Throwable t) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of(
                        "error", "Cannot fetch order statistics at this moment",
                        "reason", t.getMessage()
                ));
    }

    // Get User Order Statistics
    @GetMapping("/user/{userId}/statistics")
    @CircuitBreaker(name = "orderService", fallbackMethod = "getUserStatisticsFallback")
    @RateLimiter(name = "orderService")
    public ResponseEntity<UserOrderStatisticsDTO> getUserStatistics(@PathVariable Long userId) {
        return ResponseEntity.ok(orderService.getUserOrderStatistics(userId));
    }

    public ResponseEntity<?> getUserStatisticsFallback(Long userId, Throwable t) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of(
                        "error", "Cannot fetch user statistics at this moment",
                        "reason", t.getMessage()
                ));
    }

    // Get Orders by Date Range (Summary)
    @GetMapping("/date-range")
    @CircuitBreaker(name = "orderService", fallbackMethod = "getOrdersByDateRangeFallback")
    @RateLimiter(name = "orderService")
    public ResponseEntity<List<OrderSummaryDTO>> getOrdersByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        // Implementation needed
        return ResponseEntity.ok(List.of());
    }

    public ResponseEntity<?> getOrdersByDateRangeFallback(LocalDateTime start, LocalDateTime end, Throwable t) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of(
                        "error", "Cannot fetch orders by date range at this moment",
                        "reason", t.getMessage()
                ));
    }

    // Health check endpoint
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "UP", "service", "order-service"));
    }
}
