package com.otus.order_service.service;

import com.otus.order_service.dto.*;
import com.otus.order_service.entity.Order;
import com.otus.order_service.entity.OrderItem;
import com.otus.order_service.enums.OrderStatus;
import com.otus.order_service.mapper.OrderMapper;
import com.otus.order_service.repository.OrderItemRepository;
import com.otus.order_service.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Service
public class OrderService {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private CatalogServiceClient catalogServiceClient;

    @Autowired
    private OrderMapper orderMapper;

    // Ручное кэширование с java.util.concurrent
    private final ConcurrentHashMap<Long, OrderResponseDTO> orderCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, OrderResponseDTO> orderNumberCache = new ConcurrentHashMap<>();
    private final ReentrantReadWriteLock cacheLock = new ReentrantReadWriteLock();

    @Cacheable(value = "orders", key = "#id")
    public OrderResponseDTO getOrder(Long id) {
        Order order = orderRepository.findById(id).orElse(null);
        return order != null ? orderMapper.toResponseDTO(order) : null;
    }

    @Cacheable(value = "ordersByNumber", key = "#orderNumber")
    public OrderResponseDTO getOrderByNumber(String orderNumber) {
        Order order = orderRepository.findByOrderNumber(orderNumber).orElse(null);
        return order != null ? orderMapper.toResponseDTO(order) : null;
    }

    public List<OrderSummaryDTO> getUserOrders(Long userId) {
        List<Order> orders = orderRepository.findByUserId(userId);
        return orderMapper.toSummaryDTOList(orders);
    }

    public List<OrderSummaryDTO> getOrdersByStatus(OrderStatus status) {
        List<Order> orders = orderRepository.findByStatus(status);
        return orderMapper.toSummaryDTOList(orders);
    }

    public List<OrderSummaryDTO> getUserOrdersByStatus(Long userId, OrderStatus status) {
        List<Order> orders = orderRepository.findByUserIdAndStatus(userId, status);
        return orderMapper.toSummaryDTOList(orders);
    }

    @Transactional
    public OrderResponseDTO createOrder(OrderRequestDTO requestDTO) {
        // Validate user exists
        Map<String, Object> user = catalogServiceClient.getUser(requestDTO.getUserId());
        if (user == null) {
            throw new RuntimeException("User not found with id: " + requestDTO.getUserId());
        }

        // Generate unique order number
        String orderNumber;
        do {
            orderNumber = "ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        } while (orderRepository.existsByOrderNumber(orderNumber));

        // Convert DTO to Entity
        Order order = orderMapper.toEntity(requestDTO);
        order.setOrderNumber(orderNumber);
        order.setOrderDate(LocalDateTime.now());
        order.setStatus(OrderStatus.PENDING);

        BigDecimal total = BigDecimal.ZERO;

        // Process each item
        for (OrderItem item : order.getItems()) {
            Map<String, Object> book = catalogServiceClient.getBook(item.getBookId());
            if (book == null) {
                throw new RuntimeException("Book not found: " + item.getBookId());
            }

            // Check and reserve stock
            boolean reserved = catalogServiceClient.checkAndReserveStock(item.getBookId(), item.getQuantity());
            if (!reserved) {
                throw new RuntimeException("Insufficient stock for book: " + book.get("title"));
            }

            item.setBookTitle((String) book.get("title"));
            BigDecimal price = new BigDecimal(book.get("price").toString());
            item.setPrice(price);
            item.setSubtotal(price.multiply(BigDecimal.valueOf(item.getQuantity())));
            total = total.add(item.getSubtotal());
        }

        order.setTotalAmount(total);
        Order savedOrder = orderRepository.save(order);

        // Save order items with correct order ID
        for (OrderItem item : savedOrder.getItems()) {
            item.setId(savedOrder.getId());
            orderItemRepository.save(item);
        }

        OrderResponseDTO responseDTO = orderMapper.toResponseDTO(savedOrder);

        // Cache the response
        cacheLock.writeLock().lock();
        try {
            orderCache.put(savedOrder.getId(), responseDTO);
            orderNumberCache.put(savedOrder.getOrderNumber(), responseDTO);
        } finally {
            cacheLock.writeLock().unlock();
        }

        return responseDTO;
    }

    @Transactional
    public OrderResponseDTO updateOrderStatus(Long id, OrderStatusUpdateDTO updateDTO) {
        Order order = orderRepository.findById(id).orElse(null);
        if (order != null) {
            orderMapper.updateEntity(order, updateDTO);
            Order updatedOrder = orderRepository.save(order);
            OrderResponseDTO responseDTO = orderMapper.toResponseDTO(updatedOrder);

            cacheLock.writeLock().lock();
            try {
                orderCache.put(id, responseDTO);
                orderNumberCache.put(updatedOrder.getOrderNumber(), responseDTO);
            } finally {
                cacheLock.writeLock().unlock();
            }

            return responseDTO;
        }
        return null;
    }

    @Transactional
    public OrderResponseDTO updateOrderStatusByNumber(String orderNumber, OrderStatusUpdateDTO updateDTO) {
        Order order = orderRepository.findByOrderNumber(orderNumber).orElse(null);
        if (order != null) {
            orderMapper.updateEntity(order, updateDTO);
            Order updatedOrder = orderRepository.save(order);
            return orderMapper.toResponseDTO(updatedOrder);
        }
        return null;
    }

    @Transactional
    public OrderResponseDTO updateOrder(Long id, OrderRequestDTO requestDTO) {
        Order existingOrder = orderRepository.findById(id).orElse(null);
        if (existingOrder != null && existingOrder.getStatus() == OrderStatus.PENDING) {
            existingOrder.setShippingAddress(requestDTO.getShippingAddress());
            existingOrder.setPaymentMethod(requestDTO.getPaymentMethod());

            Order updatedOrder = orderRepository.save(existingOrder);
            OrderResponseDTO responseDTO = orderMapper.toResponseDTO(updatedOrder);

            cacheLock.writeLock().lock();
            try {
                orderCache.put(id, responseDTO);
            } finally {
                cacheLock.writeLock().unlock();
            }

            return responseDTO;
        }
        return null;
    }

    @CacheEvict(value = "orders", key = "#id")
    @Transactional
    public boolean cancelOrder(Long id, String reason) {
        Order order = orderRepository.findById(id).orElse(null);
        if (order != null && order.getStatus() == OrderStatus.PENDING) {
            order.setStatus(OrderStatus.CANCELLED);
            orderRepository.save(order);

            // Return items to stock
            for (OrderItem item : order.getItems()) {
                 catalogServiceClient.returnStock(item.getBookId(), item.getQuantity());
            }

            cacheLock.writeLock().lock();
            try {
                orderCache.remove(id);
                orderNumberCache.remove(order.getOrderNumber());
            } finally {
                cacheLock.writeLock().unlock();
            }

            return true;
        }
        return false;
    }

    public OrderStatisticsDTO getOrderStatistics() {
        OrderStatisticsDTO stats = new OrderStatisticsDTO();

        List<Order> allOrders = orderRepository.findAll();

        stats.setTotalOrders((long) allOrders.size());
        stats.setCompletedOrders(allOrders.stream().filter(o -> o.getStatus() == OrderStatus.COMPLETED).count());
        stats.setPendingOrders(allOrders.stream().filter(o -> o.getStatus() == OrderStatus.PENDING).count());
        stats.setCancelledOrders(allOrders.stream().filter(o -> o.getStatus() == OrderStatus.CANCELLED).count());
        stats.setProcessingOrders(allOrders.stream().filter(o -> o.getStatus() == OrderStatus.PROCESSING).count());

        // Calculate revenue
        BigDecimal totalRevenue = allOrders.stream()
                .filter(o -> o.getStatus() == OrderStatus.COMPLETED)
                .map(Order::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        stats.setTotalRevenue(totalRevenue);

        // Calculate average order value
        if (stats.getCompletedOrders() > 0) {
            stats.setAverageOrderValue(totalRevenue.divide(BigDecimal.valueOf(stats.getCompletedOrders()), 2, BigDecimal.ROUND_HALF_UP));
        } else {
            stats.setAverageOrderValue(BigDecimal.ZERO);
        }

        // Orders by status map
        Map<String, Long> ordersByStatus = new HashMap<>();
        ordersByStatus.put("PENDING", stats.getPendingOrders());
        ordersByStatus.put("PROCESSING", stats.getProcessingOrders());
        ordersByStatus.put("COMPLETED", stats.getCompletedOrders());
        ordersByStatus.put("CANCELLED", stats.getCancelledOrders());
        stats.setOrdersByStatus(ordersByStatus);

        return stats;
    }

    public UserOrderStatisticsDTO getUserOrderStatistics(Long userId) {
        List<Order> userOrders = orderRepository.findByUserId(userId);

        UserOrderStatisticsDTO stats = new UserOrderStatisticsDTO();
        stats.setUserId(userId);
        stats.setTotalOrders((long) userOrders.size());

        BigDecimal totalSpent = userOrders.stream()
                .filter(o -> o.getStatus() == OrderStatus.COMPLETED)
                .map(Order::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        stats.setTotalSpent(totalSpent);

        stats.setCompletedOrders(userOrders.stream().filter(o -> o.getStatus() == OrderStatus.COMPLETED).count());
        stats.setPendingOrders(userOrders.stream().filter(o -> o.getStatus() == OrderStatus.PENDING).count());
        stats.setCancelledOrders(userOrders.stream().filter(o -> o.getStatus() == OrderStatus.CANCELLED).count());

        if (stats.getCompletedOrders() > 0) {
            stats.setAverageOrderValue(totalSpent.divide(BigDecimal.valueOf(stats.getCompletedOrders()), 2, BigDecimal.ROUND_HALF_UP));
        }

        return stats;
    }

    public OrderResponseDTO getOrderWithManualCache(Long id) {
        cacheLock.readLock().lock();
        try {
            OrderResponseDTO cached = orderCache.get(id);
            if (cached != null) {
                return cached;
            }
        } finally {
            cacheLock.readLock().unlock();
        }

        Order order = orderRepository.findById(id).orElse(null);
        if (order != null) {
            OrderResponseDTO responseDTO = orderMapper.toResponseDTO(order);
            cacheLock.writeLock().lock();
            try {
                orderCache.put(id, responseDTO);
                orderNumberCache.put(order.getOrderNumber(), responseDTO);
            } finally {
                cacheLock.writeLock().unlock();
            }
            return responseDTO;
        }
        return null;
    }

    public void clearCache() {
        cacheLock.writeLock().lock();
        try {
            orderCache.clear();
            orderNumberCache.clear();
        } finally {
            cacheLock.writeLock().unlock();
        }
    }
}
