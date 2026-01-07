package com.agrichain.order.service;

import com.agrichain.order.model.CreateOrderRequest;
import com.agrichain.order.model.OrderResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public OrderResponse createOrder(CreateOrderRequest request) {
        String orderId = UUID.randomUUID().toString();
        
        // Calculate total amount
        double totalAmount = request.getItems().stream()
                .mapToDouble(item -> item.getPrice() * item.getQuantity())
                .sum();

        OrderResponse response = OrderResponse.builder()
                .orderId(orderId)
                .customerId(request.getCustomerId())
                .items(request.getItems())
                .status("PENDING")
                .totalAmount(totalAmount)
                .createdAt(Instant.now())
                .build();

        // Publish order.created event to Kafka
        try {
            String orderEvent = objectMapper.writeValueAsString(response);
            kafkaTemplate.send("order.created", orderId, orderEvent);
            log.info("Published order.created event for order: {}", orderId);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize order event", e);
        }

        // TODO: Persist to database (stubbed for now)
        log.info("Order created (persistence stubbed): {}", orderId);

        return response;
    }
}
