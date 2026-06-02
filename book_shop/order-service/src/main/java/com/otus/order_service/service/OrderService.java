package com.otus.order_service.service;

import com.otus.order_service.dto.*;
import com.otus.order_service.entity.Order;
import com.otus.order_service.entity.OrderItem;
import com.otus.order_service.enums.OrderStatus;
import com.otus.order_service.mapper.OrderMapper;
import com.otus.order_service.repository.OrderItemRepository;
import com.otus.order_service.repository.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Service
public class OrderService {

    private static final Logger logger = LoggerFactory.getLogger(OrderService.class);

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
        if (order != null) {
            logger.debug("Order retrieved: id={}", id);
        }
        return order != null ? orderMapper.toResponseDTO(order) : null;
    }

    @Cacheable(value = "ordersByNumber", key = "#orderNumber")
    public OrderResponseDTO getOrderByNumber(String orderNumber) {
        Order order = orderRepository.findByOrderNumber(orderNumber).orElse(null);
        if (order != null) {
            logger.debug("Order retrieved by number: number={}", orderNumber);
        }
        return order != null ? orderMapper.toResponseDTO(order) : null;
    }

    public List<OrderSummaryDTO> getUserOrders(Long userId) {
        List<Order> orders = orderRepository.findByUserId(userId);
        logger.debug("Retrieved {} orders for user: {}", orders.size(), userId);
        return orderMapper.toSummaryDTOList(orders);
    }

    public List<OrderSummaryDTO> getOrdersByStatus(OrderStatus status) {
        List<Order> orders = orderRepository.findByStatus(status);
        logger.debug("Retrieved {} orders with status: {}", orders.size(), status);
        return orderMapper.toSummaryDTOList(orders);
    }

    public List<OrderSummaryDTO> getUserOrdersByStatus(Long userId, OrderStatus status) {
        List<Order> orders = orderRepository.findByUserIdAndStatus(userId, status);
        logger.debug("Retrieved {} orders for user {} with status: {}", orders.size(), userId, status);
        return orderMapper.toSummaryDTOList(orders);
    }

    @Transactional
    public OrderResponseDTO createOrder(OrderRequestDTO requestDTO) {
        logger.info("Creating order for user: {}", requestDTO.getUserId());
        
        // Validate user exists
        Map<String, Object> user = catalogServiceClient.getUser(requestDTO.getUserId());
        if (user == null) {
            logger.error("User not found with id: {}", requestDTO.getUserId());
            throw new RuntimeException("User not found with id: " + requestDTO.getUserId());
        }
        logger.debug("User validated: id={}", requestDTO.getUserId());

        BigDecimal total = BigDecimal.ZERO;
        List<OrderItem> orderItems = new ArrayList<>();

        for (OrderItemRequestDTO itemDTO : requestDTO.getItems()) {
            Map<String, Object> book = catalogServiceClient.getBook(itemDTO.getBookId());
            if (book == null) {
                logger.error("Book not found: {}", itemDTO.getBookId());
                throw new RuntimeException("Book not found: " + itemDTO.getBookId());
            }

            // Проверяем сток ДО создания заказа
            boolean reserved = catalogServiceClient.checkAndReserveStock(itemDTO.getBookId(), itemDTO.getQuantity());
            if (!reserved) {
                logger.error("Insufficient stock for book: {}", book.get("title"));
                throw new RuntimeException("Insufficient stock for book: " + book.get("title"));
            }
            logger.debug("Stock reserved for book: id={}, quantity={}", itemDTO.getBookId(), itemDTO.getQuantity());

            // Создаем OrderItem (пока без сохранения)
            OrderItem item = new OrderItem();
            item.setBookId(itemDTO.getBookId());
            item.setQuantity(itemDTO.getQuantity());
            item.setBookTitle((String) book.get("title"));
            BigDecimal price = new BigDecimal(book.get("price").toString());
            item.setPrice(price);
            item.setSubtotal(price.multiply(BigDecimal.valueOf(itemDTO.getQuantity())));

            total = total.add(item.getSubtotal());
            orderItems.add(item);
        }

        // Generate unique order number
        String orderNumber;
        do {
            orderNumber = "ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        } while (orderRepository.existsByOrderNumber(orderNumber));

        // Now create the Order entity with validated data
        Order order = new Order();
        order.setUserId(requestDTO.getUserId());
        order.setOrderNumber(orderNumber);
        order.setOrderDate(LocalDateTime.now());
        order.setStatus(OrderStatus.PENDING);
        order.setShippingAddress(requestDTO.getShippingAddress());
        order.setPaymentMethod(requestDTO.getPaymentMethod());

        Order savedOrder = orderRepository.save(order);

        if (savedOrder == null) {
            logger.error("Failed to save order for user: {}", requestDTO.getUserId());
            throw new RuntimeException("Failed to save order");
        }
        logger.info("Order created successfully: id={}, number={}, user={}", savedOrder.getId(), savedOrder.getOrderNumber(), requestDTO.getUserId());

        for (OrderItem item : orderItems) {
            item.setOrderId(savedOrder.getId());
        }
        orderItemRepository.saveAll(orderItems);

        // Set the saved items back to the order for the response
//        savedOrder.setTotalAmount(total);
        savedOrder.setItems(orderItems);
//        savedOrder = orderRepository.save(savedOrder);

        OrderResponseDTO responseDTO = orderMapper.toResponseDTO(savedOrder);

        // Cache the response
        if (responseDTO != null) {
            cacheLock.writeLock().lock();
            try {
                if (savedOrder.getId() != null) {
                    orderCache.put(savedOrder.getId(), responseDTO);
                }
                if (savedOrder.getOrderNumber() != null) {
                    orderNumberCache.put(savedOrder.getOrderNumber(), responseDTO);
                }
            } finally {
                cacheLock.writeLock().unlock();
            }
        }

        return responseDTO;
    }

    @Transactional
    public OrderResponseDTO updateOrderStatus(Long id, OrderStatusUpdateDTO updateDTO) {
        logger.info("Updating order status: id={}, status={}", id, updateDTO.getStatus());
        Order order = orderRepository.findById(id).orElse(null);
        if (order == null) {
            logger.warn("Order not found: id={}", id);
            return null;
        }
        orderMapper.updateEntity(order, updateDTO);
        Order updatedOrder = orderRepository.save(order);

        if (updatedOrder == null) {
            logger.error("Failed to update order: id={}", id);
            return null;
        }
        logger.info("Order status updated: id={}, new status={}", id, updatedOrder.getStatus());

        OrderResponseDTO responseDTO = orderMapper.toResponseDTO(updatedOrder);

        // Кэшируем только если responseDTO не null
        if (responseDTO != null) {
            cacheLock.writeLock().lock();
            try {
                orderCache.put(id, responseDTO);
                if (updatedOrder.getOrderNumber() != null) {
                    orderNumberCache.put(updatedOrder.getOrderNumber(), responseDTO);
                }
            } finally {
                cacheLock.writeLock().unlock();
            }
        }

        return responseDTO;
    }

    @Transactional
    public OrderResponseDTO updateOrderStatusByNumber(String orderNumber, OrderStatusUpdateDTO updateDTO) {
        logger.info("Updating order status by number: number={}, status={}", orderNumber, updateDTO.getStatus());
        Order order = orderRepository.findByOrderNumber(orderNumber).orElse(null);
        if (order == null) {
            logger.warn("Order not found by number: {}", orderNumber);
            return null;
        }
        orderMapper.updateEntity(order, updateDTO);
        Order updatedOrder = orderRepository.save(order);

        if (updatedOrder == null) {
            logger.error("Failed to update order by number: {}", orderNumber);
            return null;
        }
        logger.info("Order status updated by number: number={}, new status={}", orderNumber, updatedOrder.getStatus());

        OrderResponseDTO responseDTO = orderMapper.toResponseDTO(updatedOrder);

        // Кэшируем только если responseDTO не null
        if (responseDTO != null) {
            cacheLock.writeLock().lock();
            try {
                if (updatedOrder.getId() != null) {
                    orderCache.put(updatedOrder.getId(), responseDTO);
                }
                if (updatedOrder.getOrderNumber() != null) {
                    orderNumberCache.put(updatedOrder.getOrderNumber(), responseDTO);
                }
            } finally {
                cacheLock.writeLock().unlock();
            }
        }

        return responseDTO;
    }

    @Transactional
    public OrderResponseDTO updateOrder(Long id, OrderRequestDTO requestDTO) {
        logger.info("Updating order: id={}", id);
        Order existingOrder = orderRepository.findById(id).orElse(null);
        if (existingOrder == null) {
            logger.warn("Order not found: id={}", id);
            return null;
        }
        if (existingOrder.getStatus() != OrderStatus.PENDING) {
            logger.warn("Cannot update order with status: id={}, status={}", id, existingOrder.getStatus());
            return null;
        }
        existingOrder.setShippingAddress(requestDTO.getShippingAddress());
        existingOrder.setPaymentMethod(requestDTO.getPaymentMethod());

        Order updatedOrder = orderRepository.save(existingOrder);

        if (updatedOrder == null) {
            logger.error("Failed to update order: id={}", id);
            return null;
        }
        logger.info("Order updated: id={}, new shippingAddress={}, new paymentMethod={}", id, updatedOrder.getShippingAddress(), updatedOrder.getPaymentMethod());

        OrderResponseDTO responseDTO = orderMapper.toResponseDTO(updatedOrder);

        // Кэшируем только если responseDTO не null
        if (responseDTO != null) {
            cacheLock.writeLock().lock();
            try {
                orderCache.put(id, responseDTO);
            } finally {
                cacheLock.writeLock().unlock();
            }
        }

        return responseDTO;
    }

    @CacheEvict(value = "orders", key = "#id")
    @Transactional
    public boolean cancelOrder(Long id, String reason) {
        logger.info("Cancelling order: id={}, reason={}", id, reason);
        Order order = orderRepository.findById(id).orElse(null);
        if (order == null) {
            logger.warn("Order not found for cancellation: id={}", id);
            return false;
        }
        if (order.getStatus() != OrderStatus.PENDING) {
            logger.warn("Cannot cancel order with status: id={}, status={}", id, order.getStatus());
            return false;
        }
        order.setStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);

        // Return items to stock
        for (OrderItem item : order.getItems()) {
            catalogServiceClient.returnStock(item.getBookId(), item.getQuantity());
        }

        logger.info("Order cancelled: id={}, number={}", id, order.getOrderNumber());

        cacheLock.writeLock().lock();
        try {
            orderCache.remove(id);
            orderNumberCache.remove(order.getOrderNumber());
        } finally {
            cacheLock.writeLock().unlock();
        }

        return true;
    }

    public OrderStatisticsDTO getOrderStatistics() {
        logger.info("Generating order statistics");
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

        logger.info("Order statistics generated: total={}, completed={}, pending={}, cancelled={}", 
                stats.getTotalOrders(), stats.getCompletedOrders(), stats.getPendingOrders(), stats.getCancelledOrders());

        return stats;
    }

    public UserOrderStatisticsDTO getUserOrderStatistics(Long userId) {
        logger.debug("Generating user statistics: userId={}", userId);
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
                logger.debug("Order retrieved from cache: id={}", id);
                return cached;
            }
        } finally {
            cacheLock.readLock().unlock();
        }

        Order order = orderRepository.findById(id).orElse(null);
        if (order == null) {
            logger.warn("Order not found: id={}", id);
            return null;
        }
        OrderResponseDTO responseDTO = orderMapper.toResponseDTO(order);
        cacheLock.writeLock().lock();
        try {
            orderCache.put(id, responseDTO);
            orderNumberCache.put(order.getOrderNumber(), responseDTO);
            logger.debug("Order cached: id={}, number={}", id, order.getOrderNumber());
        } finally {
            cacheLock.writeLock().unlock();
        }
        return responseDTO;
    }

    public void clearCache() {
        logger.info("Clearing order cache");
        cacheLock.writeLock().lock();
        try {
            orderCache.clear();
            orderNumberCache.clear();
        } finally {
            cacheLock.writeLock().unlock();
        }
    }
}
