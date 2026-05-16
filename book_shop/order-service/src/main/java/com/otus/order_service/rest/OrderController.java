package com.otus.order_service.rest;

import com.otus.order_service.dto.*;
import com.otus.order_service.enums.OrderStatus;
import com.otus.order_service.service.OrderService;
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
    public ResponseEntity<?> createOrder(@Valid @RequestBody OrderRequestDTO requestDTO) {
        try {
            OrderResponseDTO createdOrder = orderService.createOrder(requestDTO);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdOrder);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // Get Order by ID
    @GetMapping("/{id}")
    public ResponseEntity<OrderResponseDTO> getOrder(@PathVariable Long id) {
        OrderResponseDTO order = orderService.getOrder(id);
        return order != null ? ResponseEntity.ok(order) : ResponseEntity.notFound().build();
    }

    // Get Order by Order Number
    @GetMapping("/number/{orderNumber}")
    public ResponseEntity<OrderResponseDTO> getOrderByNumber(@PathVariable String orderNumber) {
        OrderResponseDTO order = orderService.getOrderByNumber(orderNumber);
        return order != null ? ResponseEntity.ok(order) : ResponseEntity.notFound().build();
    }

    // Get User Orders (Summary)
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<OrderSummaryDTO>> getUserOrders(@PathVariable Long userId) {
        return ResponseEntity.ok(orderService.getUserOrders(userId));
    }

    // Get Orders by Status (Summary)
    @GetMapping("/status/{status}")
    public ResponseEntity<List<OrderSummaryDTO>> getOrdersByStatus(@PathVariable OrderStatus status) {
        return ResponseEntity.ok(orderService.getOrdersByStatus(status));
    }

    // Get User Orders by Status (Summary)
    @GetMapping("/user/{userId}/status/{status}")
    public ResponseEntity<List<OrderSummaryDTO>> getUserOrdersByStatus(
            @PathVariable Long userId,
            @PathVariable OrderStatus status) {
        return ResponseEntity.ok(orderService.getUserOrdersByStatus(userId, status));
    }

    // Update Order Status
    @PutMapping("/{id}/status")
    public ResponseEntity<OrderResponseDTO> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody OrderStatusUpdateDTO updateDTO) {
        OrderResponseDTO order = orderService.updateOrderStatus(id, updateDTO);
        return order != null ? ResponseEntity.ok(order) : ResponseEntity.notFound().build();
    }

    // Update Order Status by Order Number
    @PutMapping("/number/{orderNumber}/status")
    public ResponseEntity<OrderResponseDTO> updateStatusByNumber(
            @PathVariable String orderNumber,
            @Valid @RequestBody OrderStatusUpdateDTO updateDTO) {
        OrderResponseDTO order = orderService.updateOrderStatusByNumber(orderNumber, updateDTO);
        return order != null ? ResponseEntity.ok(order) : ResponseEntity.notFound().build();
    }

    // Update Order
    @PutMapping("/{id}")
    public ResponseEntity<OrderResponseDTO> updateOrder(
            @PathVariable Long id,
            @Valid @RequestBody OrderRequestDTO requestDTO) {
        OrderResponseDTO updated = orderService.updateOrder(id, requestDTO);
        return updated != null ? ResponseEntity.ok(updated) : ResponseEntity.badRequest().build();
    }

    // Cancel Order
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancelOrder(
            @PathVariable Long id,
            @RequestParam(required = false) String reason) {
        boolean cancelled = orderService.cancelOrder(id, reason);
        return cancelled ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }

    // Get Overall Order Statistics
    @GetMapping("/statistics")
    public ResponseEntity<OrderStatisticsDTO> getOrderStatistics() {
        return ResponseEntity.ok(orderService.getOrderStatistics());
    }

    // Get User Order Statistics
    @GetMapping("/user/{userId}/statistics")
    public ResponseEntity<UserOrderStatisticsDTO> getUserStatistics(@PathVariable Long userId) {
        return ResponseEntity.ok(orderService.getUserOrderStatistics(userId));
    }

    // Get Orders by Date Range (Summary)
    @GetMapping("/date-range")
    public ResponseEntity<List<OrderSummaryDTO>> getOrdersByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        // Implementation needed
        return ResponseEntity.ok(List.of());
    }

    // Health check endpoint
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "UP", "service", "order-service"));
    }
}
