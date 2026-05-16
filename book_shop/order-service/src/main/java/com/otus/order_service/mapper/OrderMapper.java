package com.otus.order_service.mapper;

import com.otus.order_service.dto.*;
import com.otus.order_service.entity.Order;
import com.otus.order_service.entity.OrderItem;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class OrderMapper {

    // Convert Request DTO to Entity
    public Order toEntity(OrderRequestDTO requestDTO) {
        if (requestDTO == null) {
            return null;
        }

        Order order = new Order();
        order.setUserId(requestDTO.getUserId());
        order.setShippingAddress(requestDTO.getShippingAddress());
        order.setPaymentMethod(requestDTO.getPaymentMethod());

        // Convert order items
        if (requestDTO.getItems() != null) {
            List<OrderItem> items = requestDTO.getItems().stream()
                    .map(this::toOrderItemEntity)
                    .collect(Collectors.toList());
            order.setItems(items);
        }

        return order;
    }

    // Convert OrderItem Request DTO to Entity
    public OrderItem toOrderItemEntity(OrderItemRequestDTO requestDTO) {
        if (requestDTO == null) {
            return null;
        }

        OrderItem item = new OrderItem();
        item.setBookId(requestDTO.getBookId());
        item.setQuantity(requestDTO.getQuantity());

        return item;
    }

    // Convert Entity to Response DTO
    public OrderResponseDTO toResponseDTO(Order order) {
        if (order == null) {
            return null;
        }

        OrderResponseDTO responseDTO = new OrderResponseDTO();
        responseDTO.setId(order.getId());
        responseDTO.setUserId(order.getUserId());
        responseDTO.setOrderNumber(order.getOrderNumber());
        responseDTO.setTotalAmount(order.getTotalAmount());
        responseDTO.setStatus(order.getStatus());
        responseDTO.setOrderDate(order.getOrderDate());
        responseDTO.setShippingAddress(order.getShippingAddress());
        responseDTO.setPaymentMethod(order.getPaymentMethod());

        // Convert order items
        if (order.getItems() != null) {
            List<OrderItemResponseDTO> items = order.getItems().stream()
                    .map(this::toOrderItemResponseDTO)
                    .collect(Collectors.toList());
            responseDTO.setItems(items);
        }

        return responseDTO;
    }

    // Convert OrderItem Entity to Response DTO
    public OrderItemResponseDTO toOrderItemResponseDTO(OrderItem item) {
        if (item == null) {
            return null;
        }

        OrderItemResponseDTO responseDTO = new OrderItemResponseDTO();
        responseDTO.setId(item.getId());
        responseDTO.setBookId(item.getBookId());
        responseDTO.setBookTitle(item.getBookTitle());
        responseDTO.setQuantity(item.getQuantity());
        responseDTO.setPrice(item.getPrice());
        responseDTO.setSubtotal(item.getSubtotal());

        return responseDTO;
    }

    // Convert Entity to Summary DTO
    public OrderSummaryDTO toSummaryDTO(Order order) {
        if (order == null) {
            return null;
        }

        OrderSummaryDTO summaryDTO = new OrderSummaryDTO();
        summaryDTO.setId(order.getId());
        summaryDTO.setOrderNumber(order.getOrderNumber());
        summaryDTO.setTotalAmount(order.getTotalAmount());
        summaryDTO.setStatus(order.getStatus());
        summaryDTO.setOrderDate(order.getOrderDate());

        if (order.getItems() != null) {
            summaryDTO.setItemCount(order.getItems().size());
        } else {
            summaryDTO.setItemCount(0);
        }

        return summaryDTO;
    }

    // Convert list of Entities to list of Summary DTOs
    public List<OrderSummaryDTO> toSummaryDTOList(List<Order> orders) {
        if (orders == null) {
            return null;
        }

        return orders.stream()
                .map(this::toSummaryDTO)
                .collect(Collectors.toList());
    }

    // Convert list of Entities to list of Response DTOs
    public List<OrderResponseDTO> toResponseDTOList(List<Order> orders) {
        if (orders == null) {
            return null;
        }

        return orders.stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    // Update entity from update DTO
    public void updateEntity(Order order, OrderStatusUpdateDTO updateDTO) {
        if (order == null || updateDTO == null) {
            return;
        }

        if (updateDTO.getStatus() != null) {
            order.setStatus(updateDTO.getStatus());
        }
    }
}
