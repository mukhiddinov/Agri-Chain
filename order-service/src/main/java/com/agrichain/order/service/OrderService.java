package com.agrichain.order.service;

import com.agrichain.order.entity.OrderEntity;
import com.agrichain.order.entity.OrderItemEntity;
import com.agrichain.order.model.CreateOrderRequest;
import com.agrichain.order.model.OrderResponse;
import com.agrichain.order.repository.OrderRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final OrderRepository orderRepository;

    public OrderResponse createOrder(CreateOrderRequest request) {
        String orderId = UUID.randomUUID().toString();
        
        // Calculate total amount
        double totalAmount = request.getItems().stream()
                .mapToDouble(item -> item.getPrice() * item.getQuantity())
                .sum();

        Instant createdAt = Instant.now();

        OrderResponse response = OrderResponse.builder()
                .orderId(orderId)
                .customerId(request.getCustomerId())
                .items(request.getItems())
                .status("PENDING")
                .totalAmount(totalAmount)
                .createdAt(createdAt)
                .build();

        // Persist order to database
        OrderEntity orderEntity = OrderEntity.builder()
                .id(orderId)
                .customerId(request.getCustomerId())
                .status("PENDING")
                .totalAmount(totalAmount)
                .createdAt(createdAt)
                .build();

        List<OrderItemEntity> itemEntities = request.getItems().stream()
                .map(item -> OrderItemEntity.builder()
                        .order(orderEntity)
                        .productId(item.getProductId())
                        .productName(item.getProductName())
                        .quantity(item.getQuantity())
                        .price(item.getPrice())
                        .build())
                .toList();

        orderEntity.setItems(itemEntities);
        orderRepository.save(orderEntity);
        log.info("Order persisted to database: {}", orderId);

        // Publish order.created event to Kafka
        try {
            String orderEvent = objectMapper.writeValueAsString(response);
            kafkaTemplate.send("order.created", orderId, orderEvent);
            log.info("Published order.created event for order: {}", orderId);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize order event", e);
        }

        return response;
    }

    public Optional<OrderResponse> getOrder(String orderId) {
        return orderRepository.findById(orderId)
                .map(order -> OrderResponse.builder()
                        .orderId(order.getId())
                        .customerId(order.getCustomerId())
                        .items(order.getItems().stream()
                                .map(item -> new com.agrichain.order.model.OrderItemRequest(
                                        item.getProductId(),
                                        item.getProductName(),
                                        item.getQuantity(),
                                        item.getPrice()))
                                .toList())
                        .status(order.getStatus())
                        .totalAmount(order.getTotalAmount())
                        .createdAt(order.getCreatedAt())
                        .build());
    }

    public void updateOrderStatus(String orderId, String status) {
        orderRepository.findById(orderId).ifPresentOrElse(order -> {
            order.setStatus(status);
            orderRepository.save(order);
            log.info("Updated order status: {} -> {}", orderId, status);
        }, () -> log.warn("Order not found when updating status: {}", orderId));
    }
}
